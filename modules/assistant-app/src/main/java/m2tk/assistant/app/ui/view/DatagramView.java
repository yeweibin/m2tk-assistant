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
import m2tk.assistant.api.event.InfoViewRefreshingEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.event.SourceStateEvent;
import m2tk.assistant.app.ui.component.SectionDatagramPanel;
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

@Extension(ordinal = 7)
public class DatagramView extends JPanel implements InfoView
{
    private SectionDatagramPanel sectionDatagramPanel;
    private Timer timer;
    private EventBus bus;
    private M2TKDatabase database;
    private volatile long transactionId;

    public DatagramView()
    {
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(10000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer.stop();
            else
                queryDatagrams();
        });

        sectionDatagramPanel = new SectionDatagramPanel();
        ComponentUtil.setTitledBorder(sectionDatagramPanel, "PSI/SI");

        setLayout(new MigLayout("fill"));
        add(sectionDatagramPanel, "center, grow");

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

    public void refresh()
    {
        queryDatagrams();
    }

    @Override
    public void setupApplication(Application application)
    {
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
        JMenuItem item = new JMenuItem("数据结构");
        item.setIcon(getViewIcon());
        item.setAccelerator(KeyStroke.getKeyStroke("alt 7"));
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
        return "数据结构";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularMZ.TEXT_BULLET_LIST_TREE_20, 20, Color.decode("#3366FF"));
    }

    @Subscribe
    public void onSourceStateEvent(SourceStateEvent event)
    {
        switch (event.state())
        {
            case SourceStateEvent.ATTACHED ->
            {
                transactionId = 1;
                //event.getSource().getTransactionId();
                timer.start();
                refresh();
            }
            case SourceStateEvent.DETACHED ->
            {
                transactionId = -1;
            }
        }
    }

    @Subscribe
    public void onInfoViewRefreshingEvent(InfoViewRefreshingEvent event)
    {
        if (event.enabled())
        {
            if (transactionId != -1)
                timer.start();
        } else
        {
            timer.stop();
        }
    }

    public void reset()
    {
        sectionDatagramPanel.reset();
        if (transactionId != -1)
            timer.restart();
    }

    private void queryDatagrams()
    {
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1)
//            return;
//
//        Supplier<Map<String, List<PrivateSectionEntity>>> query = () ->
//                Global.getDatabaseService().getSectionGroups(currentTransaction);
//
//        Consumer<Map<String, List<PrivateSectionEntity>>> consumer = sectionDatagramPanel::update;
//
//        AsyncQueryTask<Map<String, List<PrivateSectionEntity>>> task =
//                new AsyncQueryTask<>(frameView.getApplication(), query, consumer);
//        task.execute();
    }
}
