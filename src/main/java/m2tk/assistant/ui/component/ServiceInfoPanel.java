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

import m2tk.assistant.analyzer.domain.SIService;
import m2tk.assistant.ui.model.ServiceInfoTableModel;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ThreeStateRowSorterListener;

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
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false); // ??????
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 180, true);  // ????????????
        ComponentUtil.configTableColumn(columnModel, 2, leadingRenderer, 120, true);  // ?????????
        ComponentUtil.configTableColumn(columnModel, 3, leadingRenderer, 140, true);  // ????????????
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false); // ?????????
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false); // ???????????????
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false); // ????????????

        columnModel = table2.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false); // ??????
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 180, true);  // ????????????
        ComponentUtil.configTableColumn(columnModel, 2, leadingRenderer, 120, true);  // ?????????
        ComponentUtil.configTableColumn(columnModel, 3, leadingRenderer, 140, true);  // ????????????
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 80, false); // ?????????
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 80, false); // ???????????????
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 80, false); // ????????????

        setLayout(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("???????????????", new JScrollPane(table1));
        tabbedPane.add("???????????????", new JScrollPane(table2));
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