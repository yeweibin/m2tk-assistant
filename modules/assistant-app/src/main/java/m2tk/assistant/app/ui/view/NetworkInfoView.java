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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.SIMultiplex;
import m2tk.assistant.api.event.InfoViewRefreshingEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.event.SourceStateEvent;
import m2tk.assistant.app.ui.component.MultiplexInfoPanel;
import m2tk.assistant.app.ui.component.NetworkTimePanel;
import m2tk.assistant.app.ui.component.ServiceInfoPanel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

@Extension(ordinal = 2)
public class NetworkInfoView extends JPanel implements InfoView
{
    private NetworkTimePanel networkTimePanel;
    private MultiplexInfoPanel multiplexInfoPanel;
    private ServiceInfoPanel serviceInfoPanel;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private volatile long transactionId;
    private EventBus bus;
    private M2TKDatabase database;

    public NetworkInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(2000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer1.stop();
            else
                queryNetworks();
        });
        timer2 = new Timer(2000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer2.stop();
            else
                queryServices();
        });
        timer3 = new Timer(1000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer3.stop();
            else
                queryNetworkTime();
        });

        networkTimePanel = new NetworkTimePanel();
        multiplexInfoPanel = new MultiplexInfoPanel();
        serviceInfoPanel = new ServiceInfoPanel();

        ComponentUtil.setTitledBorder(networkTimePanel, "网络时间");
        ComponentUtil.setTitledBorder(multiplexInfoPanel, "传输流信息");
        ComponentUtil.setTitledBorder(serviceInfoPanel, "业务信息");

        setLayout(new MigLayout("fill", "[50%][50%]", "[][][grow]"));
        add(networkTimePanel, "span 2, grow, wrap");
        add(multiplexInfoPanel, "span 1 2, grow");
        add(serviceInfoPanel, "span 1 2, grow, wrap");

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
        JMenuItem item = new JMenuItem("网络信息");
        item.setIcon(getViewIcon());
        item.setAccelerator(KeyStroke.getKeyStroke("alt 2"));
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
        return "网络信息";
    }

    @Override
    public Icon getViewIcon()
    {
        FlatSVGIcon icon = new FlatSVGIcon("images/organization.svg", 18, 18);
        FlatSVGIcon.ColorFilter colorFilter = new FlatSVGIcon.ColorFilter();
        colorFilter.add(Color.black, UIManager.getColor("Label.foreground"));
        icon.setColorFilter(colorFilter);
        return icon;
    }

    @Subscribe
    public void onSourceStateEvent(SourceStateEvent event)
    {
        switch (event.state())
        {
            case SourceStateEvent.ATTACHED ->
            {
                transactionId = 1;
                timer1.start();
                timer2.start();
                timer3.start();
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
            {
                timer1.start();
                timer2.start();
                timer3.start();
            }
        } else
        {
            timer1.stop();
            timer2.stop();
            timer3.stop();
        }
    }

    public void refresh()
    {
        queryNetworks();
        queryServices();
        queryNetworkTime();
    }

    private void queryNetworks()
    {
        List<SIMultiplex> tsActualNW = new ArrayList<>();
        List<SIMultiplex> tsOtherNW = new ArrayList<>();

//        Supplier<Void> query = () -> {
//            DatabaseService databaseService = Global.getDatabaseService();
//            List<SINetworkEntity> networks = databaseService.listNetworks(currentTransaction);
//            Map<Integer, List<SIMultiplexEntity>> muxGroups =
//                    databaseService.listMultiplexes(currentTransaction)
//                                   .stream()
//                                   .collect(groupingBy(SIMultiplexEntity::getNetworkId));
//            Map<String, Integer> srvCnts =
//                    databaseService.listMultiplexServiceCounts(currentTransaction)
//                                   .stream()
//                                   .collect(toMap(srvCnt -> String.format("%d.%d",
//                                                                          srvCnt.getTransportStreamId(),
//                                                                          srvCnt.getOriginalNetworkId()),
//                                                  MultiplexServiceCountViewEntity::getServiceCount));
//
//            for (SINetworkEntity network : networks)
//            {
//                List<SIMultiplexEntity> group = muxGroups.get(network.getNetworkId());
//                if (group == null)
//                    continue;
//                for (SIMultiplexEntity multiplex : group)
//                {
//                    String key = String.format("%d.%d",
//                                               multiplex.getTransportStreamId(),
//                                               multiplex.getOriginalNetworkId());
//                    SIMultiplex mux = new SIMultiplex();
////                    multiplex.getTransportStreamId(),
////                                                      multiplex.getOriginalNetworkId(),
////                                                      network.getNetworkName(),
////                                                      multiplex.getDeliverySystemType(),
////                                                      multiplex.getTransmitFrequency(),
////                                                      srvCnts.getOrDefault(key, 0));
//                    if (network.isActualNetwork())
//                        tsActualNW.add(mux);
//                    else
//                        tsOtherNW.add(mux);
//                }
//            }
//            return null;
//        };
//        Consumer<Void> consumer = nothing ->
//        {
//            multiplexInfoPanel.updateActualNetworkMultiplexes(tsActualNW);
//            multiplexInfoPanel.updateOtherNetworkMultiplexes(tsOtherNW);
//        };
//
//        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                         query,
//                                                         consumer);
//        task.execute();
    }

    private void queryServices()
    {
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1)
//            return;
//
//        List<SIService> srvActualTS = new ArrayList<>();
//        List<SIService> srvOtherTS = new ArrayList<>();
//
//        Comparator<SIService> comparator = (s1, s2) -> {
//            if (s1.getOriginalNetworkId() != s2.getOriginalNetworkId())
//                return Integer.compare(s1.getOriginalNetworkId(), s2.getOriginalNetworkId());
//            if (s1.getTransportStreamId() != s2.getTransportStreamId())
//                return Integer.compare(s1.getTransportStreamId(), s2.getTransportStreamId());
//            return Integer.compare(s1.getServiceId(), s2.getServiceId());
//        };
//
//        Supplier<Void> query = () -> {
//            List<SIServiceEntity> services = Global.getDatabaseService().listServices(currentTransaction);
//
//            for (SIServiceEntity service : services)
//            {
//                SIService srv = new SIService();
////                service.getTransportStreamId(),
////                                              service.getOriginalNetworkId(),
////                                              service.getServiceId(),
////                                              service.getServiceTypeName(),
////                                              service.getServiceName(),
////                                              service.getServiceProvider());
//                if (service.isActualTransportStream())
//                    srvActualTS.add(srv);
//                else
//                    srvOtherTS.add(srv);
//            }
//
//            srvActualTS.sort(comparator);
//            srvOtherTS.sort(comparator);
//            return null;
//        };
//
//        Consumer<Void> consumer = nothing ->
//        {
//            serviceInfoPanel.updateActualTransportStreamServices(srvActualTS);
//            serviceInfoPanel.updateOtherTransportStreamsServices(srvOtherTS);
//        };
//
//        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                         query,
//                                                         consumer);
//        task.execute();
    }

    private void queryNetworkTime()
    {
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1)
//            return;
//
//        Supplier<SIDateTimeEntity> query = () -> Global.getDatabaseService().getLatestDateTime(currentTransaction);
//        Consumer<SIDateTimeEntity> consumer = networkTimePanel::updateTime;
//
//        AsyncQueryTask<SIDateTimeEntity> task = new AsyncQueryTask<>(frameView.getApplication(),
//                                                                     query,
//                                                                     consumer);
//        task.execute();
    }

    public void reset()
    {
        networkTimePanel.resetTime();
        if (transactionId != -1)
        {
            timer1.restart();
            timer2.restart();
            timer3.restart();
        }
    }
}
