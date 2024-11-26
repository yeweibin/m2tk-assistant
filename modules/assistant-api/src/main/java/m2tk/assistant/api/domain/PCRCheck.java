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
package m2tk.assistant.api.domain;

import lombok.Data;

@Data
public class PCRCheck
{
    private int pid;
    private long prevPosition;
    private long prevValue;
    private long currPosition;
    private long currValue;
    private long bitrate;
    private long intervalNanos;
    private long diffNanos;
    private long accuracyNanos;
    private boolean repetitionCheckFailed;
    private boolean discontinuityCheckFailed;
    private boolean accuracyCheckFailed;
}
