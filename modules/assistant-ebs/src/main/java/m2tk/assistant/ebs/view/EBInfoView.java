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
package m2tk.assistant.ebs.view;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.PrivateSection;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.ebs.AsyncQueryTask;
import m2tk.assistant.ebs.component.EBSectionDatagramPanel;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Extension
public class EBInfoView extends JPanel implements InfoView
{
    private Application application;
    private EBSectionDatagramPanel sectionDatagramPanel;
    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    public EBInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        sectionDatagramPanel = new EBSectionDatagramPanel();
        TitledBorder border = BorderFactory.createTitledBorder("应急广播");
        border.setTitleJustification(TitledBorder.LEFT);
        sectionDatagramPanel.setBorder(border);

        setLayout(new MigLayout("fill"));
        add(sectionDatagramPanel, "center, grow");
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
        return "应急广播";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularMZ.MEGAPHONE_24, 20, Color.decode("#F25022"));
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryDatagrams();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryDatagrams()
    {
        Supplier<Map<String, List<PrivateSection>>> query = () ->
            database.getPrivateSectionGroups("EBSection.EBIndex",
                                             "EBSection.EBContent",
                                             "EBSection.EBCertAuth",
                                             "EBSection.EBConfigure");

        Consumer<Map<String, List<PrivateSection>>> consumer = sectionDatagramPanel::update;

        AsyncQueryTask<Map<String, List<PrivateSection>>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }
}