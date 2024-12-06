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
import m2tk.assistant.api.StreamObserver;
import m2tk.assistant.api.domain.ElementaryStream;
import m2tk.assistant.api.domain.StreamDensityBulk;
import m2tk.assistant.api.domain.StreamDensityStats;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.DensityChartPanel;
import m2tk.assistant.app.ui.component.DensityStatsPanel;
import m2tk.assistant.app.ui.event.ShowStreamDensityEvent;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 5)
public class DensityInfoView extends JPanel implements InfoView, StreamObserver
{
    private Application application;
    private DensityStatsPanel densityStatsPanel;
    private DensityChartPanel densityChartPanel;
    private JSplitPane splitPane;

    private M2TKDatabase database;
    private EventBus bus;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    private static class DensityContext
    {
        private List<StreamDensityStats> stats;
        private int bitrate;
    }

    public DensityInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        densityStatsPanel = new DensityStatsPanel();
        densityStatsPanel.addDensityStatConsumer(stats -> {
            if (stats == null)
                densityChartPanel.setVisible(false);
            else
                queryDensityBulks(stats.getPid());
        });

        densityChartPanel = new DensityChartPanel();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.add(densityStatsPanel, JSplitPane.TOP);
        splitPane.add(densityChartPanel, JSplitPane.BOTTOM);
        ComponentUtil.setTitledBorder(splitPane, getViewTitle());

        setLayout(new MigLayout("fill"));
        add(splitPane, "center, grow");

        densityChartPanel.setVisible(false);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryDensityStats();
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
        item.setAccelerator(KeyStroke.getKeyStroke("alt 5"));
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
        return "传输密度";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularMZ.PULSE_24, 20, Color.decode("#FFDC80"));
    }

    @Override
    public List<JMenuItem> getContextMenuItem(ElementaryStream stream)
    {
        JMenuItem item = new JMenuItem("查看流密度");
        item.addActionListener(e -> {
            bus.post(new ShowInfoViewEvent(this));
            bus.post(new ShowStreamDensityEvent(stream.getStreamPid()));
        });
        return List.of(item);
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryDensityStats();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onShowStreamDensityEvent(ShowStreamDensityEvent event)
    {
        queryDensityStatsAndSelect(event.getStream());
    }

    private void queryDensityStats()
    {
        Supplier<DensityContext> query = () -> {
            DensityContext context = new DensityContext();
            context.stats = database.listStreamDensityStats();
            context.bitrate = Optional.ofNullable(database.getCurrentStreamSource())
                                      .map(StreamSource::getBitrate)
                                      .orElse(0);
            return context;
        };
        Consumer<DensityContext> consumer = context -> densityStatsPanel.updateStats(context.bitrate, context.stats);

        AsyncQueryTask<DensityContext> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void queryDensityStatsAndSelect(int pid)
    {
        Supplier<DensityContext> query = () -> {
            DensityContext context = new DensityContext();
            context.stats = database.listStreamDensityStats();
            context.bitrate = Optional.ofNullable(database.getCurrentStreamSource())
                                      .map(StreamSource::getBitrate)
                                      .orElse(0);
            return context;
        };
        Consumer<DensityContext> consumer = context -> {
            densityStatsPanel.updateStats(context.bitrate, context.stats);
            densityStatsPanel.selectStreamStats(pid);
        };

        AsyncQueryTask<DensityContext> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void queryDensityBulks(int pid)
    {
        Supplier<List<StreamDensityBulk>> query = () -> database.getRecentStreamDensityBulks(pid, 2);
        Consumer<List<StreamDensityBulk>> consumer = checks ->
        {
            densityChartPanel.update(checks);
            densityChartPanel.setVisible(true);
            splitPane.setDividerLocation(0.3);
        };

        AsyncQueryTask<List<StreamDensityBulk>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }
}
