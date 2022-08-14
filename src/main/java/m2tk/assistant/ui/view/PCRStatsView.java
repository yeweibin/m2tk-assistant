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
import m2tk.assistant.analyzer.domain.SIEvent;
import m2tk.assistant.analyzer.domain.SIService;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.PCRStatEntity;
import m2tk.assistant.dbi.entity.SIEventEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.component.PCRStatsPanel;
import m2tk.assistant.ui.component.ServiceEventGuidePanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PCRStatsView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private PCRStatsPanel pcrStatsPanel;
    private Timer timer;

    public PCRStatsView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(500, actionMap.get("queryPCRStats"));

        pcrStatsPanel = new PCRStatsPanel();
        ComponentUtil.setTitledBorder(pcrStatsPanel, "PCR统计", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(pcrStatsPanel, "center, grow");

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

    public void reset()
    {
        pcrStatsPanel.reset();
        timer.restart();
    }

    public void startRefreshing()
    {
        timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    @Action
    public void queryPCRStats()
    {
        Supplier<List<PCRStatEntity>> query = () -> Global.getDatabaseService().listPCRStats();
        Consumer<List<PCRStatEntity>> consumer = pcrStatsPanel::update;

        AsyncQueryTask<List<PCRStatEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                        query,
                                                                        consumer);
        task.execute();
    }
}
