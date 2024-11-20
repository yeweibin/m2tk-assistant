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

import m2tk.assistant.api.domain.SIService;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.ThreeStateRowSorterListener;
import m2tk.assistant.app.ui.model.ServiceInfoTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class ServiceInfoPanel extends JPanel
{
    private ServiceInfoTableModel modelActualTS;
    private ServiceInfoTableModel modelOtherTS;
    private TableRowSorter<ServiceInfoTableModel> rowSorterActualTS;
    private TableRowSorter<ServiceInfoTableModel> rowSorterOtherTS;

    public ServiceInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        modelActualTS = new ServiceInfoTableModel();
        modelOtherTS = new ServiceInfoTableModel();

        rowSorterActualTS = new TableRowSorter<>(modelActualTS);
        rowSorterActualTS.addRowSorterListener(new ThreeStateRowSorterListener(rowSorterActualTS));
        rowSorterOtherTS = new TableRowSorter<>(modelOtherTS);
        rowSorterOtherTS.addRowSorterListener(new ThreeStateRowSorterListener(rowSorterOtherTS));

        JTable table1 = new JTable();
        table1.setModel(modelActualTS);
        table1.setRowSorter(rowSorterActualTS);
        table1.getTableHeader().setReorderingAllowed(true);
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTable table2 = new JTable();
        table2.setModel(modelOtherTS);
        table2.setRowSorter(rowSorterOtherTS);
        table2.getTableHeader().setReorderingAllowed(true);
        table2.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table1.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false); // 序号
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 180, true);  // 业务名称
        ComponentUtil.configTableColumn(columnModel, 2, leadingRenderer, 120, true);  // 提供商
        ComponentUtil.configTableColumn(columnModel, 3, leadingRenderer, 140, true);  // 业务类型
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false); // 业务号
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false); // 原始网络号
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false); // 传输流号

        columnModel = table2.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false); // 序号
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 180, true);  // 业务名称
        ComponentUtil.configTableColumn(columnModel, 2, leadingRenderer, 120, true);  // 提供商
        ComponentUtil.configTableColumn(columnModel, 3, leadingRenderer, 140, true);  // 业务类型
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false); // 业务号
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false); // 原始网络号
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false); // 传输流号

        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("当前传输流", new JScrollPane(table1));
        tabbedPane.add("其他传输流", new JScrollPane(table2));
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateActualTransportStreamServices(List<SIService> services)
    {
        modelActualTS.update(services);
    }

    public void updateOtherTransportStreamsServices(List<SIService> services)
    {
        modelOtherTS.update(services);
    }
}