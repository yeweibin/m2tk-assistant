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

package m2tk.assistant.analyzer.presets;

import java.util.HashMap;
import java.util.Map;

public class StreamTypes
{
    private static final Map<Integer, String> NAMES;
    static
    {
        NAMES = new HashMap<>();
        NAMES.put(0x01, "MPEG1视频");
        NAMES.put(0x02, "MPEG2视频");
        NAMES.put(0x03, "MPEG1音频");
        NAMES.put(0x04, "MPEG2音频");
        NAMES.put(0x05, "ISO/IEC 13818-1 私有段");
        NAMES.put(0x06, "ISO/IEC 13818-1 私有PES");
        NAMES.put(0x08, "DSM-CC 数据段");
        NAMES.put(0x0A, "ISO/IEC 13818-1 A类");
        NAMES.put(0x0B, "ISO/IEC 13818-1 B类");
        NAMES.put(0x0C, "ISO/IEC 13818-1 C类");
        NAMES.put(0x0D, "ISO/IEC 13818-1 D类");
        NAMES.put(0x0F, "AAC音频");
        NAMES.put(0x10, "H.263视频");
        NAMES.put(0x11, "MPEG4音频");
        NAMES.put(0x17, "DSM-CC数据轮播元信息");
        NAMES.put(0x18, "DSM-CC对象轮播元信息");
        NAMES.put(0x1B, "H.264视频");
        NAMES.put(0x24, "H.265视频");
        NAMES.put(0x42, "AVS视频");
        NAMES.put(0x81, "AC3音频");
        NAMES.put(0x82, "DTS音频（6通道）");
        NAMES.put(0x83, "Dolby TrueHD音频");
        NAMES.put(0x84, "Dolby Digital Plus音频");
        NAMES.put(0x85, "DTS音频（8通道）");
        NAMES.put(0xEA, "WMV9视频");
    }

    public static final String CATEGORY_VIDEO = "V";
    public static final String CATEGORY_AUDIO = "A";
    public static final String CATEGORY_DATA = "D";
    public static final String CATEGORY_USER_PRIVATE = "U";

    public static String description(int type)
    {
        String name = NAMES.get(type);
        if (name != null)
            return name;

        return (type < 0x80) ? "MPEG保留" : "用户私有";
    }

    public static String category(int type)
    {
        switch (type)
        {
            case 0x01:
            case 0x02:
            case 0x10:
            case 0x1B:
            case 0x24:
            case 0x42:
            case 0xEA:
                return CATEGORY_VIDEO;
            case 0x03:
            case 0x04:
            case 0x0F:
            case 0x11:
            case 0x81:
            case 0x82:
            case 0x83:
            case 0x84:
            case 0x85:
                return CATEGORY_AUDIO;
            case 0x05:
            case 0x06:
            case 0x08:
            case 0x09:
            case 0x0A:
            case 0x0B:
            case 0x0C:
            case 0x0D:
            case 0x17:
            case 0x18:
                return CATEGORY_DATA;
            default:
                return CATEGORY_USER_PRIVATE;
        }
    }
}
