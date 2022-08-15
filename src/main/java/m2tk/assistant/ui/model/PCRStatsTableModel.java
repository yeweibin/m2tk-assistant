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

package m2tk.assistant.ui.model;

import m2tk.assistant.SmallIcons;
import m2tk.assistant.dbi.entity.PCRStatEntity;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class PCRStatsTableModel extends AbstractTableModel
{
    private final List<PCRStatEntity> data = new ArrayList<>();
    private static final String[] COLUMNS = {
            "", "PID", "PCR总数", "平均码率", "平均间隔", "最小间隔", "最大间隔", "间隔越界", "平均精度", "最小精度", "最大精度", "精度越界", ""
    };

    public void update(List<PCRStatEntity> stats)
    {
        if (isSame(data, stats))
            return;

        data.clear();
        data.addAll(stats);
        fireTableDataChanged();
    }

    public PCRStatEntity getStatAtRow(int row)
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
        return (columnIndex == 0) ? Icon.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        PCRStatEntity stat = data.get(rowIndex);
        switch (columnIndex)
        {
            case 0:
                return (stat.getRepetitionErrors() + stat.getDiscontinuityErrors() + stat.getAccuracyErrors()) > 0
                       ? SmallIcons.EXCLAMATION
                       : SmallIcons.CHECK;
            case 1:
                return String.format("0x%04X", stat.getPid());
            case 2:
                return String.format("%,d", stat.getPcrCount());
            case 3:
                return String.format("%,d bps", stat.getAvgBitrate());
            case 4:
                return String.format("%,d ms", stat.getAvgInterval() / 1000000);
            case 5:
                return String.format("%,d ms", stat.getMinInterval() / 1000000);
            case 6:
                return String.format("%,d ms", stat.getMaxInterval() / 1000000);
            case 7:
                return String.format("%,d", stat.getRepetitionErrors());
            case 8:
                return String.format("%,d ns", stat.getAvgAccuracy());
            case 9:
                return String.format("%,d ns", stat.getMinAccuracy());
            case 10:
                return String.format("%,d ns", stat.getMaxAccuracy());
            case 11:
                return String.format("%,d", stat.getAccuracyErrors());
            default:
                return null;
        }
    }

    private boolean isSame(List<PCRStatEntity> current, List<PCRStatEntity> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        for (int i = 0; i < incoming.size(); i++)
        {
            PCRStatEntity s1 = current.get(i);
            PCRStatEntity s2 = incoming.get(i);

            if (s1.getPid() != s2.getPid() ||
                s1.getPcrCount() != s2.getPcrCount())
                return false;
        }

        return true;
    }
}
