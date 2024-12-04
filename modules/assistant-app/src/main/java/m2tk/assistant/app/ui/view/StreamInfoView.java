/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app.ui.view;

import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.ProgramObserver;
import m2tk.assistant.api.StreamObserver;
import m2tk.assistant.api.domain.*;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.assistant.app.ui.AssistantApp;
import m2tk.assistant.app.ui.component.CASystemInfoPanel;
import m2tk.assistant.app.ui.component.ProgramInfoPanel;
import m2tk.assistant.app.ui.component.StreamInfoPanel;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Extension(ordinal = 1)
public class StreamInfoView extends JPanel
    implements InfoView, StreamObserver, ProgramObserver
{
    private Application application;
    private ProgramInfoPanel programInfoPanel;
    private StreamInfoPanel streamInfoPanel;
    private CASystemInfoPanel casInfoPanel;

    private List<StreamObserver> streamObservers = new ArrayList<>();
    private List<ProgramObserver> programObservers = new ArrayList<>();

    private StreamSource currentSource;
    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    private static class StreamInfoSnapshot
    {
        private StreamSource source;
        private List<ElementaryStream> streams;
        private List<MPEGProgram> programs;
        private List<CASystemStream> caStreams;
    }

    public StreamInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        streamObservers = new ArrayList<>();
        programObservers = new ArrayList<>();

        streamInfoPanel = new StreamInfoPanel();
        programInfoPanel = new ProgramInfoPanel();
        casInfoPanel = new CASystemInfoPanel();

        streamInfoPanel.setPopupListener(this::showStreamPopupMenu);
        programInfoPanel.setPopupListener(this::showProgramPopupMenu);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("节目", programInfoPanel);
        tabbedPane.add("条件接收", casInfoPanel);
        JPanel psiInfoPanel = new JPanel(new MigLayout("fill, insets 7"));
        psiInfoPanel.add(tabbedPane, "grow");

        ComponentUtil.setTitledBorder(streamInfoPanel, "传输流信息");
        ComponentUtil.setTitledBorder(psiInfoPanel, "PSI信息");

        setLayout(new MigLayout("", "[grow][400!]", "[grow]"));
        add(streamInfoPanel, "grow");
        add(psiInfoPanel, "grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryStreamSnapshot();
            }
        });
    }

    @Override
    public void setupApplication(Application application)
    {
        this.application = application;
    }

    @Override
    public void setupDataSource(EventBus bus, M2TKDatabase database)
    {
        this.bus = bus;
        this.database = database;

        bus.register(this);
    }

    @Override
    public void setupMenu(JMenu menu)
    {
        JMenuItem item = new JMenuItem(getViewTitle());
        item.setIcon(getViewIcon());
        item.setAccelerator(KeyStroke.getKeyStroke("alt 1"));
        item.addActionListener(e -> bus.post(new ShowInfoViewEvent(this)));
        menu.add(item);
    }

    @Override
    public JComponent getViewComponent()
    {
        return this;
    }

    @Override
    public String getViewTitle()
    {
        return "传输流信息";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularAL.DATA_USAGE_20, 20, Color.decode("#F56040"));
    }

    @Override
    public List<JMenuItem> getContextMenuItem(MPEGProgram program)
    {
        if (program.isFreeAccess())
        {
            String text = (program.getName() == null)
                          ? String.format("播放 节目%d", program.getProgramNumber())
                          : String.format("播放 %s", program.getName());
            JMenuItem item = new JMenuItem();
            item.setText(text);
            item.addActionListener(e -> playProgram(program));
            return List.of(item);
        }
        return List.of();
    }

    @Override
    public List<JMenuItem> getContextMenuItem(ElementaryStream stream)
    {
        if (stream.isScrambled() ||
            !StrUtil.equalsAny(stream.getCategory(), StreamTypes.CATEGORY_VIDEO, StreamTypes.CATEGORY_AUDIO))
            return List.of();

        JMenuItem item = new JMenuItem();
        item.setText("播放 " + stream.getDescription());
        item.addActionListener(e -> playStream(stream));
        return List.of(item);
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryStreamSnapshot();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    public void setStreamObservers(List<StreamObserver> observers)
    {
        streamObservers.clear();
        streamObservers.addAll(observers);
    }

    public void setProgramObservers(List<ProgramObserver> observers)
    {
        programObservers.clear();
        programObservers.addAll(observers);
    }

    private void queryStreamSnapshot()
    {
        Supplier<StreamInfoSnapshot> query = () ->
        {
            StreamInfoSnapshot snapshot = new StreamInfoSnapshot();

            // 查询快照数据
            snapshot.source = database.getCurrentStreamSource();
            snapshot.streams = database.listElementaryStreams(true);
            snapshot.programs = database.listMPEGPrograms();
            snapshot.caStreams = database.listCASystemStreams();

            Map<String, SIService> serviceMap = database.listRegularSIServices()
                                                        .stream()
                                                        .collect(toMap(service -> String.format("%d.%d",
                                                                                                service.getTransportStreamId(),
                                                                                                service.getServiceId()),
                                                                       service -> service));
            for (MPEGProgram program : snapshot.programs)
            {
                String key = String.format("%d.%d", program.getTransportStreamId(), program.getProgramNumber());
                String programName = Optional.ofNullable(serviceMap.get(key))
                                             .map(SIService::getName)
                                             .orElse(null);
                program.setName(programName);
            }
            snapshot.programs.sort(Comparator.comparingInt(MPEGProgram::getProgramNumber));

            snapshot.source.setStreamCount(snapshot.streams.size());
            snapshot.source.setProgramCount(snapshot.programs.size());

            return snapshot;
        };

        Consumer<StreamInfoSnapshot> consumer = snapshot ->
        {
            streamInfoPanel.updateStreamInfo(snapshot.source, snapshot.streams);
            programInfoPanel.updateProgramList(snapshot.programs);
            casInfoPanel.updateStreamList(snapshot.caStreams);

            currentSource = snapshot.source;
        };

        AsyncQueryTask<StreamInfoSnapshot> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void playStream(ElementaryStream stream)
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        if (StrUtil.equals(stream.getCategory(), StreamTypes.CATEGORY_VIDEO))
            videoPid = stream.getStreamPid();

        if (StrUtil.equals(stream.getCategory(), StreamTypes.CATEGORY_AUDIO))
            audioPid = stream.getStreamPid();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", stream.getDescription());
            JOptionPane.showMessageDialog(null, text);
            log.info(text);
            return;
        }

        log.info("播放'流{}'，类型：{}", stream.getStreamPid(), stream.getDescription());

        if (videoPid != 0x1FFF)
            AssistantApp.getInstance().playVideo(currentSource.getUri(), videoPid);
        else
            AssistantApp.getInstance().playAudio(currentSource.getUri(), audioPid);
    }

    private void playProgram(MPEGProgram program)
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        for (ElementaryStream es : program.getElementaryStreams())
        {
            if (es.isScrambled())
                continue;

            if (StreamTypes.CATEGORY_VIDEO.equals(es.getCategory()))
                videoPid = es.getStreamPid();

            if (StreamTypes.CATEGORY_AUDIO.equals(es.getCategory()))
                audioPid = es.getStreamPid();
        }

        String programName = (program.getName() == null)
                             ? "节目" + program.getProgramNumber()
                             : program.getName();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = program.isFreeAccess()
                          ? programName + "无可播放内容"
                          : programName + "完全加扰，无法播放";
            JOptionPane.showMessageDialog(null, text);
            log.info(text);
            return;
        }

        log.info("播放'{}'，视频PID：{}，音频PID：{}", programName, videoPid, audioPid);

        AssistantApp.getInstance().playProgram(currentSource.getUri(), program.getProgramNumber());
    }

    private void showStreamPopupMenu(MouseEvent event, ElementaryStream stream)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        for (StreamObserver observer : streamObservers)
        {
            List<JMenuItem> menuItems = observer.getContextMenuItem(stream);
            for (JMenuItem menuItem : menuItems)
                popupMenu.add(menuItem);
        }
        popupMenu.show(event.getComponent(), event.getX(), event.getY());
    }

    private void showProgramPopupMenu(MouseEvent event, MPEGProgram program)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        for (ProgramObserver observer : programObservers)
        {
            List<JMenuItem> menuItems = observer.getContextMenuItem(program);
            for (JMenuItem menuItem : menuItems)
                popupMenu.add(menuItem);
        }
        popupMenu.show(event.getComponent(), event.getX(), event.getY());
    }
}
