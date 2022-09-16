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
import m2tk.assistant.analyzer.domain.SIMultiplex;
import m2tk.assistant.analyzer.domain.SIService;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SIDateTimeEntity;
import m2tk.assistant.dbi.entity.SIMultiplexEntity;
import m2tk.assistant.dbi.entity.SINetworkEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.component.MultiplexInfoPanel;
import m2tk.assistant.ui.component.NetworkTimePanel;
import m2tk.assistant.ui.component.ServiceInfoPanel;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class NetworkInfoView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private NetworkTimePanel networkTimePanel;
    private MultiplexInfoPanel multiplexInfoPanel;
    private ServiceInfoPanel serviceInfoPanel;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;

    public NetworkInfoView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer1 = new Timer(500, actionMap.get("queryNetworks"));
        timer2 = new Timer(500, actionMap.get("queryServices"));
        timer3 = new Timer(1000, actionMap.get("queryNetworkTime"));

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
                queryNetworks();
                queryServices();
                queryNetworkTime();
                startRefreshing();
            }

            @Override
            public void componentHidden(ComponentEvent e)
            {
                stopRefreshing();
            }
        });
    }

    @Action
    public void queryNetworks()
    {
        List<SIMultiplex> tsActualNW = new ArrayList<>();
        List<SIMultiplex> tsOtherNW = new ArrayList<>();

        Supplier<Void> query = () -> {
            DatabaseService databaseService = Global.getDatabaseService();
            List<SINetworkEntity> networks = databaseService.listNetworks();
            Map<Integer, List<SIMultiplexEntity>> muxGroups =
                    databaseService.listMultiplexes().stream().collect(groupingBy(SIMultiplexEntity::getNetworkId));
            Map<String, Integer> srvCnts =
                    databaseService.listMultiplexServiceCounts()
                                   .stream()
                                   .collect(toMap(srvCnt -> String.format("%d.%d",
                                                                          srvCnt.getTransportStreamId(),
                                                                          srvCnt.getOriginalNetworkId()),
                                                  srvCnt -> srvCnt.getServiceCount()));

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

    @Action
    public void queryServices()
    {
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
            List<SIServiceEntity> services = Global.getDatabaseService().listServices();

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

    @Action
    public void queryNetworkTime()
    {
        Supplier<SIDateTimeEntity> query = () -> Global.getDatabaseService().getLatestDateTime();
        Consumer<SIDateTimeEntity> consumer = networkTimePanel::updateTime;

        AsyncQueryTask<SIDateTimeEntity> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                     query,
                                                                     consumer);
        task.execute();
    }

    public void reset()
    {
        networkTimePanel.resetTime();
        if (Global.getStreamAnalyser().isRunning())
        {
            timer1.restart();
            timer2.restart();
            timer3.restart();
        }
    }

    public void startRefreshing()
    {
        if (Global.getStreamAnalyser().isRunning())
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
