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
public class StreamSource
{
    private int id;
    private String name;
    private String uri;
    private int frameSize;
    private int transportStreamId;
    private int bitrate;
    private long packetCount;
    private int streamCount;
    private int programCount;
    private boolean scrambled;
    private boolean ecmPresent;
    private boolean emmPresent;
    private boolean patPresent;
    private boolean pmtPresent;
    private boolean catPresent;
    private boolean nitActualPresent;
    private boolean nitOtherPresent;
    private boolean sdtActualPresent;
    private boolean sdtOtherPresent;
    private boolean eitPnfActualPresent;
    private boolean eitPnfOtherPresent;
    private boolean eitSchActualPresent;
    private boolean eitSchOtherPresent;
    private boolean batPresent;
    private boolean tdtPresent;
    private boolean totPresent;
}
