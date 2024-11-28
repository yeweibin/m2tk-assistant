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
package m2tk.assistant.api.presets;

import java.util.HashMap;
import java.util.Map;

public final class StreamTypes
{
    private static final Map<Integer, String> NAMES;

    static
    {
        NAMES = new HashMap<>();
        NAMES.put(0x01, "MPEG-1 视频");
        NAMES.put(0x02, "MPEG-2 视频");
        NAMES.put(0x03, "MPEG-1 音频");
        NAMES.put(0x04, "MPEG-2 音频");
        NAMES.put(0x05, "ISO/IEC 13818-1 私有段");
        NAMES.put(0x06, "ISO/IEC 13818-1 私有PES");
        NAMES.put(0x08, "DSM-CC 数据段");
        NAMES.put(0x0A, "ISO/IEC 13818-1 A类");
        NAMES.put(0x0B, "ISO/IEC 13818-1 B类");
        NAMES.put(0x0C, "ISO/IEC 13818-1 C类");
        NAMES.put(0x0D, "ISO/IEC 13818-1 D类");
        NAMES.put(0x0F, "AAC 音频");
        NAMES.put(0x10, "H.263 视频");
        NAMES.put(0x11, "MPEG-4 音频");
        NAMES.put(0x17, "DSM-CC 数据轮播元信息");
        NAMES.put(0x18, "DSM-CC 对象轮播元信息");
        NAMES.put(0x1B, "H.264 视频");
        NAMES.put(0x24, "H.265 视频");
        NAMES.put(0x42, "AVS 视频");
        NAMES.put(0x81, "AC3 音频");
        NAMES.put(0x82, "DTS 音频（6通道）");
        NAMES.put(0x83, "Dolby TrueHD 音频");
        NAMES.put(0x84, "Dolby Digital Plus 音频");
        NAMES.put(0x85, "DTS 音频（8通道）");
        NAMES.put(0xEA, "WMV9 视频");
    }

    private StreamTypes()
    {
    }

    public static final String CATEGORY_VIDEO = "Video";
    public static final String CATEGORY_AUDIO = "Audio";
    public static final String CATEGORY_DATA = "Data";
    public static final String CATEGORY_USER_PRIVATE = "UserPrivate";
    public static final String CATEGORY_NULL_PACKET = "NullPacket";

    public static void register(int type, String description)
    {
        NAMES.put(type, description);
    }

    public static String description(int type)
    {
        String name = NAMES.get(type);
        if (name != null)
            return name;

        return (type < 0x80) ? "MPEG 保留" : "用户私有";
    }

    public static String category(int type)
    {
        return switch (type)
        {
            case 0x01, 0x02, 0x10, 0x1B, 0x24, 0x42, 0xEA -> CATEGORY_VIDEO;
            case 0x03, 0x04, 0x0F, 0x11, 0x81, 0x82, 0x83, 0x84, 0x85 -> CATEGORY_AUDIO;
            case 0x05, 0x06, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x17, 0x18 -> CATEGORY_DATA;
            default -> CATEGORY_USER_PRIVATE;
        };
    }
}
