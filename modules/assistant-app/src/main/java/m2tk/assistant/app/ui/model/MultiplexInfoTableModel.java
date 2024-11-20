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

package m2tk.assistant.app.ui.model;

import cn.hutool.core.collection.CollUtil;
import m2tk.assistant.api.domain.SIMultiplex;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MultiplexInfoTableModel extends AbstractTableModel
{
    private final List<SIMultiplex> data;
    private static final String[] COLUMNS = {
            "序号", "网络名称", "原始网络号", "传输流号", "传输系统", "传输频点", "业务数量"
    };
    private static final Class<?>[] COLUMN_CLASSES = {
            Integer.class, String.class, Integer.class, Integer.class, String.class, String.class, Integer.class
    };

    public MultiplexInfoTableModel()
    {
        data = new ArrayList<>();
    }

    public void update(List<SIMultiplex> multiplexes)
    {
        if (isSame(data, multiplexes))
            return;

        data.clear();
        data.addAll(multiplexes);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount()
    {
        return data.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return COLUMNS[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return COLUMN_CLASSES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        SIMultiplex multiplex = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> rowIndex + 1;
            case 1 -> multiplex.getNetworkName();
            case 2 -> multiplex.getOriginalNetworkId();
            case 3 -> multiplex.getTransportStreamId();
            case 4 -> multiplex.getDeliverySystemType();
            case 5 -> multiplex.getTransmitFrequency();
            case 6 -> CollUtil.size(multiplex.getServices());
            default -> null;
        };
    }

    private boolean isSame(List<SIMultiplex> current, List<SIMultiplex> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparingInt(SIMultiplex::getTransportStreamId));

        int n = current.size();
        for (int i = 0; i < n; i ++)
        {
            SIMultiplex m1 = current.get(i);
            SIMultiplex m2 = incoming.get(i);

            if (!Objects.equals(m1, m2))
                return false;
        }

        return true;
    }
}
