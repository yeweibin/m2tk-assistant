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

import m2tk.assistant.analyzer.domain.TR290Stats;
import m2tk.assistant.dbi.entity.PCRStatEntity;
import m2tk.assistant.ui.model.PCRStatsTableModel;
import m2tk.assistant.ui.util.ComponentUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PCRStatsPanel extends JPanel
{
    private PCRStatsTableModel tableModel;

    public PCRStatsPanel()
    {
        initUI();
    }

    private void initUI()
    {
        tableModel = new PCRStatsTableModel();
        JTable table = new JTable();
        table.setModel(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, 40, false);  // 状态
        ComponentUtil.configTableColumn(columnModel, 1, centeredRenderer, 80, false); // PID
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 80, false); // PCR总数
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 100, false); // 平均间隔
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 100, false); // 最小间隔
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 100, false); // 最大间隔
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 100, false); // 间隔越界
        ComponentUtil.configTableColumn(columnModel, 7, trailingRenderer, 180, false); // 平均精度
        ComponentUtil.configTableColumn(columnModel, 8, trailingRenderer, 180, false); // 最小精度
        ComponentUtil.configTableColumn(columnModel, 9, trailingRenderer, 180, false); // 最大精度
        ComponentUtil.configTableColumn(columnModel, 10, trailingRenderer, 100, false); // 精度越界

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void update(List<PCRStatEntity> stats)
    {
        tableModel.update(Objects.requireNonNull(stats));
    }

    public void reset()
    {
        tableModel.update(Collections.emptyList());
    }
}
