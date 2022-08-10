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
import m2tk.assistant.dbi.entity.SIEventEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
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

public class EPGInfoView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private ServiceEventGuidePanel serviceEventGuidePanel;
    private Timer timer;

    public EPGInfoView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(1000, actionMap.get("queryServiceAndEvents"));

        serviceEventGuidePanel = new ServiceEventGuidePanel();
        ComponentUtil.setTitledBorder(serviceEventGuidePanel, "EPG全览", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(serviceEventGuidePanel, "center, grow");

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
        serviceEventGuidePanel.reset();
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
    public void queryServiceAndEvents()
    {
        List<SIService> serviceList = new ArrayList<>();
        List<SIEvent> eventList = new ArrayList<>();

        Comparator<SIService> comparator1 = (s1, s2) -> {
            if (s1.getOriginalNetworkId() != s2.getOriginalNetworkId())
                return Integer.compare(s1.getOriginalNetworkId(), s2.getOriginalNetworkId());
            if (s1.getTransportStreamId() != s2.getTransportStreamId())
                return Integer.compare(s1.getTransportStreamId(), s2.getTransportStreamId());
            return Integer.compare(s1.getServiceId(), s2.getServiceId());
        };

        Comparator<SIEvent> comparator2 = (e1, e2) -> {
            if (e1.getOriginalNetworkId() != e2.getOriginalNetworkId())
                return Integer.compare(e1.getOriginalNetworkId(), e2.getOriginalNetworkId());
            if (e1.getTransportStreamId() != e2.getTransportStreamId())
                return Integer.compare(e1.getTransportStreamId(), e2.getTransportStreamId());
            if (e1.getServiceId() != e2.getServiceId())
                return Integer.compare(e1.getServiceId(), e2.getServiceId());
            return e1.getStartTime().compareTo(e2.getStartTime());
        };

        Supplier<Void> query = () -> {
            DatabaseService databaseService = Global.getDatabaseService();
            List<SIServiceEntity> services = databaseService.listServices();
            List<SIEventEntity> events = databaseService.listEvents();

            for (SIServiceEntity service : services)
            {
                SIService srv = new SIService(service.getTransportStreamId(),
                                              service.getOriginalNetworkId(),
                                              service.getServiceId(),
                                              service.getServiceTypeName(),
                                              service.getServiceName(),
                                              service.getServiceProvider());
                serviceList.add(srv);
            }

            for (SIEventEntity event : events)
            {
                SIEvent evt = new SIEvent(event.getTransportStreamId(),
                                          event.getOriginalNetworkId(),
                                          event.getServiceId(),
                                          event.getEventId(),
                                          event.getEventName(),
                                          event.getEventDescription(),
                                          event.getLanguageCode(),
                                          event.getStartTime(),
                                          event.getDuration(),
                                          event.getEventType().equals(SIEventEntity.TYPE_SCHEDULE),
                                          event.isPresentEvent());
                eventList.add(evt);
            }

            serviceList.sort(comparator1);
            eventList.sort(comparator2);
            return null;
        };

        Consumer<Void> consumer = nothing ->
        {
            serviceEventGuidePanel.updateServiceList(serviceList);
            serviceEventGuidePanel.updateEventRegistry(eventList);
        };

        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                         query,
                                                         consumer);
        task.execute();
    }
}
