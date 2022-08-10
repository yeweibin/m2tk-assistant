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

import m2tk.assistant.Global;
import m2tk.assistant.analyzer.domain.MPEGProgram;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.*;
import m2tk.assistant.ui.component.CASystemInfoPanel;
import m2tk.assistant.ui.component.ProgramInfoPanel;
import m2tk.assistant.ui.component.SourceInfoPanel;
import m2tk.assistant.ui.component.StreamInfoPanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StreamGeneralInfoView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private SourceInfoPanel sourceInfoPanel;
    private ProgramInfoPanel programInfoPanel;
    private StreamInfoPanel streamInfoPanel;
    private CASystemInfoPanel casInfoPanel;
    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private Timer timer4;

    public StreamGeneralInfoView(FrameView view)
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
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = databaseService.getProgramMappings();
            Map<Integer, List<CAStreamEntity>> ecmGroups = databaseService.listECMGroups();
            for (ProgramEntity program : mappings.keySet())
            {
                programs.add(new MPEGProgram(program,
                                             ecmGroups.getOrDefault(program.getProgramNumber(),
                                                                    Collections.emptyList()),
                                             mappings.get(program),
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
        timer1.restart();
        timer2.restart();
        timer3.restart();
        timer4.restart();
    }

    public void startRefreshing()
    {
        timer1.start();
        timer2.start();
        timer3.start();
        timer4.start();
    }

    public void stopRefreshing()
    {
        timer1.stop();
        timer2.stop();
        timer3.stop();
        timer4.stop();
    }
}
