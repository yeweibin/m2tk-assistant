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

import com.google.common.eventbus.Subscribe;
import m2tk.assistant.Global;
import m2tk.assistant.analyzer.domain.NVODEvent;
import m2tk.assistant.analyzer.domain.NVODService;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SIEventEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.component.NVODServiceEventGuidePanel;
import m2tk.assistant.ui.event.SourceChangedEvent;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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

            if (!Global.getStreamAnalyser().isRunning())
                timer.stop();

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

    @Override
    public void refresh()
    {
        queryServiceAndEvents();
    }

    @Subscribe
    public void onSourceChanged(SourceChangedEvent event)
    {
        transactionId = event.getTransactionId();
    }

    public void reset()
    {
        serviceEventGuidePanel.reset();
        if (Global.getStreamAnalyser().isRunning())
            timer.restart();
    }

    public void startRefreshing()
    {
        if (Global.getStreamAnalyser().isRunning())
            timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    private void queryServiceAndEvents()
    {
        Map<String, NVODService> serviceRegistry = new HashMap<>();
        Map<String, NVODEvent> eventRegistry = new HashMap<>();

        long currentTransactionId = (transactionId == -1) ? Global.getCurrentTransactionId() : transactionId;

        Supplier<Void> query = () -> {
            DatabaseService databaseService = Global.getDatabaseService();
            List<SIServiceEntity> services = databaseService.listNVODServices(currentTransactionId);
            List<SIEventEntity> events = databaseService.listNVODEvents(currentTransactionId);

            services.forEach(service -> {
                NVODService nvodService = service.isNvodReferenceService()
                                          ? NVODService.ofReference(service.getTransportStreamId(),
                                                                    service.getOriginalNetworkId(),
                                                                    service.getServiceId())
                                          : NVODService.ofTimeShifted(service.getTransportStreamId(),
                                                                      service.getOriginalNetworkId(),
                                                                      service.getServiceId(),
                                                                      service.getReferenceServiceId());
                serviceRegistry.put(nvodService.getId(), nvodService);
            });

            // 先筛查所有的引用事件
            for (SIEventEntity event : events)
            {
                if (event.isNvodReferenceEvent())
                {
                    NVODEvent referenceEvent = NVODEvent.ofReference(event.getTransportStreamId(),
                                                                     event.getOriginalNetworkId(),
                                                                     event.getServiceId(),
                                                                     event.getEventId(),
                                                                     event.getEventName(),
                                                                     event.getEventDescription(),
                                                                     event.getLanguageCode(),
                                                                     event.getStartTime(), event.getDuration());
                    eventRegistry.put(referenceEvent.getId(), referenceEvent);
                }
            }

            // 再筛出所有的时移事件，并更新事件描述
            for (SIEventEntity event : events)
            {
                if (event.isNvodTimeShiftedEvent())
                {
                    String refKey = NVODEvent.referenceId(event.getTransportStreamId(),
                                                          event.getOriginalNetworkId(),
                                                          event.getReferenceServiceId(),
                                                          event.getReferenceEventId());
                    NVODEvent referenceEvent = eventRegistry.get(refKey);
                    NVODEvent shiftedEvent = NVODEvent.ofTimeShifted(event.getTransportStreamId(),
                                                                     event.getOriginalNetworkId(),
                                                                     event.getServiceId(),
                                                                     event.getEventId(),
                                                                     event.getReferenceServiceId(), event.getReferenceEventId(),
                                                                     referenceEvent != null ? referenceEvent.getEventName() : String.format("事件%d", event.getReferenceEventId()),
                                                                     referenceEvent != null ? referenceEvent.getEventDescription() : "",
                                                                     referenceEvent != null ? referenceEvent.getLanguageCode() : "",
                                                                     event.getStartTime(), event.getDuration(),
                                                                     event.isPresentEvent());
                    eventRegistry.put(shiftedEvent.getId(), shiftedEvent);
                }
            }

            return null;
        };

        Consumer<Void> consumer = nothing -> serviceEventGuidePanel.update(serviceRegistry, eventRegistry);

        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                         query,
                                                         consumer);
        task.execute();
    }
}
