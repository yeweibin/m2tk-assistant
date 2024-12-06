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

import m2tk.assistant.api.domain.TR290Event;

import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TR290EventTableModel extends AbstractTableModel
{
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] COLUMNS = {
        "序号", "时间", "PID", "位置", "描述"
    };

    private static final Class<?>[] COLUMN_CLASSES = {
        Integer.class, String.class, String.class, String.class, String.class
    };

    private final List<TR290Event> data = new ArrayList<>();

    public void update(List<TR290Event> events)
    {
        data.clear();
        data.addAll(events);
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
        TR290Event event = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> rowIndex + 1;
            case 1 -> event.getTimestamp()
                           .atZoneSameInstant(ZoneId.systemDefault())
                           .format(TIME_FORMATTER);
            case 2 -> String.format("%d (0x%04X)", event.getStream(), event.getStream());
            case 3 -> String.format("%,d", event.getPosition());
            case 4 -> event.getDescription();
            default -> null;
        };
    }
}
