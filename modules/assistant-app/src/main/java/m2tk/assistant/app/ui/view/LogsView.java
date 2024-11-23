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
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.ui.event.ClearLogsEvent;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.ListModelOutputStream;
import m2tk.assistant.app.util.TextListLogAppender;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;

@Extension
public class LogsView extends JPanel implements InfoView
{
    private DefaultListModel<String> model;
    private EventBus bus;

    public LogsView()
    {
        initUI();
    }

    private void initUI()
    {
        model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setDragEnabled(false);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.putClientProperty("FlatLaf.style",
                                     """
                                     borderWidth: 0;
                                     focusWidth: 0; innerFocusWidth: 0.5; innerOutlineWidth: 0.5;
                                     """);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        ComponentUtil.setTitledBorder(panel, "日志");

        setLayout(new MigLayout("fill"));
        add(panel, "center, grow");

        TextListLogAppender.setStaticOutputStream(new ListModelOutputStream(model));
    }

    @Override
    public void setupApplication(Application application)
    {
    }

    @Override
    public void setupDataSource(EventBus bus, M2TKDatabase database)
    {
        this.bus = bus;
        bus.register(this);
    }

    @Override
    public void setupMenu(JMenu menu)
    {
        JMenuItem item = new JMenuItem("查看日志");
        item.setIcon(getViewIcon());
        item.setAccelerator(KeyStroke.getKeyStroke("ctrl L"));
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
        return "日志";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularMZ.TEXTBOX_20, 20, UIManager.getColor("Label.foreground"));
    }

    @Subscribe
    public void onClearLogsEvent(ClearLogsEvent event)
    {
        model.clear();
    }
}
