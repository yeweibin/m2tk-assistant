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

import com.google.common.eventbus.Subscribe;
import m2tk.assistant.Global;
import m2tk.assistant.dbi.entity.PCRCheckEntity;
import m2tk.assistant.dbi.entity.PCRStatEntity;
import m2tk.assistant.ui.component.PCRChartPanel;
import m2tk.assistant.ui.component.PCRStatsPanel;
import m2tk.assistant.ui.event.SourceChangedEvent;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PCRInfoView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private PCRStatsPanel pcrStatsPanel;
    private PCRChartPanel pcrChartPanel;
    private JSplitPane splitPane;
    private Timer timer;
    private volatile long transactionId;
    private transient PCRStatEntity selectedPCRStat;

    public PCRInfoView(FrameView view)
    {
        frameView = view;
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (!Global.getStreamAnalyser().isRunning())
                timer.stop();

            queryPCRStats();
        });

        pcrStatsPanel = new PCRStatsPanel();
        pcrStatsPanel.addPCRStatConsumer(stat -> {
            if (stat == null)
            {
                pcrChartPanel.setVisible(false);
            } else
            {
                selectedPCRStat = stat;
                queryPCRRecords();
            }
        });

        pcrChartPanel = new PCRChartPanel();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.add(pcrStatsPanel, JSplitPane.TOP);
        splitPane.add(pcrChartPanel, JSplitPane.BOTTOM);
        ComponentUtil.setTitledBorder(splitPane, "PCR", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(splitPane, "center, grow");

        pcrChartPanel.setVisible(false);

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

    @Override
    public void refresh()
    {
        queryPCRStats();
    }

    @Subscribe
    public void onSourceChanged(SourceChangedEvent event)
    {
        transactionId = event.getTransactionId();
    }

    public void reset()
    {
        pcrStatsPanel.reset();
        pcrChartPanel.reset();

        if (Global.getStreamAnalyser().isRunning())
            timer.restart();
    }

    public void startRefreshing()
    {
        if (Global.getStreamAnalyser().isRunning())
            timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    private void updatePCRChart(List<PCRCheckEntity> checks)
    {
        pcrChartPanel.setVisible(true);
        pcrChartPanel.update(checks);
        splitPane.setDividerLocation(0.25);
    }

    private void queryPCRStats()
    {
        long currentTransactionId = (transactionId == -1) ? Global.getCurrentTransactionId() : transactionId;

        Supplier<List<PCRStatEntity>> query = () -> Global.getDatabaseService().listPCRStats(currentTransactionId);
        Consumer<List<PCRStatEntity>> consumer = pcrStatsPanel::update;

        AsyncQueryTask<List<PCRStatEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                        query,
                                                                        consumer);
        task.execute();
    }

    private void queryPCRRecords()
    {
        PCRStatEntity target = selectedPCRStat;
        if (target == null)
            return;

        long currentTransactionId = (transactionId == -1) ? Global.getCurrentTransactionId() : transactionId;

        Supplier<List<PCRCheckEntity>> query = () -> Global.getDatabaseService()
                                                           .getRecentPCRChecks(currentTransactionId,
                                                                               target.getPid(),
                                                                               1000);
        Consumer<List<PCRCheckEntity>> consumer = this::updatePCRChart;
        AsyncQueryTask<List<PCRCheckEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                         query,
                                                                         consumer);
        task.execute();
    }
}
