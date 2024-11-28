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
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.SIMultiplex;
import m2tk.assistant.api.domain.SIService;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.component.MultiplexInfoPanel;
import m2tk.assistant.app.ui.component.NetworkTimePanel;
import m2tk.assistant.app.ui.component.ServiceInfoPanel;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Extension(ordinal = 2)
public class NetworkInfoView extends JPanel implements InfoView
{
    private Application application;
    private NetworkTimePanel networkTimePanel;
    private MultiplexInfoPanel multiplexInfoPanel;
    private ServiceInfoPanel serviceInfoPanel;

    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    private static class NetworkInfoSnapshot
    {
        private List<SIMultiplex> tsActualNetwork;
        private List<SIMultiplex> tsOtherNetwork;
        private List<SIService> srvActualTS;
        private List<SIService> srvOtherTS;
        private OffsetDateTime latestNetworkTime;
    }

    public NetworkInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        networkTimePanel = new NetworkTimePanel();
        multiplexInfoPanel = new MultiplexInfoPanel();
        serviceInfoPanel = new ServiceInfoPanel();

        ComponentUtil.setTitledBorder(networkTimePanel, "网络时间");
        ComponentUtil.setTitledBorder(multiplexInfoPanel, "传输流信息");
        ComponentUtil.setTitledBorder(serviceInfoPanel, "业务信息");

        setLayout(new MigLayout("fill", "[50%][50%]", "[][][grow]"));
        add(networkTimePanel, "span 2, grow, wrap");
        add(multiplexInfoPanel, "span 1 2, grow");
        add(serviceInfoPanel, "span 1 2, grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryNetworkSnapshot();
            }
        });
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
        colorFilter.add(Color.black, Color.decode("#777737"));
        icon.setColorFilter(colorFilter);
        return icon;
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryNetworkSnapshot();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryNetworkSnapshot()
    {
        Supplier<NetworkInfoSnapshot> query = () ->
        {
            NetworkInfoSnapshot snapshot = new NetworkInfoSnapshot();
            snapshot.tsActualNetwork = database.getActualNetworkMultiplexes();
            snapshot.tsOtherNetwork = database.getOtherNetworkMultiplexes();
            snapshot.srvActualTS = database.getActualTransportStreamServices();
            snapshot.srvOtherTS = database.getOtherTransportStreamServices();
            snapshot.latestNetworkTime = database.getLastTimestamp();
            return snapshot;
        };

        Consumer<NetworkInfoSnapshot> consumer = snapshot ->
        {
            multiplexInfoPanel.updateMultiplexes(snapshot.tsActualNetwork, snapshot.tsOtherNetwork);
            serviceInfoPanel.updateServices(snapshot.srvActualTS, snapshot.srvOtherTS);
            networkTimePanel.updateTime(snapshot.latestNetworkTime);
        };

        AsyncQueryTask<NetworkInfoSnapshot> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }
}
