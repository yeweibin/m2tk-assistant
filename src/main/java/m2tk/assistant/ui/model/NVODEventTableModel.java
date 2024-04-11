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

import m2tk.assistant.analyzer.domain.NVODEvent;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class NVODEventTableModel extends AbstractTableModel
{
    private final List<NVODEvent> data = new ArrayList<>();
    private final String[] COLUMNS = {
            "类型", "事件号", "开始时间", "持续时间", "语言", "标题", "描述"
    };
    private final Class<?>[] COLUMN_CLASSES = {
            String.class, Integer.class, String.class, String.class, String.class, String.class, String.class
    };

    private static final NVODEvent EMPTY_PRESENT_EVENT = NVODEvent.ofTimeShifted(-1, -1, -1, -1,
                                                                                 -1, -1,
                                                                                 "", "", "",
                                                                                 "", "", true);
    private static final NVODEvent EMPTY_FOLLOWING_EVENT = NVODEvent.ofTimeShifted(-1, -1, -1, -1,
                                                                                   -1, -1,
                                                                                   "", "", "",
                                                                                   "", "", false);

    public void update(List<NVODEvent> events)
    {
        NVODEvent pEvent = events.stream()
                                 .filter(e -> e.isTimeShiftedEvent() && e.isPresentEvent())
                                 .findFirst()
                                 .orElse(EMPTY_PRESENT_EVENT);
        NVODEvent fEvent = events.stream()
                                 .filter(e -> e.isTimeShiftedEvent() && !e.isPresentEvent())
                                 .findFirst()
                                 .orElse(EMPTY_FOLLOWING_EVENT);

        data.clear();
        data.add(pEvent);
        data.add(fEvent);
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
        NVODEvent event = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> event.isPresentEvent() ? "当前" : "后续";
            case 1 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getEventId();
            case 2 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getStartTime();
            case 3 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getDuration();
            case 4 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getLanguageCode();
            case 5 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getEventName();
            case 6 -> (event == EMPTY_PRESENT_EVENT || event == EMPTY_FOLLOWING_EVENT)
                      ? null
                      : event.getEventDescription();
            default -> null;
        };
    }
}
