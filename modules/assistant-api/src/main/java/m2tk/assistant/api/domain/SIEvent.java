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

import java.time.OffsetDateTime;
import java.util.Objects;

@Data
public class SIEvent
{
    private int id;

    private String title;
    private String description;
    private String languageCode;
    private OffsetDateTime startTime;
    private int duration;
    private int runningStatus;
    private boolean freeAccess;
    private boolean presentEvent;
    private boolean scheduleEvent;

    private int transportStreamId;
    private int originalNetworkId;
    private int serviceId;
    private int eventId;

    private int referenceServiceId;
    private int referenceEventId;
    private boolean nvodTimeShiftedEvent;

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) return false;
        SIEvent siEvent = (SIEvent) o;
        return id == siEvent.id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(id);
    }
}
