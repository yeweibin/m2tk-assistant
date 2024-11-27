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
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.NVODServiceEventGuidePanel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.pf4j.Extension;

import javax.swing.*;

//@Extension(ordinal = 6)
public class NVODInfoView extends JPanel implements InfoView
{
    private NVODServiceEventGuidePanel serviceEventGuidePanel;
    private EventBus bus;
    private M2TKDatabase database;

    public NVODInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        serviceEventGuidePanel = new NVODServiceEventGuidePanel();
        ComponentUtil.setTitledBorder(serviceEventGuidePanel, "NVOD");

        setLayout(new MigLayout("fill"));
        add(serviceEventGuidePanel, "center, grow");
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
        JMenuItem item = new JMenuItem(getViewTitle());
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
        return "NVOD";
    }

    @Override
    public Icon getViewIcon()
    {
        return null;
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        queryServiceAndEvents();
    }

    private void queryServiceAndEvents()
    {
//        long currentTransactionId = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransactionId == -1)
//            return;
//
//        Map<Long, SIService> serviceRegistry = new HashMap<>();
//        Map<Long, SIEvent> eventRegistry = new HashMap<>();
//
//        Supplier<Void> query = () -> {
//            DatabaseService databaseService = Global.getDatabaseService();
//            List<SIServiceEntity> services = databaseService.listNVODServices(currentTransactionId);
//            List<SIEventEntity> events = databaseService.listNVODEvents(currentTransactionId);
//
//            services.forEach(service -> {
//                SIService nvodService = service.isNvodReferenceService() ? ofReference(service)
//                                                                         : ofTimeShifted(service);
//                serviceRegistry.put(nvodService.getRef(), nvodService);
//            });
//
//            // 先筛查所有的引用事件
//            for (SIEventEntity event : events)
//            {
//                if (event.isNvodReferenceEvent())
//                {
//                    SIEvent referenceEvent = ofReference(event);
//                    eventRegistry.put(referenceEvent.getRef(), referenceEvent);
//                }
//            }
//
//            // 再筛出所有的时移事件，并更新事件描述
//            for (SIEventEntity event : events)
//            {
////                if (event.isNvodTimeShiftedEvent())
////                {
////                    String refKey = NVODEvent.referenceId(event.getTransportStreamId(),
////                                                          event.getOriginalNetworkId(),
////                                                          event.getReferenceServiceId(),
////                                                          event.getReferenceEventId());
////                    SIEvent referenceEvent = eventRegistry.get(refKey);
////                    SIEvent shiftedEvent = NVODEvent.ofTimeShifted(event.getTransportStreamId(),
////                                                                   event.getOriginalNetworkId(),
////                                                                   event.getServiceId(),
////                                                                   event.getEventId(),
////                                                                   event.getReferenceServiceId(), event.getReferenceEventId(),
////                                                                   referenceEvent != null ? referenceEvent.getEventName() : String.format("事件%d", event.getReferenceEventId()),
////                                                                   referenceEvent != null ? referenceEvent.getEventDescription() : "",
////                                                                   referenceEvent != null ? referenceEvent.getLanguageCode() : "",
////                                                                   event.getStartTime(), event.getDuration(),
////                                                                   event.isPresentEvent());
////                    eventRegistry.put(shiftedEvent.getRef(), shiftedEvent);
////                }
//            }
//
//            return null;
//        };
//
////        Consumer<Void> consumer = nothing -> serviceEventGuidePanel.update(serviceRegistry, eventRegistry);
////
////        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
////                                                         query,
////                                                         consumer);
////        task.execute();
    }
}
