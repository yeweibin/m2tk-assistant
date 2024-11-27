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

import m2tk.assistant.api.domain.PCRStats;
import m2tk.assistant.app.ui.util.FormatUtil;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class PCRStatsTableModel extends AbstractTableModel
{
    private List<PCRStats> data = Collections.emptyList();

    private static final Icon BAD = FontIcon.of(FluentUiFilledAL.ERROR_CIRCLE_24, 20, Color.decode("#FD1D1D"));
    private static final Icon GOOD = FontIcon.of(FluentUiFilledAL.CHECKMARK_CIRCLE_24, 20, Color.decode("#7FBA00"));

    private static final String[] COLUMNS = {
            "", "PID", "PCR总数", "平均码率", "平均间隔", "最小间隔", "最大间隔", "间隔越界", "平均精度", "最小精度", "最大精度", "精度越界", ""
    };

    public void update(List<PCRStats> stats)
    {
        if (isSame(data, stats))
            return;

        data = stats;
        fireTableDataChanged();
    }

    public PCRStats getStatAtRow(int row)
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
        PCRStats stats = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> (stats.getRepetitionErrors() + stats.getDiscontinuityErrors() + stats.getAccuracyErrors()) > 0
                      ? BAD : GOOD;
            case 1 -> String.format("0x%04X", stats.getPid());
            case 2 -> String.format("%,d", stats.getPcrCount());
            case 3 -> FormatUtil.formatBitrate(stats.getAvgBitrate());
            case 4 -> String.format("%,d ms", stats.getAvgInterval() / 1000000);
            case 5 -> String.format("%,d ms", stats.getMinInterval() / 1000000);
            case 6 -> String.format("%,d ms", stats.getMaxInterval() / 1000000);
            case 7 -> String.format("%,d", stats.getRepetitionErrors());
            case 8 -> String.format("%,d ns", stats.getAvgAccuracy());
            case 9 -> String.format("%,d ns", stats.getMinAccuracy());
            case 10 -> String.format("%,d ns", stats.getMaxAccuracy());
            case 11 -> String.format("%,d", stats.getAccuracyErrors());
            default -> null;
        };
    }

    private boolean isSame(List<PCRStats> current, List<PCRStats> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        for (int i = 0; i < incoming.size(); i++)
        {
            PCRStats s1 = current.get(i);
            PCRStats s2 = incoming.get(i);

            if (s1.getPid() != s2.getPid() ||
                s1.getPcrCount() != s2.getPcrCount())
                return false;
        }

        return true;
    }
}
