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

import m2tk.assistant.api.domain.ElementaryStream;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreamInfoTableModel extends AbstractTableModel
{
    private final transient List<ElementaryStream> data;
    private static final String[] COLUMNS = {
        "序号", "PID", "基本流描述", "带宽占比", "传输包", "传输错误", "连续计数错误", "摘要"
    };
    private static final Class<?>[] COLUMN_CLASSES = {
        Integer.class, String.class, String.class, Double.class, String.class, String.class, String.class, String.class
    };

    public StreamInfoTableModel()
    {
        data = new ArrayList<>();
    }

    public void update(List<ElementaryStream> streams)
    {
        if (isSame(data, streams))
            return;

        data.clear();
        data.addAll(streams);
        fireTableDataChanged();
    }

    public ElementaryStream getRow(int row)
    {
        return data.get(row);
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
        ElementaryStream stream = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> rowIndex + 1;
            case 1 -> String.format("%d (0x%04X)", stream.getStreamPid(), stream.getStreamPid());
            case 2 -> stream.getDescription();
            case 3 -> stream.getRatio();
            case 4 -> String.format("%,d", stream.getPacketCount());
            case 5 -> String.format("%,d", stream.getTransportErrorCount());
            case 6 -> String.format("%,d", stream.getContinuityErrorCount());
            case 7 -> String.format("%s,%b,%b,%b,%b,",  // 依次表示：流类型，是否加扰，是否存在PCR，是否存在传输错误，是否存在连续计数错误。
                                    stream.getCategory(),
                                    stream.isScrambled(),
                                    stream.getPcrCount() > 0,
                                    stream.getTransportErrorCount() > 0,
                                    stream.getContinuityErrorCount() > 0);
            default -> null;
        };
    }

    private boolean isSame(List<ElementaryStream> current, List<ElementaryStream> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming = new ArrayList<>(incoming);
        incoming.sort(Comparator.comparingInt(ElementaryStream::getStreamPid));

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            ElementaryStream s1 = current.get(i);
            ElementaryStream s2 = incoming.get(i);

            if (s1.getStreamPid() != s2.getStreamPid() ||
                s1.getPacketCount() != s2.getPacketCount() ||
                s1.getTransportErrorCount() != s2.getTransportErrorCount() ||
                s1.getContinuityErrorCount() != s2.getContinuityErrorCount())
                return false;
        }

        return true;
    }
}
