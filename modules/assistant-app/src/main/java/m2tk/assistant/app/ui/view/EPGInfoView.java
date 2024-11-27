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
import lombok.Data;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.SIEvent;
import m2tk.assistant.api.domain.SIService;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.ServiceEventGuidePanel;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 5)
public class EPGInfoView extends JPanel implements InfoView
{
    private Application application;
    private ServiceEventGuidePanel serviceEventGuidePanel;
    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    @Data
    private static class EPGSnapshot
    {
        private List<SIService> services;
        private Map<SIService, List<SIEvent>> events;
    }

    public EPGInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        serviceEventGuidePanel = new ServiceEventGuidePanel();
        ComponentUtil.setTitledBorder(serviceEventGuidePanel, "EPG");

        setLayout(new MigLayout("fill"));
        add(serviceEventGuidePanel, "center, grow");
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
        return "EPG";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularAL.CALENDAR_AGENDA_24, 20, Color.decode("#00A4EF"));
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryServiceAndEvents();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryServiceAndEvents()
    {
        Supplier<EPGSnapshot> query = () ->
        {
            List<SIService> services = database.listSIServices();
            Map<SIService, List<SIEvent>> events = new HashMap<>();

            for (SIService service : services)
            {
                List<SIEvent> list = database.listSIEvents(service.getTransportStreamId(),
                                                           service.getOriginalNetworkId(),
                                                           service.getServiceId(),
                                                           false, false, false,
                                                           null, null);
                events.put(service, list);
            }

            EPGSnapshot snapshot = new EPGSnapshot();
            snapshot.setServices(services);
            snapshot.setEvents(events);
            return snapshot;
        };

        Consumer<EPGSnapshot> consumer = snapshot -> serviceEventGuidePanel.update(snapshot.getServices(), snapshot.getEvents());

        AsyncQueryTask<EPGSnapshot> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }
}
