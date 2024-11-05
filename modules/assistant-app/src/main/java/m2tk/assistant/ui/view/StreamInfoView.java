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
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.ui.AssistantApp;
import m2tk.assistant.Global;
import m2tk.assistant.analyzer.domain.ElementaryStream;
import m2tk.assistant.analyzer.domain.MPEGProgram;
import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.*;
import m2tk.assistant.ui.component.CASystemInfoPanel;
import m2tk.assistant.ui.component.ProgramInfoPanel;
import m2tk.assistant.ui.component.SourceInfoPanel;
import m2tk.assistant.ui.component.StreamInfoPanel;
import m2tk.assistant.ui.event.SourceAttachedEvent;
import m2tk.assistant.ui.event.SourceDetachedEvent;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private volatile long transactionId;
    private transient StreamEntity selectedStream;
    private transient MPEGProgram selectedProgram;

    public StreamInfoView(FrameView view)
    {
        frameView = view;
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer1.stop();
            else
                querySourceInfo();
        });
        timer2 = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer2.stop();
            else
                queryProgramInfo();
        });
        timer3 = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer3.stop();
            else
                queryStreamInfo();
        });
        timer4 = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer4.stop();
            else
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

        Global.registerSubscriber(this);
        transactionId = -1;
    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        transactionId = event.getSource().getTransactionId();
        timer1.start();
        timer2.start();
        timer3.start();
        timer4.start();
        refresh();
    }

    @Subscribe
    public void onSourceDetachedEvent(SourceDetachedEvent event)
    {
        transactionId = -1;
    }

    @Override
    public void refresh()
    {
        querySourceInfo();
        queryStreamInfo();
        queryProgramInfo();
        queryCASystemInfo();
    }

    private void playStream()
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        if (selectedStream.isScrambled())
        {
            String text = String.format("流%d 被加扰，无法播放", selectedStream.getPid());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        if (StreamTypes.CATEGORY_VIDEO.equals(selectedStream.getCategory()))
            videoPid = selectedStream.getPid();

        if (StreamTypes.CATEGORY_AUDIO.equals(selectedStream.getCategory()))
            audioPid = selectedStream.getPid();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", selectedStream.getDescription());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        log.info("播放'流{}'，类型：{}", selectedStream.getPid(), selectedStream.getDescription());

//        RxChannel channel = ProtocolManager.openRxChannel(Global.getLatestSourceUrl());
        if (videoPid != 0x1FFF)
            AssistantApp.getInstance().playVideo(Global.getLatestSourceUrl(), videoPid);
        else
            AssistantApp.getInstance().playAudio(Global.getLatestSourceUrl(), audioPid);
    }

    private void playProgram()
    {
        int videoPid = 0x1FFF;
        int audioPid = 0x1FFF;
        for (ElementaryStream es : selectedProgram.getElementList())
        {
            if (es.isScrambled())
                continue;

            if (StreamTypes.CATEGORY_VIDEO.equals(es.getCategory()))
                videoPid = es.getStreamPid();

            if (StreamTypes.CATEGORY_AUDIO.equals(es.getCategory()))
                audioPid = es.getStreamPid();
        }

        String programName = (selectedProgram.getProgramName() == null)
                             ? "节目" + selectedProgram.getProgramNumber()
                             : selectedProgram.getProgramName();

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text = !selectedProgram.isFreeAccess()
                          ? programName + "完全加扰，无法播放"
                          : programName + "无可播放内容";
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
            String text = String.format("流%d 被加扰，无法解码", selectedStream.getPid());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        int pid = 0x1FFF;
        if (StreamTypes.CATEGORY_DATA.equals(selectedStream.getCategory()) ||
            StreamTypes.CATEGORY_USER_PRIVATE.equals(selectedStream.getCategory()))
            pid = selectedStream.getPid();

        if (pid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", selectedStream.getDescription());
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            log.info(text);
            return;
        }

        log.info("添加私有段过滤器：'流{}'，类型：{}", selectedStream.getPid(), selectedStream.getDescription());

        Global.addUserPrivateSectionStreams(List.of(pid));
    }

    private void querySourceInfo()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        Supplier<SourceEntity> query = () -> Global.getDatabaseService().getSource(currentTransaction);
        Consumer<SourceEntity> consumer = sourceInfoPanel::updateSourceInfo;

        AsyncQueryTask<SourceEntity> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                 query,
                                                                 consumer);
        task.execute();
    }

    private void queryProgramInfo()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        Supplier<List<MPEGProgram>> query = () ->
        {
            DatabaseService databaseService = Global.getDatabaseService();
            Map<Integer, StreamEntity> streamRegistry = databaseService.getStreamRegistry(currentTransaction);

            List<MPEGProgram> programs = new ArrayList<>();
            Map<String, SIServiceEntity> serviceMap = databaseService.listServices(currentTransaction)
                                                                     .stream()
                                                                     .collect(toMap(service -> String.format("%d.%d",
                                                                                                             service.getTransportStreamId(),
                                                                                                             service.getServiceId()),
                                                                                    service -> service));
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = databaseService.getProgramMappings(currentTransaction);
            Map<Integer, List<CAStreamEntity>> ecmGroups = databaseService.listECMGroups(currentTransaction);
            for (Map.Entry<ProgramEntity, List<ProgramStreamMappingEntity>> mapping : mappings.entrySet())
            {
                ProgramEntity program = mapping.getKey();
                List<ProgramStreamMappingEntity> mappedStreams = mapping.getValue();
                String key = String.format("%d.%d", program.getTransportStreamId(), program.getProgramNumber());
                String programName = Optional.ofNullable(serviceMap.get(key))
                                             .map(SIServiceEntity::getServiceName)
                                             .orElse(null);

                programs.add(new MPEGProgram(programName,
                                             program,
                                             ecmGroups.getOrDefault(program.getProgramNumber(),
                                                                    Collections.emptyList()),
                                             mappedStreams,
                                             streamRegistry));
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
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        Supplier<List<StreamEntity>> query = () -> Global.getDatabaseService().listStreams(currentTransaction);
        Consumer<List<StreamEntity>> consumer = streamInfoPanel::updateStreamList;

        AsyncQueryTask<List<StreamEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                       query,
                                                                       consumer);
        task.execute();
    }

    private void queryCASystemInfo()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        Supplier<List<CAStreamEntity>> query = () -> Global.getDatabaseService().listCAStreams(currentTransaction);
        Consumer<List<CAStreamEntity>> consumer = casInfoPanel::updateStreamList;

        AsyncQueryTask<List<CAStreamEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
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

        if (transactionId != -1)
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
        if (transactionId != -1)
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

    private void showStreamPopupMenu(MouseEvent event, StreamEntity stream)
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
        String text = (selectedProgram.getProgramName() == null)
                      ? String.format("播放 节目%d", selectedProgram.getProgramNumber())
                      : String.format("播放 %s", selectedProgram.getProgramName());
        programContextMenuItem.setText(text);

        if (program.isPlayable())
            programContextMenu.show(event.getComponent(), event.getX(), event.getY());
    }
}
