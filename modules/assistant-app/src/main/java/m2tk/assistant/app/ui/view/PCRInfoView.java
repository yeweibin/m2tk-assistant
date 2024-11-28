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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.PCRCheck;
import m2tk.assistant.api.domain.PCRStats;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.PCRChartPanel;
import m2tk.assistant.app.ui.component.PCRStatsPanel;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 4)
public class PCRInfoView extends JPanel implements InfoView
{
    private Application application;
    private PCRStatsPanel pcrStatsPanel;
    private PCRChartPanel pcrChartPanel;
    private JSplitPane splitPane;

    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    public PCRInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        pcrStatsPanel = new PCRStatsPanel();
        pcrStatsPanel.addPCRStatConsumer(stats -> {
            if (stats == null)
                pcrChartPanel.setVisible(false);
            else
                queryPCRRecords(stats.getPid());
        });

        pcrChartPanel = new PCRChartPanel();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.add(pcrStatsPanel, JSplitPane.TOP);
        splitPane.add(pcrChartPanel, JSplitPane.BOTTOM);
        ComponentUtil.setTitledBorder(splitPane, "PCR");

        setLayout(new MigLayout("fill"));
        add(splitPane, "center, grow");

        pcrChartPanel.setVisible(false);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryPCRStats();
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
        item.setAccelerator(KeyStroke.getKeyStroke("alt 4"));
        item.addActionListener(e -> {
            if (bus != null)
            {
                ShowInfoViewEvent event = new ShowInfoViewEvent(this);
                bus.post(event);
            }
        });
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
        return "PCR";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularAL.CLOCK_20, 20, Color.decode("#FFDC80"));
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryPCRStats();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryPCRStats()
    {
        Supplier<List<PCRStats>> query = () -> database.listPCRStats();
        Consumer<List<PCRStats>> consumer = pcrStatsPanel::update;

        AsyncQueryTask<List<PCRStats>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void queryPCRRecords(int stream)
    {
        Supplier<List<PCRCheck>> query = () -> database.getRecentPCRChecks(stream, 1000);
        Consumer<List<PCRCheck>> consumer = checks ->
        {
            pcrChartPanel.update(checks);
            pcrChartPanel.setVisible(true);
            splitPane.setDividerLocation(0.25);
        };

        AsyncQueryTask<List<PCRCheck>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }
}
