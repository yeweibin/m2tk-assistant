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

import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.TR290Event;
import m2tk.assistant.api.domain.TR290Stats;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.TR290EventPanel;
import m2tk.assistant.app.ui.component.TR290StatsPanel;
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
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 3)
public class TR290InfoView extends JPanel implements InfoView
{
    private Application application;
    private TR290StatsPanel tr290StatsPanel;
    private TR290EventPanel tr290EventPanel;
    private JSplitPane splitPane;

    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    public TR290InfoView()
    {
        initUI();
    }

    private void initUI()
    {
        tr290StatsPanel = new TR290StatsPanel();
        tr290EventPanel = new TR290EventPanel();

        tr290StatsPanel.setPopupListener(this::showStatsPopupMenu);
        ComponentUtil.setTitledBorder(tr290StatsPanel, getViewTitle());

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(tr290StatsPanel);
        splitPane.setBottomComponent(tr290EventPanel);
        ComponentUtil.setTitledBorder(splitPane, getViewTitle());

        setLayout(new MigLayout("fill"));
        add(splitPane, "center, grow");

        tr290EventPanel.setVisible(false);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryTR290Events();
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
        item.setAccelerator(KeyStroke.getKeyStroke("alt 3"));
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
        return "TR 101.290";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularAL.BUG_20, 20, Color.decode("#FCAF45"));
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryTR290Events();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryTR290Events()
    {
        Supplier<TR290Stats> query = () -> database.getTR290Stats();
        Consumer<TR290Stats> consumer = tr290StatsPanel::update;

        AsyncQueryTask<TR290Stats> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void queryRecentTR290Events(String type)
    {
        Supplier<List<TR290Event>> query = () -> database.listTR290Events(type, 100);
        Consumer<List<TR290Event>> consumer = events -> {
            tr290EventPanel.update(events);
            tr290EventPanel.setVisible(true);
            splitPane.setDividerLocation(0.55);
        };

        AsyncQueryTask<List<TR290Event>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void showStatsPopupMenu(MouseEvent event, String type)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem item;

        if (StrUtil.isNotEmpty(type))
        {
            item = new JMenuItem("查看最近记录");
            item.addActionListener(e -> queryRecentTR290Events(type));
            popupMenu.add(item);
        }

        item = new JMenuItem("清空记录");
        item.addActionListener(e -> {
            database.clearTR290Events();
            tr290StatsPanel.reset();
        });
        popupMenu.add(item);

        popupMenu.show(event.getComponent(), event.getX(), event.getY());
    }
}
