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
import m2tk.assistant.dbi.entity.ProgramEntity;
import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.dbi.entity.StreamEntity;
import m2tk.assistant.ui.component.ProgramInfoPanel;
import m2tk.assistant.ui.component.SourceInfoPanel;
import m2tk.assistant.ui.component.StreamInfoPanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

public class StreamGeneralInfoView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private SourceInfoPanel sourceInfoPanel;
    private ProgramInfoPanel programInfoPanel;
    private StreamInfoPanel streamInfoPanel;
    private Timer timer1;
    private Timer timer2;
    private Timer timer3;

    public StreamGeneralInfoView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(1000, actionMap.get("querySourceInfo"));
        timer2 = new Timer(1000, actionMap.get("queryProgramInfo"));
        timer3 = new Timer(1000, actionMap.get("queryStreamInfo"));

        sourceInfoPanel = new SourceInfoPanel();
        streamInfoPanel = new StreamInfoPanel();
        programInfoPanel = new ProgramInfoPanel();

        // 强行让programInfoPanel与sourceInfoPanel保持同宽
        // 实在不知道怎么在MigLayout布局器里设置，并且保持左边的宽度固定。
        Dimension ds = sourceInfoPanel.getPreferredSize();
        Dimension dp = programInfoPanel.getMaximumSize();
        programInfoPanel.setMaximumSize(new Dimension(ds.width, dp.height));

        setLayout(new MigLayout("", "[fill][grow]", "[fill][grow]"));
        add(sourceInfoPanel);
        add(streamInfoPanel, "span 1 2, grow, wrap");
        add(programInfoPanel, "grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                timer1.start();
                timer2.start();
                timer3.start();
            }

            @Override
            public void componentHidden(ComponentEvent e)
            {
                timer1.stop();
                timer2.stop();
                timer3.stop();
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
            for (ProgramEntity program : mappings.keySet())
            {
                programs.add(new MPEGProgram(program, mappings.get(program), streamRegistry));
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

    public void reset()
    {
        programInfoPanel.resetProgramList();
        streamInfoPanel.resetStreamList();
        sourceInfoPanel.resetSourceInfo();
    }
}
