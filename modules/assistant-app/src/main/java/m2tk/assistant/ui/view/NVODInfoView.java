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

package m2tk.assistant.ui.view;

import com.google.common.eventbus.Subscribe;
import m2tk.assistant.Global;
import m2tk.assistant.core.domain.SIEvent;
import m2tk.assistant.core.domain.SIService;
import m2tk.assistant.core.domain.SIServiceLocator;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SIEventEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.component.NVODServiceEventGuidePanel;
import m2tk.assistant.ui.event.SourceAttachedEvent;
import m2tk.assistant.ui.event.SourceDetachedEvent;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NVODInfoView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private NVODServiceEventGuidePanel serviceEventGuidePanel;
    private Timer timer;
    private volatile long transactionId;

    public NVODInfoView(FrameView view)
    {
        frameView = view;
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
                queryServiceAndEvents();
        });

        serviceEventGuidePanel = new NVODServiceEventGuidePanel();
        ComponentUtil.setTitledBorder(serviceEventGuidePanel, "NVOD", TitledBorder.LEFT);

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

        Global.registerSubscriber(this);
        transactionId = -1;
    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        transactionId = event.getSource().getTransactionId();
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
        long currentTransactionId = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransactionId == -1)
            return;

        Map<Long, SIService> serviceRegistry = new HashMap<>();
        Map<Long, SIEvent> eventRegistry = new HashMap<>();

        Supplier<Void> query = () -> {
            DatabaseService databaseService = Global.getDatabaseService();
            List<SIServiceEntity> services = databaseService.listNVODServices(currentTransactionId);
            List<SIEventEntity> events = databaseService.listNVODEvents(currentTransactionId);

            services.forEach(service -> {
                SIService nvodService = service.isNvodReferenceService() ? ofReference(service)
                                                                         : ofTimeShifted(service);
                serviceRegistry.put(nvodService.getRef(), nvodService);
            });

            // 先筛查所有的引用事件
            for (SIEventEntity event : events)
            {
                if (event.isNvodReferenceEvent())
                {
                    SIEvent referenceEvent = ofReference(event);
                    eventRegistry.put(referenceEvent.getRef(), referenceEvent);
                }
            }

            // 再筛出所有的时移事件，并更新事件描述
            for (SIEventEntity event : events)
            {
//                if (event.isNvodTimeShiftedEvent())
//                {
//                    String refKey = NVODEvent.referenceId(event.getTransportStreamId(),
//                                                          event.getOriginalNetworkId(),
//                                                          event.getReferenceServiceId(),
//                                                          event.getReferenceEventId());
//                    SIEvent referenceEvent = eventRegistry.get(refKey);
//                    SIEvent shiftedEvent = NVODEvent.ofTimeShifted(event.getTransportStreamId(),
//                                                                   event.getOriginalNetworkId(),
//                                                                   event.getServiceId(),
//                                                                   event.getEventId(),
//                                                                   event.getReferenceServiceId(), event.getReferenceEventId(),
//                                                                   referenceEvent != null ? referenceEvent.getEventName() : String.format("事件%d", event.getReferenceEventId()),
//                                                                   referenceEvent != null ? referenceEvent.getEventDescription() : "",
//                                                                   referenceEvent != null ? referenceEvent.getLanguageCode() : "",
//                                                                   event.getStartTime(), event.getDuration(),
//                                                                   event.isPresentEvent());
//                    eventRegistry.put(shiftedEvent.getRef(), shiftedEvent);
//                }
            }

            return null;
        };

//        Consumer<Void> consumer = nothing -> serviceEventGuidePanel.update(serviceRegistry, eventRegistry);
//
//        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                         query,
//                                                         consumer);
//        task.execute();
    }

    private SIService ofReference(SIServiceEntity entity)
    {
        SIService nvodService = new SIService();
        nvodService.setOriginalNetworkId(entity.getOriginalNetworkId());
        nvodService.setTransportStreamId(entity.getTransportStreamId());
        nvodService.setServiceId(entity.getServiceId());
        nvodService.setReference(true);
        return nvodService;
    }

    private SIService ofTimeShifted(SIServiceEntity entity)
    {
        SIService nvodService = new SIService();
        nvodService.setOriginalNetworkId(entity.getOriginalNetworkId());
        nvodService.setTransportStreamId(entity.getTransportStreamId());
        nvodService.setServiceId(entity.getServiceId());
        nvodService.setReferenceServiceId(entity.getReferenceServiceId());
        nvodService.setTimeShifted(true);
        return nvodService;
    }

    private SIEvent ofReference(SIEventEntity entity)
    {
        SIEvent nvodEvent = new SIEvent();
        nvodEvent.setOriginalNetworkId(entity.getOriginalNetworkId());
        nvodEvent.setTransportStreamId(entity.getTransportStreamId());
        nvodEvent.setServiceId(entity.getServiceId());
        nvodEvent.setEventId(entity.getEventId());
        nvodEvent.setTitle(entity.getEventName());
        nvodEvent.setDescription(entity.getEventDescription());
        nvodEvent.setStartTime(OffsetDateTime.parse(entity.getStartTime()));
        nvodEvent.setDuration(Integer.parseInt(entity.getDuration()));
        nvodEvent.setReferenceEventId(entity.getReferenceEventId());
        nvodEvent.setReferenceServiceId(entity.getReferenceServiceId());
        return nvodEvent;
    }

    private SIEvent ofTimeShifted(SIEventEntity entity)
    {
        SIEvent nvodEvent = new SIEvent();
        nvodEvent.setOriginalNetworkId(entity.getOriginalNetworkId());
        nvodEvent.setTransportStreamId(entity.getTransportStreamId());
        nvodEvent.setServiceId(entity.getServiceId());
        nvodEvent.setEventId(entity.getEventId());
        nvodEvent.setTitle(entity.getEventName());
        nvodEvent.setDescription(entity.getEventDescription());
        nvodEvent.setStartTime(OffsetDateTime.parse(entity.getStartTime()));
        nvodEvent.setDuration(Integer.parseInt(entity.getDuration()));
        nvodEvent.setReferenceEventId(entity.getReferenceEventId());
        nvodEvent.setReferenceServiceId(entity.getReferenceServiceId());
        nvodEvent.setTimeShiftedEvent(true);
        return nvodEvent;
    }
}
