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

import m2tk.assistant.api.domain.SIEvent;
import m2tk.assistant.app.ui.util.FormatUtil;

import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EventTableModel extends AbstractTableModel
{
    private final List<SIEvent> data = new ArrayList<>();
    private final String[] COLUMNS = {"类型", "事件号", "开始时间", "持续时长", "语言", "标题", "描述"};
    private final Class<?>[] COLUMN_CLASSES = {String.class, Integer.class, String.class, String.class, String.class, String.class, String.class};

    private static final SIEvent PLACEHOLDER_PRESENT_EVENT = new SIEvent();
    private static final SIEvent PLACEHOLDER_FOLLOWING_EVENT = new SIEvent();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    static
    {
        PLACEHOLDER_PRESENT_EVENT.setPresentEvent(true);
        PLACEHOLDER_PRESENT_EVENT.setScheduleEvent(false);
        PLACEHOLDER_FOLLOWING_EVENT.setPresentEvent(false);
        PLACEHOLDER_FOLLOWING_EVENT.setScheduleEvent(false);
    }

    public void update(List<SIEvent> events)
    {
        SIEvent pEvent = events.stream()
                               .filter(e -> !e.isScheduleEvent() && e.isPresentEvent())
                               .findFirst()
                               .orElse(PLACEHOLDER_PRESENT_EVENT);
        SIEvent fEvent = events.stream()
                               .filter(e -> !e.isScheduleEvent() && !e.isPresentEvent())
                               .findFirst()
                               .orElse(PLACEHOLDER_FOLLOWING_EVENT);

        List<SIEvent> copy = new ArrayList<>(events);
        copy.removeIf(e -> !e.isScheduleEvent());

        data.clear();
        data.add(pEvent);
        data.add(fEvent);
        data.addAll(copy);
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
        SIEvent event = data.get(rowIndex);
        return switch (columnIndex)
        {
            case 0 -> event.isScheduleEvent()
                      ? ""
                      : event.isPresentEvent() ? "当前" : "后续";
            case 1 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : event.getEventId();
            case 2 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : event.getStartTime()
                             .atZoneSameInstant(ZoneId.systemDefault())
                             .format(TIME_FORMATTER);
            case 3 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : FormatUtil.formatDuration(event.getDuration());
            case 4 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : event.getLanguageCode();
            case 5 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : event.getTitle();
            case 6 -> (event == PLACEHOLDER_PRESENT_EVENT || event == PLACEHOLDER_FOLLOWING_EVENT)
                      ? null
                      : event.getDescription();
            default -> null;
        };
    }
}
