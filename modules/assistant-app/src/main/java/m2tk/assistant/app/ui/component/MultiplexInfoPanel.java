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
package m2tk.assistant.app.ui.component;

import m2tk.assistant.api.domain.SIMultiplex;
import m2tk.assistant.app.ui.model.MultiplexInfoTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.ThreeStateRowSorterListener;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class MultiplexInfoPanel extends JPanel
{
    private MultiplexInfoTableModel modelActualNW;
    private MultiplexInfoTableModel modelOtherNW;
    private TableRowSorter<MultiplexInfoTableModel> rowSorterActualNW;
    private TableRowSorter<MultiplexInfoTableModel> rowSorterOtherNW;

    public MultiplexInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        modelActualNW = new MultiplexInfoTableModel();
        modelOtherNW = new MultiplexInfoTableModel();

        rowSorterActualNW = new TableRowSorter<>(modelActualNW);
        rowSorterActualNW.addRowSorterListener(new ThreeStateRowSorterListener(rowSorterActualNW));
        rowSorterOtherNW = new TableRowSorter<>(modelOtherNW);
        rowSorterOtherNW.addRowSorterListener(new ThreeStateRowSorterListener(rowSorterOtherNW));

        JTable table1 = new JTable();
        table1.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table1.setModel(modelActualNW);
        table1.setRowSorter(rowSorterActualNW);
        table1.getTableHeader().setReorderingAllowed(false);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTable table2 = new JTable();
        table2.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table2.setModel(modelOtherNW);
        table2.setRowSorter(rowSorterOtherNW);
        table2.getTableHeader().setReorderingAllowed(false);
        table2.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table1.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 80, false);  // 序号
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 360, true);   // 网络名称
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 120, true);  // 传输系统
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 120, true);  // 传输频点
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false);  // 原始网络号
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false);  // 传输流号
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false);  // 业务数量

        columnModel = table2.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 80, false);  // 序号
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 360, true);   // 网络名称
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 120, true);  // 传输系统
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 120, true);  // 传输频点
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false);  // 原始网络号
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false);  // 传输流号
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false);  // 业务数量

        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("当前网络", new JScrollPane(table1));
        tabbedPane.add("其他网络", new JScrollPane(table2));
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateMultiplexes(List<SIMultiplex> tsActualNW, List<SIMultiplex> tsOtherNW)
    {
        modelActualNW.update(tsActualNW);
        modelOtherNW.update(tsOtherNW);
    }
}
