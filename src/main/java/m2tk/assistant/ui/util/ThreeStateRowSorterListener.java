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

package m2tk.assistant.ui.util;

import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import java.util.List;

public class ThreeStateRowSorterListener implements RowSorterListener
{
    private int counter = 0;
    private int lastColumn = -1;
    private final RowSorter<?> rowSorter;

    public ThreeStateRowSorterListener(RowSorter<?> sorter)
    {
        rowSorter = sorter;
    }

    @Override
    public void sorterChanged(RowSorterEvent e)
    {
        List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
        if (sortKeys.isEmpty())
            return;

        RowSorter.SortKey sortKey = sortKeys.get(0);
        if (lastColumn != sortKey.getColumn())
        {
            lastColumn = sortKey.getColumn();
            counter = 0;
        }

        if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED)
            counter++;

        if (counter > 2)
        {
            counter = 0;
            rowSorter.setSortKeys(null); // 恢复默认排序（无排序）
            // 正排 -> 倒排 -> 还原 -> 正排 -> 倒排 -> 还原 -> ...（循环）
        }
    }
}
