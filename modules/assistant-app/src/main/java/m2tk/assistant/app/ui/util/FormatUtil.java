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

public final class FormatUtil
{
    private FormatUtil()
    {
    }

    public static String formatBitrate(long bitrate)
    {
        if (bitrate >= 1000000000L)
            return String.format("%.3f Gbps", bitrate / 1000000000.0);
        if (bitrate >= 1000000)
            return String.format("%.3f Mbps", bitrate / 1000000.0);
        if (bitrate >= 1000)
            return String.format("%.3f Kbps", bitrate / 1000.0);
        return String.format("%d bps", bitrate);
    }

    public static String formatDuration(int duration)
    {
        int hh = duration / 3600;
        int mm = duration % 3600 / 60;
        int ss = duration % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }
}
