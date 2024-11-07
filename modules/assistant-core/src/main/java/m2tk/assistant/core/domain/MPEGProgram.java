/*
 * Copyright (c) M2TK Project. All rights reserved.
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

package m2tk.assistant.core.domain;

import lombok.Data;

import java.util.List;

@Data
public class MPEGProgram
{
    private long ref;
    private long transactionId;

    private String name;
    private boolean scrambled;
    private boolean playable;
    private int programNumber;
    private int transportStreamId;
    private int bandwidth;
    private int pmtPid;
    private int pcrPid;
    private int pmtVersion;
    private List<CASystemStream> ecmStreams;
    private List<ElementaryStream> elementaryStreams;
}
