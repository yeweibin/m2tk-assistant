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

public class RunningStatus
{
    public static String name(int status)
    {
        switch (status)
        {
            case 0x00:
                return "未定义";
            case 0x01:
                return "未开始";
            case 0x02:
                return "即将开始";
            case 0x03:
                return "暂停中";
            case 0x04:
                return "进行中";
            case 0x05:
                return "已结束";
            default:
                return "保留";
        }
    }
}
