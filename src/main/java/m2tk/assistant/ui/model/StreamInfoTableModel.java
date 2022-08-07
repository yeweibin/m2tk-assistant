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
import m2tk.assistant.dbi.entity.StreamEntity;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreamInfoTableModel extends AbstractTableModel
{
    private final List<StreamEntity> data;
    private static final String[] COLUMNS = {
            "序号", "流状态", "加扰状态", "PID", "平均Kbps", "带宽占比", "类型描述", "包数量", "连续计数错误"
    };

    public StreamInfoTableModel()
    {
        data = new ArrayList<>();
    }

    public void update(List<StreamEntity> streams)
    {
        if (isSame(data, streams))
            return;

        data.clear();
        data.addAll(streams);
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
        return (columnIndex == 1 || columnIndex == 2) ? ImageIcon.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        StreamEntity entity = data.get(rowIndex);
        switch (columnIndex)
        {
            case 0:
                return rowIndex + 1;
            case 1:
                return entity.getContinuityErrorCount() == 0 ? SmallIcons.CHECK : SmallIcons.EXCLAMATION;
            case 2:
                return entity.isScrambled() ? SmallIcons.LOCK : null;
            case 3:
                return String.format("%d (0x%04X)", entity.getPid(), entity.getPid());
            case 4:
                return String.format("%,.02f", entity.getBitrate() / 1000.0);
            case 5:
                return String.format("%.02f%%", 100 * entity.getRatio());
            case 6:
                return entity.getDescription();
            case 7:
                return String.format("%,d", entity.getPacketCount());
            case 8:
                return String.format("%,d", entity.getContinuityErrorCount());
            default:
                return null;
        }
    }

    private boolean isSame(List<StreamEntity> current, List<StreamEntity> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparingInt(StreamEntity::getPid));

        int n = current.size();
        for (int i = 0; i < n; i ++)
        {
            StreamEntity s1 = current.get(i);
            StreamEntity s2 = incoming.get(i);

            if (s1.getId() != s2.getId() ||
                s1.getPid() != s2.getPid() ||
                s1.getPacketCount() != s2.getPacketCount())
                return false;
        }

        return true;
    }
}
