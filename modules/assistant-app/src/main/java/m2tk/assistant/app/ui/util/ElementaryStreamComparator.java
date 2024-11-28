/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app.ui.util;

import m2tk.assistant.api.domain.ElementaryStream;
import m2tk.assistant.api.presets.StreamTypes;

import java.util.Comparator;
import java.util.Objects;

public class ElementaryStreamComparator implements Comparator<ElementaryStream>
{
    @Override
    public int compare(ElementaryStream s1, ElementaryStream s2)
    {
        // 视频 -> 音频 -> 数据 -> 私有
        // 同类则比pid
        String c1 = s1.getCategory();
        String c2 = s2.getCategory();

        if (Objects.equals(c1, c2))
            return Integer.compare(s1.getStreamPid(), s2.getStreamPid());

        return switch (c1)
        {
            case StreamTypes.CATEGORY_VIDEO -> -1; // 视频排在最前面
            case StreamTypes.CATEGORY_AUDIO -> Objects.equals(c2, StreamTypes.CATEGORY_VIDEO) ? 1 : -1; // 音频落后视频，先于其他
            case StreamTypes.CATEGORY_DATA -> Objects.equals(c2, StreamTypes.CATEGORY_USER_PRIVATE) ? -1 : 1; // 数据先于私有数据，落后于音视频
            case StreamTypes.CATEGORY_USER_PRIVATE -> 1; // 私有类型排在最后面
            default -> Integer.compare(s1.getStreamPid(), s2.getStreamPid()); // 保护性措施，实际并不会触发。
        };
    }
}
