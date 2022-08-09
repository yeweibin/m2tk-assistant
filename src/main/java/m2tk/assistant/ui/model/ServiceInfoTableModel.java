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

import m2tk.assistant.analyzer.domain.SIService;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceInfoTableModel extends AbstractTableModel
{
    private final List<SIService> data;
    private static final String[] COLUMNS = {
            "序号", "名称", "提供商", "业务类型", "业务号", "原始网络号", "传输流号"
    };
    private static final Class<?>[] COLUMN_CLASSES = {
            Integer.class, String.class, String.class, String.class, Integer.class, Integer.class, Integer.class
    };

    public ServiceInfoTableModel()
    {
        data = new ArrayList<>();
    }

    public void update(List<SIService> services)
    {
        if (isSame(data, services))
            return;

        data.clear();
        data.addAll(services);
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
        SIService service = data.get(rowIndex);
        switch (columnIndex)
        {
            case 0:
                return rowIndex + 1;
            case 1:
                return service.getServiceName();
            case 2:
                return service.getServiceProvider();
            case 3:
                return service.getServiceType();
            case 4:
                return service.getServiceId();
            case 5:
                return service.getOriginalNetworkId();
            case 6:
                return service.getTransportStreamId();
            default:
                return null;
        }
    }

    private boolean isSame(List<SIService> current, List<SIService> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            SIService s1 = current.get(i);
            SIService s2 = incoming.get(i);

            if (!Objects.equals(s1, s2))
                return false;
        }

        return true;
    }
}
