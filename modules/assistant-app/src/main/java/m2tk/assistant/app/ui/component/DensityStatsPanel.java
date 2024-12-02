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

import m2tk.assistant.api.domain.StreamDensityStats;
import m2tk.assistant.app.ui.model.DensityStatsTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class DensityStatsPanel extends JPanel
{
    private DensityStatsTableModel tableModel;
    private JTable table;
    private Consumer<StreamDensityStats> consumer;

    public DensityStatsPanel()
    {
        initUI();
    }

    private void initUI()
    {
        tableModel = new DensityStatsTableModel();
        table = new JTable();
        table.setModel(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;

            int row = table.getSelectedRow();
            if (row != -1 && consumer != null)
                consumer.accept(tableModel.getStatAtRow(row));
        });

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 60, false);   // 序号
        ComponentUtil.configTableColumn(columnModel, 1, trailingRenderer, 150, false);  // PID
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 200, false);  // PCR总数
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 200, false);  // 平均间隔
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 200, false);  // 最小间隔
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 200, false);  // 最大间隔

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void update(List<StreamDensityStats> stats)
    {
        tableModel.update(stats);
    }

    public void addDensityStatConsumer(Consumer<StreamDensityStats> consumer)
    {
        this.consumer = consumer;
    }

    public void selectStreamStats(int pid)
    {
        int row = tableModel.getStatRow(pid);
        if (row != -1)
            table.getSelectionModel().setSelectionInterval(row, row);
    }
}
