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

package m2tk.assistant.analyzer.domain;

import lombok.Getter;

@Getter
public class CASystemStream
{
    private final int systemId;
    private final int streamPid;
    private final String streamPrivateData;
    private final String streamDescription;

    public CASystemStream(int casid, int pid, String privateData, String description)
    {
        systemId = casid;
        streamPid = pid;
        streamPrivateData = privateData;
        streamDescription = description;
    }
}
