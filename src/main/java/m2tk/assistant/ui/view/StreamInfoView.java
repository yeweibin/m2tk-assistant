/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

import m2tk.assistant.AssistantApp;
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
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.util.RxChannelInputStream;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
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

public class StreamInfoView extends JPanel
{
    private final transient FrameView frameView;
    private final ActionMap actionMap;
    private SourceInfoPanel sourceInfoPanel;
    private ProgramInfoPanel programInfoPanel;
    private StreamInfoPanel streamInfoPanel;
    private CASystemInfoPanel casInfoPanel;
    private JPopupMenu programContextMenu;
    private JMenuItem programContextMenuItem;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private Timer timer4;
    private MPEGProgram selectedProgram;

    public StreamInfoView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(500, actionMap.get("querySourceInfo"));
        timer2 = new Timer(500, actionMap.get("queryProgramInfo"));
        timer3 = new Timer(500, actionMap.get("queryStreamInfo"));
        timer4 = new Timer(500, actionMap.get("queryCASystemInfo"));

        programContextMenuItem = new JMenuItem();
        programContextMenuItem.setAction(actionMap.get("playProgram"));
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
                startRefreshing();
            }

            @Override
            public void componentHidden(ComponentEvent e)
            {
                stopRefreshing();
            }
        });
    }

    @Action
    public void playProgram()
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

        if (videoPid == 0x1FFF && audioPid == 0x1FFF)
        {
            String text= !selectedProgram.isFreeAccess()
                         ? "节目完全加扰，无法播放"
                         : "无可播放内容";
            JOptionPane.showMessageDialog(frameView.getFrame(), text);
            return;
        }

        RxChannel channel = ProtocolManager.openRxChannel(Global.getInputResource());
        AssistantApp.getInstance().playVideoAndAudio(new RxChannelInputStream(channel), videoPid, audioPid);
    }

    @Action
    public void querySourceInfo()
    {
        Supplier<SourceEntity> query = () -> Global.getDatabaseService().getSource();
        Consumer<SourceEntity> consumer = sourceInfoPanel::updateSourceInfo;

        AsyncQueryTask<SourceEntity> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                 query,
                                                                 consumer);
        task.execute();
    }

    @Action
    public void queryProgramInfo()
    {
        Supplier<List<MPEGProgram>> query = () ->
        {
            DatabaseService databaseService = Global.getDatabaseService();
            Map<Integer, StreamEntity> streamRegistry = databaseService.getStreamRegistry();

            List<MPEGProgram> programs = new ArrayList<>();
            Map<String, SIServiceEntity> serviceMap = databaseService.listServices()
                                                                     .stream()
                                                                     .collect(toMap(service -> String.format("%d.%d",
                                                                                                             service.getTransportStreamId(),
                                                                                                             service.getServiceId()),
                                                                                    service -> service));
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = databaseService.getProgramMappings();
            Map<Integer, List<CAStreamEntity>> ecmGroups = databaseService.listECMGroups();
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

    @Action
    public void queryStreamInfo()
    {
        Supplier<List<StreamEntity>> query = () -> Global.getDatabaseService().listStreams();
        Consumer<List<StreamEntity>> consumer = streamInfoPanel::updateStreamList;

        AsyncQueryTask<List<StreamEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                       query,
                                                                       consumer);
        task.execute();
    }

    @Action
    public void queryCASystemInfo()
    {
        Supplier<List<CAStreamEntity>> query = () -> Global.getDatabaseService().listCAStreams();
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

        if (Global.getStreamAnalyser().isRunning())
        {
            timer1.restart();
            timer2.restart();
            timer3.restart();
            timer4.restart();
            programInfoPanel.setPopupListener(null);
        }
    }

    public void startRefreshing()
    {
        if (Global.getStreamAnalyser().isRunning())
        {
            timer1.start();
            timer2.start();
            timer3.start();
            timer4.start();
            programInfoPanel.setPopupListener(null);
        }
    }

    public void stopRefreshing()
    {
        timer1.stop();
        timer2.stop();
        timer3.stop();
        timer4.stop();
        programInfoPanel.setPopupListener(this::showProgramPopupMenu);
    }

    private void showProgramPopupMenu(MouseEvent event, MPEGProgram program)
    {
        selectedProgram = program;
        String text = (selectedProgram.getProgramName() == null)
                      ? String.format("播放 节目%d", selectedProgram.getProgramNumber())
                      : String.format("播放 %s", selectedProgram.getProgramName());
        programContextMenuItem.setText(text);
        programContextMenu.show(event.getComponent(), event.getX(), event.getY());
    }
}
