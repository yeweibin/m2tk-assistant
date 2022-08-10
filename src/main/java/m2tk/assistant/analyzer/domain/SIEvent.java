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

package m2tk.assistant.analyzer.domain;

import lombok.Getter;

import java.util.Objects;

@Getter
public class SIEvent
{
    private final String id;
    private final String parentId;
    private final int transportStreamId;
    private final int originalNetworkId;
    private final int serviceId;
    private final int eventId;
    private final String eventName;
    private final String eventDescription;
    private final String languageCode;
    private final String startTime;
    private final String duration;
    private final boolean scheduleEvent;
    private final boolean presentEvent;

    public SIEvent(int transportStreamId, int originalNetworkId, int serviceId, int eventId, String eventName, String eventDescription, String languageCode, String startTime, String duration, boolean isSchedule, boolean isPresent)
    {
        this.id = String.format("%05d.%05d.%05d.%05d",
                                originalNetworkId,
                                transportStreamId,
                                serviceId,
                                eventId);
        this.parentId = String.format("%05d.%05d.%05d",
                                originalNetworkId,
                                transportStreamId,
                                serviceId);

        this.transportStreamId = transportStreamId;
        this.originalNetworkId = originalNetworkId;
        this.serviceId = serviceId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.languageCode = languageCode;
        this.startTime = startTime;
        this.duration = duration;
        this.scheduleEvent = isSchedule;
        this.presentEvent = isPresent;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SIEvent siEvent = (SIEvent) o;
        return Objects.equals(id, siEvent.id) && Objects.equals(eventName, siEvent.eventName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, eventName);
    }
}
