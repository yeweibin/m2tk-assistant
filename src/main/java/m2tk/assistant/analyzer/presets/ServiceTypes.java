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

public class ServiceTypes
{
    private ServiceTypes() {}

    public static String name(int type)
    {
        switch (type)
        {
            case 0x00:
                return "未定义";
            case 0x01:
                return "数字电视业务";
            case 0x02:
                return "数字音频广播业务";
            case 0x03:
                return "图文电视业务";
            case 0x04:
                return "NVOD参考业务";
            case 0x05:
                return "NVOD时移业务";
            case 0x06:
                return "马赛克业务";
            case 0x07:
                return "PAL制编码信号";
            case 0x08:
                return "SECAM制编码信号";
            case 0x0A:
                return "调频广播业务";
            case 0x0B:
                return "NTSC制编码信号";
            case 0x0C:
                return "数据广播业务";
            case 0x10:
                return "MHP业务";
            default:
                return "用户定义类型业务";
        }
    }
}
