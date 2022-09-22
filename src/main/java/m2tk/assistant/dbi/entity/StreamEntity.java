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

package m2tk.assistant.dbi.entity;

import lombok.Data;
import m2tk.assistant.analyzer.presets.StreamTypes;

@Data
public class StreamEntity
{
    private long id;
    private long transactionId;
    private int pid;
    private boolean marked;
    private long packetCount;
    private int pcrCount;
    private int transportErrorCount;
    private int continuityErrorCount;
    private int bitrate;
    private double ratio;
    private boolean scrambled;
    private String category;
    private String description;

    public StreamEntity()
    {
        pid = 0x1FFF;
        marked = false;
        category = StreamTypes.CATEGORY_USER_PRIVATE;
        description = "空包";
    }
}
