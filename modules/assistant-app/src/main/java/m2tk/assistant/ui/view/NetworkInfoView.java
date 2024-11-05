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
import m2tk.assistant.analyzer.domain.SIMultiplex;
import m2tk.assistant.analyzer.domain.SIService;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.*;
import m2tk.assistant.ui.component.MultiplexInfoPanel;
import m2tk.assistant.ui.component.NetworkTimePanel;
import m2tk.assistant.ui.component.ServiceInfoPanel;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class NetworkInfoView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private NetworkTimePanel networkTimePanel;
    private MultiplexInfoPanel multiplexInfoPanel;
    private ServiceInfoPanel serviceInfoPanel;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;
    private volatile long transactionId;

    public NetworkInfoView(FrameView view)
    {
        frameView = view;
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

        ComponentUtil.setTitledBorder(networkTimePanel, "网络时间", TitledBorder.LEFT);
        ComponentUtil.setTitledBorder(multiplexInfoPanel, "传输流信息", TitledBorder.LEFT);
        ComponentUtil.setTitledBorder(serviceInfoPanel, "业务信息", TitledBorder.LEFT);

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

        Global.registerSubscriber(this);
        transactionId = -1;
    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        transactionId = event.getSource().getTransactionId();
        timer1.start();
        timer2.start();
        timer3.start();
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
        queryNetworks();
        queryServices();
        queryNetworkTime();
    }

    private void queryNetworks()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        List<SIMultiplex> tsActualNW = new ArrayList<>();
        List<SIMultiplex> tsOtherNW = new ArrayList<>();

        Supplier<Void> query = () -> {
            DatabaseService databaseService = Global.getDatabaseService();
            List<SINetworkEntity> networks = databaseService.listNetworks(currentTransaction);
            Map<Integer, List<SIMultiplexEntity>> muxGroups =
                    databaseService.listMultiplexes(currentTransaction)
                                   .stream()
                                   .collect(groupingBy(SIMultiplexEntity::getNetworkId));
            Map<String, Integer> srvCnts =
                    databaseService.listMultiplexServiceCounts(currentTransaction)
                                   .stream()
                                   .collect(toMap(srvCnt -> String.format("%d.%d",
                                                                          srvCnt.getTransportStreamId(),
                                                                          srvCnt.getOriginalNetworkId()),
                                                  SIMultiplexServiceCountView::getServiceCount));

            for (SINetworkEntity network : networks)
            {
                List<SIMultiplexEntity> group = muxGroups.get(network.getNetworkId());
                if (group == null)
                    continue;
                for (SIMultiplexEntity multiplex : group)
                {
                    String key = String.format("%d.%d",
                                               multiplex.getTransportStreamId(),
                                               multiplex.getOriginalNetworkId());
                    SIMultiplex mux = new SIMultiplex(multiplex.getTransportStreamId(),
                                                      multiplex.getOriginalNetworkId(),
                                                      network.getNetworkName(),
                                                      multiplex.getDeliverySystemType(),
                                                      multiplex.getTransmitFrequency(),
                                                      srvCnts.getOrDefault(key, 0));
                    if (network.isActualNetwork())
                        tsActualNW.add(mux);
                    else
                        tsOtherNW.add(mux);
                }
            }
            return null;
        };
        Consumer<Void> consumer = nothing ->
        {
            multiplexInfoPanel.updateActualNetworkMultiplexes(tsActualNW);
            multiplexInfoPanel.updateOtherNetworkMultiplexes(tsOtherNW);
        };

        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                         query,
                                                         consumer);
        task.execute();
    }

    private void queryServices()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        List<SIService> srvActualTS = new ArrayList<>();
        List<SIService> srvOtherTS = new ArrayList<>();

        Comparator<SIService> comparator = (s1, s2) -> {
            if (s1.getOriginalNetworkId() != s2.getOriginalNetworkId())
                return Integer.compare(s1.getOriginalNetworkId(), s2.getOriginalNetworkId());
            if (s1.getTransportStreamId() != s2.getTransportStreamId())
                return Integer.compare(s1.getTransportStreamId(), s2.getTransportStreamId());
            return Integer.compare(s1.getServiceId(), s2.getServiceId());
        };

        Supplier<Void> query = () -> {
            List<SIServiceEntity> services = Global.getDatabaseService().listServices(currentTransaction);

            for (SIServiceEntity service : services)
            {
                SIService srv = new SIService(service.getTransportStreamId(),
                                              service.getOriginalNetworkId(),
                                              service.getServiceId(),
                                              service.getServiceTypeName(),
                                              service.getServiceName(),
                                              service.getServiceProvider());
                if (service.isActualTransportStream())
                    srvActualTS.add(srv);
                else
                    srvOtherTS.add(srv);
            }

            srvActualTS.sort(comparator);
            srvOtherTS.sort(comparator);
            return null;
        };

        Consumer<Void> consumer = nothing ->
        {
            serviceInfoPanel.updateActualTransportStreamServices(srvActualTS);
            serviceInfoPanel.updateOtherTransportStreamsServices(srvOtherTS);
        };

        AsyncQueryTask<Void> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                         query,
                                                         consumer);
        task.execute();
    }

    private void queryNetworkTime()
    {
        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
        if (currentTransaction == -1)
            return;

        Supplier<SIDateTimeEntity> query = () -> Global.getDatabaseService().getLatestDateTime(currentTransaction);
        Consumer<SIDateTimeEntity> consumer = networkTimePanel::updateTime;

        AsyncQueryTask<SIDateTimeEntity> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                     query,
                                                                     consumer);
        task.execute();
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

    public void startRefreshing()
    {
        if (transactionId != -1)
        {
            timer1.start();
            timer2.start();
            timer3.start();
        }
    }

    public void stopRefreshing()
    {
        timer1.stop();
        timer2.stop();
        timer3.stop();
    }
}
