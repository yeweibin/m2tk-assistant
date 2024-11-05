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
public class ElementaryStream
{
    private final int streamPid;
    private final long packetCount;
    private final int pcrCount;
    private final int continuityErrorCount;
    private final int bitrate;
    private final double ratio;
    private final boolean scrambled;
    private final int streamType;
    private final String category;
    private final String description;
    private final int associatedProgramNumber;

    public  ElementaryStream(int streamPid,
                            int streamType,
                            String category,
                            String description,
                            int associatedProgramNumber)
    {
        this(streamPid,
             0,
             0,
             0,
             0,
             0,
             false,
             streamType,
             category,
             description,
             associatedProgramNumber);
    }

    public ElementaryStream(int streamPid,
                            long packetCount,
                            int pcrCount,
                            int continuityErrorCount,
                            int bitrate,
                            double ratio,
                            boolean scrambled,
                            int streamType,
                            String category,
                            String description,
                            int associatedProgramNumber)
    {
        this.streamPid = streamPid;
        this.packetCount = packetCount;
        this.pcrCount = pcrCount;
        this.continuityErrorCount = continuityErrorCount;
        this.bitrate = bitrate;
        this.ratio = ratio;
        this.scrambled = scrambled;
        this.streamType = streamType;
        this.category = category;
        this.description = description;
        this.associatedProgramNumber = associatedProgramNumber;
    }

    public boolean isPresent()
    {
        return packetCount > 0;
    }

    public boolean containsPCR()
    {
        return pcrCount > 0;
    }
}
