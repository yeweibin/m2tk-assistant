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

public final class TR290ErrorTypes
{
    private TR290ErrorTypes()
    {
    }

    public static final String TS_SYNC_LOSS = "同步丢失错误";
    public static final String SYNC_BYTE_ERROR = "同步字节错误";
    public static final String PAT_ERROR_2 = "PAT错误";
    public static final String CONTINUITY_COUNT_ERROR = "连续计数错误";
    public static final String PMT_ERROR_2 = "PMT错误";
    public static final String PID_ERROR = "PMT指定PID错误";

    public static final String TRANSPORT_ERROR = "传输错误";
    public static final String CRC_ERROR = "CRC错误";
    public static final String PCR_REPETITION_ERROR = "PCR间隔错误";
    public static final String PCR_DISCONTINUITY_INDICATOR_ERROR = "PCR不连续错误";
    public static final String PCR_ACCURACY_ERROR = "PCR精度错误";
    public static final String CAT_ERROR = "CAT错误";

    public static final String NIT_ACTUAL_ERROR = "NIT_actual错误";
    public static final String NIT_OTHER_ERROR = "NIT_other错误";
    public static final String SI_REPETITION_ERROR = "SI表间隔错误";
    public static final String UNREFERENCED_PID = "关联PID错误";
    public static final String SDT_ACTUAL_ERROR = "SDT_actual错误";
    public static final String SDT_OTHER_ERROR = "SDT_other错误";
    public static final String EIT_ACTUAL_ERROR = "EIT_actual错误";
    public static final String EIT_OTHER_ERROR = "EIT_other错误";
    public static final String RST_ERROR = "RST错误";
    public static final String TDT_ERROR = "TDT错误";
}
