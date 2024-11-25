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
package m2tk.assistant.ext.view;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.ext.component.EBSectionDatagramPanel;
import m2tk.assistant.ext.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import javax.swing.border.TitledBorder;

@Extension(ordinal = 10)
public class EBInfoView extends JPanel implements InfoView
{
    private EBSectionDatagramPanel sectionDatagramPanel;
    private EventBus bus;
    private M2TKDatabase database;

    public EBInfoView()
    {
        initUI();
    }

    private void initUI()
    {
        sectionDatagramPanel = new EBSectionDatagramPanel();
        ComponentUtil.setTitledBorder(sectionDatagramPanel, "应急广播", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(sectionDatagramPanel, "center, grow");
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
        JMenuItem item = new JMenuItem("应急广播");
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
        return FontIcon.of(FluentUiRegularMZ.SOUND_SOURCE_24, 20, UIManager.getColor("Label.foreground"));
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        queryDatagrams();
    }

    private void queryDatagrams()
    {
//        long currentTransaction = Math.max(transactionId, Global.getLatestTransactionId());
//        if (currentTransaction == -1)
//            return;
//
//        Supplier<Map<String, List<PrivateSectionEntity>>> query = () ->
//                Global.getDatabaseService()
//                      .getSectionGroups(currentTransaction, "eb-section.")
//                      .stream()
//                      .collect(Collectors.groupingBy(section -> section.getTag().replace("eb-section.", "")));
//
//        Consumer<Map<String, List<PrivateSectionEntity>>> consumer = sectionDatagramPanel::update;
//
//        AsyncQueryTask<Map<String, List<PrivateSectionEntity>>> task =
//                new AsyncQueryTask<>(frameView.getApplication(), query, consumer);
//        task.execute();
    }
}
