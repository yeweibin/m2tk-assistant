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

package m2tk.assistant.app.ui.view;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.PCRCheck;
import m2tk.assistant.api.domain.PCRStats;
import m2tk.assistant.api.event.SourceAttachedEvent;
import m2tk.assistant.api.event.SourceDetachedEvent;
import m2tk.assistant.app.ui.component.PCRChartPanel;
import m2tk.assistant.app.ui.component.PCRStatsPanel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.pf4j.Extension;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

@Extension(ordinal = 7)
public class PCRInfoView extends JPanel implements InfoView
{
    private PCRStatsPanel pcrStatsPanel;
    private PCRChartPanel pcrChartPanel;
    private JSplitPane splitPane;
    private Timer timer;
    private volatile long transactionId;
    private transient PCRStats selectedPCRStat;
    private EventBus bus;
    private M2TKDatabase database;

    public PCRInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(500, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer.stop();
            else
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

        transactionId = -1;
    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        transactionId = 1; //event.getSource().getTransactionId();
        timer.start();
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
        queryPCRStats();
    }

    @Override
    public void setupDataSource(EventBus bus, M2TKDatabase database)
    {
        this.bus = bus;
        this.database = database;
    }

    @Override
    public void setupApplication(Application application)
    {

    }

    @Override
    public void setupMenu(JMenu menu)
    {

    }

    @Override
    public JComponent getViewComponent()
    {
        return null;
    }

    @Override
    public String getViewTitle()
    {
        return "PCR";
    }

    @Override
    public Icon getViewIcon()
    {
        return null;
    }

    public void reset()
    {
        pcrStatsPanel.reset();
        pcrChartPanel.reset();

        if (transactionId != -1)
            timer.restart();
    }

    public void startRefreshing()
    {
        if (transactionId != -1)
            timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    private void updatePCRChart(List<PCRCheck> checks)
    {
        pcrChartPanel.setVisible(true);
        pcrChartPanel.update(checks);
        splitPane.setDividerLocation(0.25);
    }

    private void queryPCRStats()
    {
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1)
//            return;
//
//        Supplier<List<PCRStatViewEntity>> query = () -> Global.getDatabaseService().listPCRStats0(currentTransaction);
//        Consumer<List<PCRStatViewEntity>> consumer = pcrStatsPanel::update;
//
//        AsyncQueryTask<List<PCRStatViewEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                                            query,
//                                                                            consumer);
//        task.execute();
    }

    private void queryPCRRecords()
    {
//        PCRStatViewEntity target = selectedPCRStat;
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1 || target == null)
//            return;
//
//        Supplier<List<PCRCheckEntity>> query = () -> Global.getDatabaseService()
//                                                           .getRecentPCRChecks0(currentTransaction, target.getPid(), 1000);
//
//        Consumer<List<PCRCheckEntity>> consumer = this::updatePCRChart;
//        AsyncQueryTask<List<PCRCheckEntity>> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                                         query,
//                                                                         consumer);
//        task.execute();
    }
}
