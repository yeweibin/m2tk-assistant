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

import m2tk.assistant.api.domain.StreamDensityStats;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;

public class DensityStatsTableModel extends AbstractTableModel
{
    private List<StreamDensityStats> data = Collections.emptyList();

    private static final String[] COLUMNS = {
       "", "PID", "间隔采样", "平均间隔（四舍五入）", "最小间隔", "最大间隔", ""
    };

    public void update(List<StreamDensityStats> stats)
    {
        if (isSame(data, stats))
            return;

        data = stats;
        fireTableDataChanged();
    }

    public int getStatRow(int pid)
    {
        int size = data.size();
        for (int i = 0; i < size; i++)
        {
            StreamDensityStats stat = data.get(i);
            if (stat.getPid() == pid)
                return i;
        }
        return -1;
    }

    public StreamDensityStats getStatAtRow(int row)
    {
        try
        {
            return (row == -1) ? null : data.get(row);
        } catch (Exception ex)
        {
            return null;
        }
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
    public String getColumnName(int column)
    {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return (columnIndex == 0) ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        StreamDensityStats stats = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> rowIndex + 1;
            case 1 -> String.format("%d (0x%04X)", stats.getPid(), stats.getPid());
            case 2 -> String.format("%,d", stats.getCount());
            case 3 -> String.format("%,d", Math.round(stats.getAvgDensity()));
            case 4 -> String.format("%,d", stats.getMinDensity());
            case 5 -> String.format("%,d", stats.getMaxDensity());
            default -> null;
        };
    }

    private boolean isSame(List<StreamDensityStats> current, List<StreamDensityStats> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        for (int i = 0; i < incoming.size(); i++)
        {
            StreamDensityStats s1 = current.get(i);
            StreamDensityStats s2 = incoming.get(i);

            if (s1.getPid() != s2.getPid() ||
                s1.getCount() != s2.getCount())
                return false;
        }

        return true;
    }
}
