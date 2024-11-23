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
import m2tk.assistant.api.domain.TR290Event;
import m2tk.assistant.api.domain.TR290Stats;
import m2tk.assistant.api.event.InfoViewRefreshingEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.event.SourceStateEvent;
import m2tk.assistant.api.presets.TR290ErrorTypes;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 3)
public class TR290InfoView extends JPanel implements InfoView
{
    private Application application;
    private TR290StatsPanel tr290StatsPanel;
    private Timer timer;
    private EventBus bus;
    private M2TKDatabase database;
    private volatile long transactionId;

    public TR290InfoView()
    {
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(1000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer.stop();
            else
                queryTR290Events();
        });

        tr290StatsPanel = new TR290StatsPanel();
        ComponentUtil.setTitledBorder(tr290StatsPanel, "TR 101 290");

        setLayout(new MigLayout("fill"));
        add(tr290StatsPanel, "center, grow");

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
        JMenuItem item = new JMenuItem("TR290");
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
        return "TR290";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularAL.BUG_20, 20, Color.decode("#FCAF45"));
    }

    public void reset()
    {
        tr290StatsPanel.reset();
        if (transactionId != -1)
            timer.restart();
    }

    @Subscribe
    public void onSourceStateEvent(SourceStateEvent event)
    {
        switch (event.state())
        {
            case SourceStateEvent.ATTACHED ->
            {
                transactionId = 1; //event.getSource().getTransactionId();
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

    public void refresh()
    {
        queryTR290Events();
    }

    private void queryTR290Events()
    {
        Supplier<TR290Stats> query = () -> {
            Map<String, TR290Stats> statsMap = new HashMap<>();
//                Global.getDatabaseService()
//                                                          .listTR290Stats(currentTransaction)
//                                                          .stream()
//                                                          .collect(toMap(TR290StatEntity::getIndicator,
//                                                                         entity -> entity));

            TR290Stats stats = new TR290Stats();
            Consumer<TR290Stats> operator = stat -> {
                if (stat != null)
                {
                    stats.setStat("123",
                                  stat.getErrorCount("123"),
                                  new TR290Event());
//                    stat.getLastEventTimestamp(),
//                                                 stat.getIndicator(),
//                                                 stat.getLastEventDescription()));
                }
            };

            operator.accept(statsMap.get(TR290ErrorTypes.TS_SYNC_LOSS));
            operator.accept(statsMap.get(TR290ErrorTypes.SYNC_BYTE_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PAT_ERROR_2));
            operator.accept(statsMap.get(TR290ErrorTypes.CONTINUITY_COUNT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PMT_ERROR_2));
            operator.accept(statsMap.get(TR290ErrorTypes.PID_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.TRANSPORT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.CRC_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_REPETITION_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_ACCURACY_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.CAT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.NIT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.NIT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.SI_REPETITION_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.UNREFERENCED_PID));
            operator.accept(statsMap.get(TR290ErrorTypes.SDT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.SDT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.EIT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.EIT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.RST_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.TDT_ERROR));

            return stats;
        };

        Consumer<TR290Stats> consumer = tr290StatsPanel::update;

        AsyncQueryTask<TR290Stats> task = new AsyncQueryTask<>(application,
                                                               query,
                                                               consumer);
        task.execute();
    }
}
