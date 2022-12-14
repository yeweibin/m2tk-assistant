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
import m2tk.assistant.ui.util.ThreeStateRowSorterListener;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class StreamInfoPanel extends JPanel
{
    private StreamInfoTableModel model;
    private transient TableRowSorter<StreamInfoTableModel> rowSorter;
    private transient BiConsumer<MouseEvent, StreamEntity> popupListener;

    public StreamInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        model = new StreamInfoTableModel();
        rowSorter = new TableRowSorter<>(model);
        rowSorter.addRowSorterListener(new ThreeStateRowSorterListener(rowSorter));

        JTable table = new JTable();
        table.setModel(model);
        table.setRowSorter(rowSorter);
        table.getTableHeader().setReorderingAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1 && popupListener != null)
                    {
                        try
                        {
                            popupListener.accept(e, model.getRow(selectedRow));
                        } catch (Exception ignored)
                        {
                        }
                    }
                }
            }
        });

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = table.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 40, false); // ??????
        ComponentUtil.configTableColumn(columnModel, 1, 40, false); // ?????????
        ComponentUtil.configTableColumn(columnModel, 2, 40, false); // ????????????
        ComponentUtil.configTableColumn(columnModel, 3, 40, false); // PCR
        ComponentUtil.configTableColumn(columnModel, 4, trailingRenderer, 120, true); // PID
        ComponentUtil.configTableColumn(columnModel, 5, trailingRenderer, 100, true); // ??????Kbps
        ComponentUtil.configTableColumn(columnModel, 6, trailingRenderer, 100, false); // ????????????
        ComponentUtil.configTableColumn(columnModel, 7, leadingRenderer, 400, true); // ????????????
        ComponentUtil.configTableColumn(columnModel, 8, trailingRenderer, 100, false); // ?????????
        ComponentUtil.configTableColumn(columnModel, 9, trailingRenderer, 100, false); // ????????????
        ComponentUtil.configTableColumn(columnModel, 10, trailingRenderer, 100, false); // ??????????????????

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setPopupListener(BiConsumer<MouseEvent, StreamEntity> listener)
    {
        popupListener = listener;
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
