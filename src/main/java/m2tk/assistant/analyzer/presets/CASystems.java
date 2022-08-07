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

public class CASystems
{
    private static boolean between(int value, int first, int last)
    {
        return value >= first && value <= last;
    }

    public static String vendor(int systemId)
    {
        if (between(systemId, 0x0100, 0x01FF))
            return "Canal Plus";

        if (between(systemId, 0x0200, 0x02FF))
            return "CCETT";

        if (between(systemId, 0x0600, 0x06FF))
            return "Irdeto";

        if (between(systemId, 0x0A00, 0x0AFF))
            return "Nokia";

        if (between(systemId, 0x0D00, 0x0DFF))
            return "CryptoWorks (Irdeto)";

        if (between(systemId, 0x0E00, 0x0EFF))
            return "Scientific Atlanta";

        if (between(systemId, 0x0F00, 0x0FFF))
            return "Sony";

        if (between(systemId, 0x1000, 0x10FF))
            return "Tandberg Television";

        if (between(systemId, 0x1100, 0x11FF))
            return "Thomson";

        if (between(systemId, 0x1EC0, 0x1EC2) ||
            between(systemId, 0x4AEA, 0x4AEA))
            return "Cryptoguard AB";

        if (between(systemId, 0x2200, 0x22FF))
            return "Harmonic";

        if (between(systemId, 0x4825, 0x4825))
            return "北京中传数广";

        if (between(systemId, 0x4900, 0x49FF))
            return "中视联（DTVIA）";

        if (between(systemId, 0x4AB0, 0x4ABF))
            return "算通科技";

        if (between(systemId, 0x4ADB, 0x4ADB))
            return "山东泰信";

        if (between(systemId, 0x4AF1, 0x4AF2))
            return "中数传媒";

        if (between(systemId, 0x4AF6, 0x4AF6) ||
            between(systemId, 0x4B00, 0x4B02))
            return "同方凌讯";

        if (between(systemId, 0x4B0A, 0x4B0B))
            return "数码视讯";

        return "";
    }
}
