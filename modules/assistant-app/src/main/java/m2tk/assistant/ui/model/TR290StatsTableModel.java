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

package m2tk.assistant.ui.model;

import m2tk.assistant.LargeIcons;
import m2tk.assistant.core.domain.TR290Event;
import m2tk.assistant.core.domain.TR290Stats;
import m2tk.assistant.core.presets.TR290ErrorTypes;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class TR290StatsTableModel extends AbstractTableModel
{
    private static final int COLUMN_INDEX_STATUS_INDICATOR = 0;
    private static final int COLUMN_INDEX_TYPE_INDICATOR = 1;
    private static final int COLUMN_INDEX_COUNT = 2;
    private static final int COLUMN_INDEX_TIME_OF_LAST_ERROR = 3;
    private static final int COLUMN_INDEX_ERROR_MESSAGE = 4;

    private static final int ROW_INDEX_PRIORITY_1 = 0;
    private static final int ROW_INDEX_SYNC_LOSS = 1;
    private static final int ROW_INDEX_SYNC_BYTE_ERROR = 2;
    private static final int ROW_INDEX_PAT_ERROR_2 = 3;
    private static final int ROW_INDEX_CONTINUITY_COUNT_ERROR = 4;
    private static final int ROW_INDEX_PMT_ERROR_2 = 5;
    private static final int ROW_INDEX_PID_ERROR = 6;
    private static final int ROW_INDEX_PRIORITY_2 = 7;
    private static final int ROW_INDEX_TRANSPORT_ERROR = 8;
    private static final int ROW_INDEX_CRC_ERROR = 9;
    private static final int ROW_INDEX_PCR_REPETITION_ERROR = 10;
    private static final int ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR = 11;
    private static final int ROW_INDEX_PCR_ACCURACY_ERROR = 12;
    private static final int ROW_INDEX_CAT_ERROR = 13;
    private static final int ROW_INDEX_PRIORITY_3 = 14;
    private static final int ROW_INDEX_NIT_ACTUAL_ERROR = 15;
    private static final int ROW_INDEX_NIT_OTHER_ERROR = 16;
    private static final int ROW_INDEX_SI_REPETITION_ERROR = 17;
    private static final int ROW_INDEX_UNREFERENCED_PID = 18;
    private static final int ROW_INDEX_SDT_ACTUAL_ERROR = 19;
    private static final int ROW_INDEX_SDT_OTHER_ERROR = 20;
    private static final int ROW_INDEX_EIT_ACTUAL_ERROR = 21;
    private static final int ROW_INDEX_EIT_OTHER_ERROR = 22;
    private static final int ROW_INDEX_RST_ERROR = 23;
    private static final int ROW_INDEX_TDT_ERROR = 24;

    private static final int ROW_COUNT = 25;
    private static final String[] COLUMNS = {
        "", "错误类别", "错误总数", "最近一次时间", "错误描述"
    };
    private static final Class<?>[] COLUMN_CLASSES = {
            Icon.class, String.class, Integer.class, String.class, String.class
    };

    private TR290Stats data = new TR290Stats();

    /*
     * 整个表是固定大小的，只有里面的数值和事件内容会变化。
     */

    public void update(TR290Stats stats)
    {
        if (!Objects.equals(data, stats))
        {
            data = stats;
            fireTableDataChanged();
        }
    }

    @Override
    public int getRowCount()
    {
        return ROW_COUNT;
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
        return COLUMN_CLASSES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch (columnIndex)
        {
            case COLUMN_INDEX_STATUS_INDICATOR:
            {
                return switch (rowIndex)
                {
                    case ROW_INDEX_SYNC_LOSS ->
                            data.getErrorCount(TR290ErrorTypes.TS_SYNC_LOSS) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_SYNC_BYTE_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.SYNC_BYTE_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PAT_ERROR_2 ->
                            data.getErrorCount(TR290ErrorTypes.PAT_ERROR_2) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_CONTINUITY_COUNT_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.CONTINUITY_COUNT_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PMT_ERROR_2 ->
                            data.getErrorCount(TR290ErrorTypes.PMT_ERROR_2) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PID_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.PID_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_TRANSPORT_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.TRANSPORT_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_CRC_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.CRC_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PCR_REPETITION_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.PCR_REPETITION_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_PCR_ACCURACY_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.PCR_ACCURACY_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_CAT_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.CAT_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_NIT_ACTUAL_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.NIT_ACTUAL_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_NIT_OTHER_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.NIT_OTHER_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_SI_REPETITION_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.SI_REPETITION_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_UNREFERENCED_PID ->
                            data.getErrorCount(TR290ErrorTypes.UNREFERENCED_PID) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_SDT_ACTUAL_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.SDT_ACTUAL_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_SDT_OTHER_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.SDT_OTHER_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_EIT_ACTUAL_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.EIT_ACTUAL_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_EIT_OTHER_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.EIT_OTHER_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_RST_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.RST_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    case ROW_INDEX_TDT_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.TDT_ERROR) > 0 ? LargeIcons.DOT_RED : LargeIcons.DOT_GREEN;
                    default -> null;
                };
            }

            case COLUMN_INDEX_TYPE_INDICATOR:
            {
                return switch (rowIndex)
                {
                    case ROW_INDEX_PRIORITY_1 -> "--------第一级--------";
                    case ROW_INDEX_SYNC_LOSS -> TR290ErrorTypes.TS_SYNC_LOSS;
                    case ROW_INDEX_SYNC_BYTE_ERROR -> TR290ErrorTypes.SYNC_BYTE_ERROR;
                    case ROW_INDEX_PAT_ERROR_2 -> TR290ErrorTypes.PAT_ERROR_2;
                    case ROW_INDEX_CONTINUITY_COUNT_ERROR -> TR290ErrorTypes.CONTINUITY_COUNT_ERROR;
                    case ROW_INDEX_PMT_ERROR_2 -> TR290ErrorTypes.PMT_ERROR_2;
                    case ROW_INDEX_PID_ERROR -> TR290ErrorTypes.PID_ERROR;
                    case ROW_INDEX_PRIORITY_2 -> "--------第二级--------";
                    case ROW_INDEX_TRANSPORT_ERROR -> TR290ErrorTypes.TRANSPORT_ERROR;
                    case ROW_INDEX_CRC_ERROR -> TR290ErrorTypes.CRC_ERROR;
                    case ROW_INDEX_PCR_REPETITION_ERROR -> TR290ErrorTypes.PCR_REPETITION_ERROR;
                    case ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR ->
                            TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR;
                    case ROW_INDEX_PCR_ACCURACY_ERROR -> TR290ErrorTypes.PCR_ACCURACY_ERROR;
                    case ROW_INDEX_CAT_ERROR -> TR290ErrorTypes.CAT_ERROR;
                    case ROW_INDEX_PRIORITY_3 -> "--------第三级--------";
                    case ROW_INDEX_NIT_ACTUAL_ERROR -> TR290ErrorTypes.NIT_ACTUAL_ERROR;
                    case ROW_INDEX_NIT_OTHER_ERROR -> TR290ErrorTypes.NIT_OTHER_ERROR;
                    case ROW_INDEX_SI_REPETITION_ERROR -> TR290ErrorTypes.SI_REPETITION_ERROR;
                    case ROW_INDEX_UNREFERENCED_PID -> TR290ErrorTypes.UNREFERENCED_PID;
                    case ROW_INDEX_SDT_ACTUAL_ERROR -> TR290ErrorTypes.SDT_ACTUAL_ERROR;
                    case ROW_INDEX_SDT_OTHER_ERROR -> TR290ErrorTypes.SDT_OTHER_ERROR;
                    case ROW_INDEX_EIT_ACTUAL_ERROR -> TR290ErrorTypes.EIT_ACTUAL_ERROR;
                    case ROW_INDEX_EIT_OTHER_ERROR -> TR290ErrorTypes.EIT_OTHER_ERROR;
                    case ROW_INDEX_RST_ERROR -> TR290ErrorTypes.RST_ERROR;
                    case ROW_INDEX_TDT_ERROR -> TR290ErrorTypes.TDT_ERROR;
                    default -> null;
                };
            }

            case COLUMN_INDEX_COUNT:
            {
                return switch (rowIndex)
                {
                    case ROW_INDEX_SYNC_LOSS -> data.getErrorCount(TR290ErrorTypes.TS_SYNC_LOSS);
                    case ROW_INDEX_SYNC_BYTE_ERROR -> data.getErrorCount(TR290ErrorTypes.SYNC_BYTE_ERROR);
                    case ROW_INDEX_PAT_ERROR_2 -> data.getErrorCount(TR290ErrorTypes.PAT_ERROR_2);
                    case ROW_INDEX_CONTINUITY_COUNT_ERROR -> data.getErrorCount(TR290ErrorTypes.CONTINUITY_COUNT_ERROR);
                    case ROW_INDEX_PMT_ERROR_2 -> data.getErrorCount(TR290ErrorTypes.PMT_ERROR_2);
                    case ROW_INDEX_PID_ERROR -> data.getErrorCount(TR290ErrorTypes.PID_ERROR);
                    case ROW_INDEX_TRANSPORT_ERROR -> data.getErrorCount(TR290ErrorTypes.TRANSPORT_ERROR);
                    case ROW_INDEX_CRC_ERROR -> data.getErrorCount(TR290ErrorTypes.CRC_ERROR);
                    case ROW_INDEX_PCR_REPETITION_ERROR -> data.getErrorCount(TR290ErrorTypes.PCR_REPETITION_ERROR);
                    case ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR ->
                            data.getErrorCount(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR);
                    case ROW_INDEX_PCR_ACCURACY_ERROR -> data.getErrorCount(TR290ErrorTypes.PCR_ACCURACY_ERROR);
                    case ROW_INDEX_CAT_ERROR -> data.getErrorCount(TR290ErrorTypes.CAT_ERROR);
                    case ROW_INDEX_NIT_ACTUAL_ERROR -> data.getErrorCount(TR290ErrorTypes.NIT_ACTUAL_ERROR);
                    case ROW_INDEX_NIT_OTHER_ERROR -> data.getErrorCount(TR290ErrorTypes.NIT_OTHER_ERROR);
                    case ROW_INDEX_SI_REPETITION_ERROR -> data.getErrorCount(TR290ErrorTypes.SI_REPETITION_ERROR);
                    case ROW_INDEX_UNREFERENCED_PID -> data.getErrorCount(TR290ErrorTypes.UNREFERENCED_PID);
                    case ROW_INDEX_SDT_ACTUAL_ERROR -> data.getErrorCount(TR290ErrorTypes.SDT_ACTUAL_ERROR);
                    case ROW_INDEX_SDT_OTHER_ERROR -> data.getErrorCount(TR290ErrorTypes.SDT_OTHER_ERROR);
                    case ROW_INDEX_EIT_ACTUAL_ERROR -> data.getErrorCount(TR290ErrorTypes.EIT_ACTUAL_ERROR);
                    case ROW_INDEX_EIT_OTHER_ERROR -> data.getErrorCount(TR290ErrorTypes.EIT_OTHER_ERROR);
                    case ROW_INDEX_RST_ERROR -> data.getErrorCount(TR290ErrorTypes.RST_ERROR);
                    case ROW_INDEX_TDT_ERROR -> data.getErrorCount(TR290ErrorTypes.TDT_ERROR);
                    default -> null;
                };
            }

            case COLUMN_INDEX_TIME_OF_LAST_ERROR:
            {
                if (rowIndex == ROW_INDEX_PRIORITY_1 ||
                    rowIndex == ROW_INDEX_PRIORITY_2 ||
                    rowIndex == ROW_INDEX_PRIORITY_3)
                    return null;

                TR290Event event = switch (rowIndex)
                {
                    case ROW_INDEX_SYNC_LOSS -> data.getErrorLastEvent(TR290ErrorTypes.TS_SYNC_LOSS);
                    case ROW_INDEX_SYNC_BYTE_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SYNC_BYTE_ERROR);
                    case ROW_INDEX_PAT_ERROR_2 -> data.getErrorLastEvent(TR290ErrorTypes.PAT_ERROR_2);
                    case ROW_INDEX_CONTINUITY_COUNT_ERROR ->
                            data.getErrorLastEvent(TR290ErrorTypes.CONTINUITY_COUNT_ERROR);
                    case ROW_INDEX_PMT_ERROR_2 -> data.getErrorLastEvent(TR290ErrorTypes.PMT_ERROR_2);
                    case ROW_INDEX_PID_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PID_ERROR);
                    case ROW_INDEX_TRANSPORT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.TRANSPORT_ERROR);
                    case ROW_INDEX_CRC_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.CRC_ERROR);
                    case ROW_INDEX_PCR_REPETITION_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PCR_REPETITION_ERROR);
                    case ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR ->
                            data.getErrorLastEvent(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR);
                    case ROW_INDEX_PCR_ACCURACY_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PCR_ACCURACY_ERROR);
                    case ROW_INDEX_CAT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.CAT_ERROR);
                    case ROW_INDEX_NIT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.NIT_ACTUAL_ERROR);
                    case ROW_INDEX_NIT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.NIT_OTHER_ERROR);
                    case ROW_INDEX_SI_REPETITION_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SI_REPETITION_ERROR);
                    case ROW_INDEX_UNREFERENCED_PID -> data.getErrorLastEvent(TR290ErrorTypes.UNREFERENCED_PID);
                    case ROW_INDEX_SDT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SDT_ACTUAL_ERROR);
                    case ROW_INDEX_SDT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SDT_OTHER_ERROR);
                    case ROW_INDEX_EIT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.EIT_ACTUAL_ERROR);
                    case ROW_INDEX_EIT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.EIT_OTHER_ERROR);
                    case ROW_INDEX_RST_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.RST_ERROR);
                    case ROW_INDEX_TDT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.TDT_ERROR);
                    default -> null;
                };
                return (event == null) ? "-" : event.getTimestamp();
            }

            case COLUMN_INDEX_ERROR_MESSAGE:
            {
                if (rowIndex == ROW_INDEX_PRIORITY_1 ||
                    rowIndex == ROW_INDEX_PRIORITY_2 ||
                    rowIndex == ROW_INDEX_PRIORITY_3)
                    return null;

                TR290Event event = switch (rowIndex)
                {
                    case ROW_INDEX_SYNC_LOSS -> data.getErrorLastEvent(TR290ErrorTypes.TS_SYNC_LOSS);
                    case ROW_INDEX_SYNC_BYTE_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SYNC_BYTE_ERROR);
                    case ROW_INDEX_PAT_ERROR_2 -> data.getErrorLastEvent(TR290ErrorTypes.PAT_ERROR_2);
                    case ROW_INDEX_CONTINUITY_COUNT_ERROR ->
                            data.getErrorLastEvent(TR290ErrorTypes.CONTINUITY_COUNT_ERROR);
                    case ROW_INDEX_PMT_ERROR_2 -> data.getErrorLastEvent(TR290ErrorTypes.PMT_ERROR_2);
                    case ROW_INDEX_PID_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PID_ERROR);
                    case ROW_INDEX_TRANSPORT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.TRANSPORT_ERROR);
                    case ROW_INDEX_CRC_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.CRC_ERROR);
                    case ROW_INDEX_PCR_REPETITION_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PCR_REPETITION_ERROR);
                    case ROW_INDEX_PCR_DISCONTINUITY_INDICATOR_ERROR ->
                            data.getErrorLastEvent(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR);
                    case ROW_INDEX_PCR_ACCURACY_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.PCR_ACCURACY_ERROR);
                    case ROW_INDEX_CAT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.CAT_ERROR);
                    case ROW_INDEX_NIT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.NIT_ACTUAL_ERROR);
                    case ROW_INDEX_NIT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.NIT_OTHER_ERROR);
                    case ROW_INDEX_SI_REPETITION_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SI_REPETITION_ERROR);
                    case ROW_INDEX_UNREFERENCED_PID -> data.getErrorLastEvent(TR290ErrorTypes.UNREFERENCED_PID);
                    case ROW_INDEX_SDT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SDT_ACTUAL_ERROR);
                    case ROW_INDEX_SDT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.SDT_OTHER_ERROR);
                    case ROW_INDEX_EIT_ACTUAL_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.EIT_ACTUAL_ERROR);
                    case ROW_INDEX_EIT_OTHER_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.EIT_OTHER_ERROR);
                    case ROW_INDEX_RST_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.RST_ERROR);
                    case ROW_INDEX_TDT_ERROR -> data.getErrorLastEvent(TR290ErrorTypes.TDT_ERROR);
                    default -> null;
                };
                return (event == null) ? "-" : event.getDescription();
            }

            default:
                return null;
        }
    }
}
