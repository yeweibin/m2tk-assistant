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

package m2tk.assistant.ui.component;

import m2tk.assistant.dbi.entity.StreamEntity;
import m2tk.assistant.ui.model.StreamInfoTableModel;
import m2tk.assistant.ui.util.ComponentUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class StreamInfoPanel extends JPanel
{
    private StreamInfoTableModel model;

    public StreamInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        model = new StreamInfoTableModel();
        JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false);
        ComponentUtil.configTableColumn(columnModel, 1, 70, false);
        ComponentUtil.configTableColumn(columnModel, 2, 70, false);
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 120, true);
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 100, true);
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 100, false);
        ComponentUtil.configTableColumn(columnModel, 6, leadingRenderer, 400, true);
        ComponentUtil.configTableColumn(columnModel, 7, trailingRenderer, 120, false);
        ComponentUtil.configTableColumn(columnModel, 8, trailingRenderer, 120, false);

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        TitledBorder border = BorderFactory.createTitledBorder("传输流信息");
        border.setTitleJustification(TitledBorder.LEFT);
        setBorder(border);
    }

    public void resetStreamList()
    {
        model.update(Collections.emptyList());
    }

    public void updateStreamList(List<StreamEntity> streams)
    {
        model.update(streams);
    }
}
