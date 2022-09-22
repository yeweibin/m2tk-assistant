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

import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ListModelOutputStream;
import m2tk.assistant.util.TextListLogAppender;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LogsView extends JPanel
{
    private DefaultListModel<String> model;

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

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        ComponentUtil.setTitledBorder(panel, "日志", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(panel, "center, grow");

        TextListLogAppender.setStaticOutputStream(new ListModelOutputStream(model));
    }

    public void clear()
    {
        model.clear();
    }
}
