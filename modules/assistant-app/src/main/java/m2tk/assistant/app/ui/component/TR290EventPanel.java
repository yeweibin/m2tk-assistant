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
package m2tk.assistant.app.ui.component;

import m2tk.assistant.api.domain.TR290Event;
import m2tk.assistant.app.ui.model.TR290EventTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

public class TR290EventPanel extends JPanel
{
    private TR290EventTableModel tableModel;

    public TR290EventPanel()
    {
        initUI();
    }

    private void initUI()
    {
        tableModel = new TR290EventTableModel();
        JTable table = new JTable();
        table.setModel(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 80, false);  // 序号
        ComponentUtil.configTableColumn(columnModel, 1, centeredRenderer, 220, false);  // 发生时间
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 150, false); // PID
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 120, false); // 位置
        ComponentUtil.configTableColumn(columnModel, 4, leadingRenderer, 500, true);   // 错误描述

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void update(List<TR290Event> events)
    {
        tableModel.update(events);
    }
}
