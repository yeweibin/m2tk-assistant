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

import m2tk.assistant.api.domain.TR290Stats;
import m2tk.assistant.app.ui.model.TR290StatsTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public class TR290StatsPanel extends JPanel
{
    private TR290StatsTableModel tableModel;

    private transient BiConsumer<MouseEvent, String> popupListener;

    public TR290StatsPanel()
    {
        initUI();
    }

    private void initUI()
    {
        tableModel = new TR290StatsTableModel();
        JTable table = new JTable();
        table.setModel(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger() && popupListener != null)
                {
                    int rowAtPoint = table.rowAtPoint(e.getPoint());
                    if (rowAtPoint != -1)
                    {
                        table.setRowSelectionInterval(table.convertRowIndexToModel(rowAtPoint),
                                                      table.convertRowIndexToModel(rowAtPoint));
                    }
                    try
                    {
                        popupListener.accept(e, tableModel.getTR290EventType(table.convertRowIndexToModel(rowAtPoint)));
                    } catch (Exception ignored)
                    {
                    }
                }
            }
        });

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, 40, false);                    // 状态
        ComponentUtil.configTableColumn(columnModel, 1, leadingRenderer, 220, false);  // 错误名称
        ComponentUtil.configTableColumn(columnModel, 2, centeredRenderer, 100, false); // 错误数
        ComponentUtil.configTableColumn(columnModel, 3, centeredRenderer, 220, false); // 最近发生时间
        ComponentUtil.configTableColumn(columnModel, 4, leadingRenderer, 500, true);   // 错误描述

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setPopupListener(BiConsumer<MouseEvent, String> listener)
    {
        popupListener = listener;
    }

    public void update(TR290Stats stats)
    {
        tableModel.update(stats);
    }

    public void reset()
    {
        tableModel.update(new TR290Stats());
    }
}
