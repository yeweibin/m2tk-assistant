/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.ui.view;

import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.Global;
import m2tk.assistant.core.M2TKDatabase;
import m2tk.assistant.core.domain.*;
import m2tk.assistant.core.event.SourceAttachedEvent;
import m2tk.assistant.core.event.SourceDetachedEvent;
import m2tk.assistant.core.presets.StreamTypes;
import m2tk.assistant.ui.AssistantApp;
import m2tk.assistant.ui.component.CASystemInfoPanel;
import m2tk.assistant.ui.component.ProgramInfoPanel;
import m2tk.assistant.ui.component.SourceInfoPanel;
import m2tk.assistant.ui.component.StreamInfoPanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class StreamInfoView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private SourceInfoPanel sourceInfoPanel;
    private ProgramInfoPanel programInfoPanel;
    private StreamInfoPanel streamInfoPanel;
    private CASystemInfoPanel casInfoPanel;
    private JPopupMenu streamContextMenu;
    private JMenuItem streamContextMenuItem1;
    private JMenuItem streamContextMenuItem2;
    private JPopupMenu programContextMenu;
    private JMenuItem programContextMenuItem;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private Timer timer4;
    private volatile boolean stopped;
    private transient ElementaryStream selectedStream;
    private transient MPEGProgram selectedProgram;
    private EventBus bus;
    private M2TKDatabase database;

    public StreamInfoView(FrameView view)
    {
        frameView = view;
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(500, e -> {
            if (isVisible() && !stopped)
                querySourceInfo();
        });
        timer2 = new Timer(500, e -> {
            if (isVisible() && !stopped)
                queryProgramInfo();
        });
        timer3 = new Timer(500, e -> {
            if (isVisible() && !stopped)
                queryStreamInfo();
        });
        timer4 = new Timer(500, e -> {
            if (isVisible() && !stopped)
                queryCASystemInfo();
        });

        streamContextMenuItem1 = new JMenuItem();
        streamContextMenuItem1.addActionListener(e -> playStream());
        streamContextMenuItem2 = new JMenuItem();
        streamContextMenuItem2.addActionListener(e -> filterPrivateSection());
        streamContextMenu = new JPopupMenu();
        streamContextMenu.setLabel("播放");
        streamContextMenu.add(streamContextMenuItem1);
        streamContextMenu.add(streamContextMenuItem2);

        programContextMenuItem = new JMenuItem();
        programContextMenuItem.addActionListener(e -> playProgram());
        programContextMenu = new JPopupMenu();
        programContextMenu.setLabel("播放");
        programContextMenu.add(programContextMenuItem);

        sourceInfoPanel = new SourceInfoPanel();
        streamInfoPanel = new StreamInfoPanel();
        programInfoPanel = new ProgramInfoPanel();
        casInfoPanel = new CASystemInfoPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("节目", programInfoPanel);
        tabbedPane.add("条件接收", casInfoPanel);

        ComponentUtil.setTitledBorder(streamInfoPanel, "传输流信息", TitledBorder.LEFT);
        ComponentUtil.setTitledBorder(sourceInfoPanel, "基本信息", TitledBorder.LEFT);
        ComponentUtil.setTitledBorder(tabbedPane, "PSI信息", TitledBorder.LEFT);

        setLayout(new MigLayout("fill", "[grow][fill]", "[fill][grow]"));
        add(streamInfoPanel, "span 1 2, grow");
        add(sourceInfoPanel, "wrap");
        add(tabbedPane, "grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                refresh();
            }
        });

    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        log.info("source attached.");
        stopped = false;
        timer1.start();
        timer2.start();
        timer3.start();
        timer4.start();
        refresh();
    }

    @Subscribe
    public void onSourceDetachedEvent(SourceDetachedEvent event)
    {
        log.info("source detached.");
        stopped = true;
    }

    @Override
    public void refresh()
    {
        querySourceInfo();
        queryStreamInfo();
        queryProgramInfo();
        queryCASystemInfo();
    }

    @Override
    public void updateDataSource(EventBus bus, M2TKDatabase database)
    {
        this.bus = bus;
        this.database = database;
    }

    private void playStream()
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        if (selectedStream.isScrambled())
        {
            String text = String.format("流%d 被加扰，无法播放", selectedStream.getStreamPid());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        if (StreamTypes.CATEGORY_VIDEO.equals(selectedStream.getCategory()))
            videoPid = selectedStream.getStreamPid();

        if (StreamTypes.CATEGORY_AUDIO.equals(selectedStream.getCategory()))
            audioPid = selectedStream.getStreamPid();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", selectedStream.getDescription());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        log.info("播放'流{}'，类型：{}", selectedStream.getStreamPid(), selectedStream.getDescription());

        RxChannel channel = ProtocolManager.openRxChannel(Global.getLatestSourceUrl());
        if (videoPid != 0x1FFF)
            AssistantApp.getInstance().playVideo(Global.getLatestSourceUrl(), videoPid);
        else
            AssistantApp.getInstance().playAudio(Global.getLatestSourceUrl(), audioPid);
    }

    private void playProgram()
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        for (ElementaryStream es : selectedProgram.getElementaryStreams())
        {
            if (es.isScrambled())
                continue;

            if (StreamTypes.CATEGORY_VIDEO.equals(es.getCategory()))
                videoPid = es.getStreamPid();

            if (StreamTypes.CATEGORY_AUDIO.equals(es.getCategory()))
                audioPid = es.getStreamPid();
        }

        String programName = (selectedProgram.getName() == null)
                             ? "节目" + selectedProgram.getProgramNumber()
                             : selectedProgram.getName();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = selectedProgram.isFreeAccess()
                          ? programName + "无可播放内容"
                          : programName + "完全加扰，无法播放";
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        log.info("播放'{}'，视频PID：{}，音频PID：{}", programName, videoPid, audioPid);

        AssistantApp.getInstance().playProgram(Global.getLatestSourceUrl(), selectedProgram.getProgramNumber());
    }

    private void filterPrivateSection()
    {
        if (selectedStream.isScrambled())
        {
            String text = String.format("流%d 被加扰，无法解码", selectedStream.getStreamPid());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        int pid = 0x1FFF;
        if (StreamTypes.CATEGORY_DATA.equals(selectedStream.getCategory()) ||
            StreamTypes.CATEGORY_USER_PRIVATE.equals(selectedStream.getCategory()))
            pid = selectedStream.getStreamPid();

        if (pid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", selectedStream.getDescription());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        log.info("添加私有段过滤器：'流{}'，类型：{}", selectedStream.getStreamPid(), selectedStream.getDescription());

        Global.addUserPrivateSectionStreams(List.of(pid));
    }

    private void querySourceInfo()
    {
        Supplier<StreamSource> query = () -> database.getCurrentStreamSource();
        Consumer<StreamSource> consumer = sourceInfoPanel::updateSourceInfo;

        AsyncQueryTask<StreamSource> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                 query,
                                                                 consumer);
        task.execute();
    }

    private void queryProgramInfo()
    {
        Supplier<List<MPEGProgram>> query = () ->
        {
            Map<Integer, ElementaryStream> streamRegistry = database.listElementaryStreams(true)
                                                                           .stream()
                                                                           .collect(Collectors.toMap(ElementaryStream::getStreamPid,
                                                                                                     Function.identity()));

            List<MPEGProgram> programs = database.listMPEGPrograms();
            Map<String, SIService> serviceMap = database.listSIServices()
                                                               .stream()
                                                               .collect(toMap(service -> String.format("%d.%d",
                                                                                                       service.getTransportStreamId(),
                                                                                                       service.getServiceId()),
                                                                              service -> service));
            for (MPEGProgram program : programs)
            {
                String key = String.format("%d.%d", program.getTransportStreamId(), program.getProgramNumber());
                String programName = Optional.ofNullable(serviceMap.get(key))
                                             .map(SIService::getName)
                                             .orElse(null);

                program.setName(programName);
            }

            programs.sort(Comparator.comparingInt(MPEGProgram::getProgramNumber));
            return programs;
        };

        Consumer<List<MPEGProgram>> consumer = programInfoPanel::updateProgramList;

        AsyncQueryTask<List<MPEGProgram>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                      query,
                                                                      consumer);
        task.execute();
    }

    private void queryStreamInfo()
    {
        Supplier<List<ElementaryStream>> query = () -> database.listElementaryStreams(true);
        Consumer<List<ElementaryStream>> consumer = streamInfoPanel::updateStreamList;

        AsyncQueryTask<List<ElementaryStream>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                           query,
                                                                           consumer);
        task.execute();
    }

    private void queryCASystemInfo()
    {
        Supplier<List<CASystemStream>> query = () -> database.listCASystemStreams();
        Consumer<List<CASystemStream>> consumer = casInfoPanel::updateStreamList;

        AsyncQueryTask<List<CASystemStream>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                         query,
                                                                         consumer);
        task.execute();
    }

    public void reset()
    {
        programInfoPanel.resetProgramList();
        streamInfoPanel.resetStreamList();
        sourceInfoPanel.resetSourceInfo();
        casInfoPanel.resetStreamList();

        if (!stopped)
        {
            timer1.restart();
            timer2.restart();
            timer3.restart();
            timer4.restart();
            streamInfoPanel.setPopupListener(null);
            programInfoPanel.setPopupListener(null);
        }
    }

    public void startRefreshing()
    {
        if (!stopped)
        {
            timer1.start();
            timer2.start();
            timer3.start();
            timer4.start();
            streamInfoPanel.setPopupListener(null);
            programInfoPanel.setPopupListener(null);
        }
    }

    public void stopRefreshing()
    {
        timer1.stop();
        timer2.stop();
        timer3.stop();
        timer4.stop();
        streamInfoPanel.setPopupListener(this::showStreamPopupMenu);
        programInfoPanel.setPopupListener(this::showProgramPopupMenu);
    }

    private void showStreamPopupMenu(MouseEvent event, ElementaryStream stream)
    {
        selectedStream = stream;
        streamContextMenuItem1.setText("播放 " + selectedStream.getDescription());
        streamContextMenuItem2.setText("过滤私有段");

        boolean playable = !stream.isScrambled() &&
                           StrUtil.equalsAny(stream.getCategory(), StreamTypes.CATEGORY_VIDEO, StreamTypes.CATEGORY_AUDIO);
        boolean filterable = !stream.isScrambled() &&
                             StrUtil.equalsAny(stream.getCategory(), StreamTypes.CATEGORY_DATA, StreamTypes.CATEGORY_USER_PRIVATE);
        streamContextMenuItem1.setVisible(playable);
        streamContextMenuItem2.setVisible(filterable);

        if (playable || filterable)
            streamContextMenu.show(event.getComponent(), event.getX(), event.getY());
    }

    private void showProgramPopupMenu(MouseEvent event, MPEGProgram program)
    {
        selectedProgram = program;
        String text = (selectedProgram.getName() == null)
                      ? String.format("播放 节目%d", selectedProgram.getProgramNumber())
                      : String.format("播放 %s", selectedProgram.getName());
        programContextMenuItem.setText(text);

        if (program.isFreeAccess())
            programContextMenu.show(event.getComponent(), event.getX(), event.getY());
    }
}
