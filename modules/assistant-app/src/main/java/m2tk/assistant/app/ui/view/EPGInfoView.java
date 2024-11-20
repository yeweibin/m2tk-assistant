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
import m2tk.assistant.api.domain.SIEvent;
import m2tk.assistant.api.domain.SIService;
import m2tk.assistant.api.event.SourceAttachedEvent;
import m2tk.assistant.api.event.SourceDetachedEvent;
import m2tk.assistant.app.ui.component.ServiceEventGuidePanel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.pf4j.Extension;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension(ordinal = 3)
public class EPGInfoView extends JPanel implements InfoView
{
    private Application application;
    private ServiceEventGuidePanel serviceEventGuidePanel;
    private Timer timer;
    private volatile long transactionId;
    private EventBus bus;
    private M2TKDatabase database;

    public EPGInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(5000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer.stop();
            else
                queryServiceAndEvents();
        });

        serviceEventGuidePanel = new ServiceEventGuidePanel();
        ComponentUtil.setTitledBorder(serviceEventGuidePanel, "EPG", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(serviceEventGuidePanel, "center, grow");

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
        transactionId = 1;
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
        queryServiceAndEvents();
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
        this.application = application;
    }

    @Override
    public void setupMenu(JMenu menu)
    {

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
        return null;
    }

    public void reset()
    {
        serviceEventGuidePanel.reset();
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

    private void queryServiceAndEvents()
    {
        List<SIService> serviceList = new ArrayList<>();
        Map<SIService, List<SIEvent>> eventRegistry = new HashMap<>();

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
//            List<SIServiceEntity> services = databaseService.listServices(currentTransaction);
//            List<SIEventEntity> events = databaseService.listEvents(currentTransaction);
//
//            services.stream()
//                    .filter(service -> service.getServiceType() != 0x04 && service.getServiceType() != 0x05)
//                    .map(service -> new SIService())
////                    service.getTransportStreamId(),
////                                                  service.getOriginalNetworkId(),
////                                                  service.getServiceId(),
////                                                  service.getServiceTypeName(),
////                                                  service.getServiceName(),
////                                                  service.getServiceProvider()))
//                    .sorted(comparator1)
//                    .forEach(serviceList::add);
//
//            events.stream()
//                  .filter(event -> !event.isNvodReferenceEvent() && !event.isNvodTimeShiftedEvent())
//                  .map(event -> new SIEvent())
////        event.getTransportStreamId(),
////                                            event.getOriginalNetworkId(),
////                                            event.getServiceId(),
////                                            event.getEventId(),
////                                            event.getEventName(),
////                                            event.getEventDescription(),
////                                            event.getLanguageCode(),
////                                            event.getStartTime(),
////                                            event.getDuration(),
////                                            event.getEventType().equals(SIEventEntity.TYPE_SCHEDULE),
////                                            event.isPresentEvent()))
//                  .sorted(comparator2)
//                  .forEach(event -> {
//                      SIService service = new SIService();
////                      event.getTransportStreamId(),
////                                                        event.getOriginalNetworkId(),
////                                                        event.getServiceId(),
////                                                        "数字电视业务",
////                                                        String.format("未知业务（业务号：%d）", event.getServiceId()),
////                                                        "未知提供商");
//                      List<SIEvent> eventList = eventRegistry.computeIfAbsent(service, any -> new ArrayList<>());
//                      eventList.add(event);
//                  });

            return null;
        };

        Consumer<Void> consumer = nothing -> serviceEventGuidePanel.update(serviceList, eventRegistry);

        AsyncQueryTask<Void> task = new AsyncQueryTask<>(application,
                                                         query,
                                                         consumer);
        task.execute();
    }
}
