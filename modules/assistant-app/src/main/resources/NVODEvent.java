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

import lombok.Getter;

import java.util.Objects;

@Getter
public class NVODEvent
{
    private final String id;
    private final String parentId;
    private final String referenceId;
    private final int transportStreamId;
    private final int originalNetworkId;
    private final int serviceId;
    private final int eventId;
    private final int referenceServiceId;
    private final int referenceEventId;
    private final String eventName;
    private final String eventDescription;
    private final String languageCode;
    private final String startTime;
    private final String duration;
    private final boolean presentEvent;
    private final boolean referenceEvent;
    private final boolean timeShiftedEvent;

    private NVODEvent(String id, String parentId, String referenceId,
                      int transportStreamId, int originalNetworkId, int serviceId, int eventId,
                      int referenceServiceId, int referenceEventId,
                      String eventName, String eventDescription, String languageCode,
                      String startTime, String duration,
                      boolean isPresent,
                      boolean isReferenceEvent,
                      boolean isTimeShiftedEvent)
    {
        this.id = id;
        this.parentId = parentId;
        this.referenceId = referenceId;
        this.transportStreamId = transportStreamId;
        this.originalNetworkId = originalNetworkId;
        this.serviceId = serviceId;
        this.eventId = eventId;
        this.referenceServiceId = referenceServiceId;
        this.referenceEventId = referenceEventId;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.languageCode = languageCode;
        this.startTime = startTime;
        this.duration = duration;
        this.presentEvent = isPresent;
        this.referenceEvent = isReferenceEvent;
        this.timeShiftedEvent = isTimeShiftedEvent;
    }

    public static NVODEvent ofReference(int transportStreamId,
                                        int originalNetworkId,
                                        int serviceId,
                                        int eventId,
                                        String eventName, String eventDescription, String languageCode,
                                        String startTime, String duration)
    {
        return new NVODEvent(String.format("nvod.ref.evt.%d.%d.%d.%d", transportStreamId, originalNetworkId, serviceId, eventId),
                             String.format("nvod.ref.srv.%d.%d.%d", transportStreamId, originalNetworkId, serviceId),
                             null,
                             transportStreamId, originalNetworkId, serviceId, eventId,
                             -1, -1,
                             eventName, eventDescription, languageCode,
                             startTime, duration, false,
                             true, false);
    }

    public static NVODEvent ofTimeShifted(int transportStreamId,
                                          int originalNetworkId,
                                          int serviceId,
                                          int eventId,
                                          int referenceServiceId,
                                          int referenceEventId,
                                          String eventName, String eventDescription, String languageCode,
                                          String startTime, String duration, boolean isPresent)
    {
        return new NVODEvent(String.format("nvod.shift.evt.%d.%d.%d.%d", transportStreamId, originalNetworkId, serviceId, eventId),
                             String.format("nvod.shift.srv.%d.%d.%d", transportStreamId, originalNetworkId, serviceId),
                             String.format("nvod.ref.evt.%d.%d.%d.%d", transportStreamId, originalNetworkId, referenceServiceId, referenceEventId),
                             transportStreamId, originalNetworkId, serviceId, eventId,
                             referenceServiceId, referenceEventId,
                             eventName, eventDescription, languageCode,
                             startTime, duration, isPresent,
                             false, true);
    }

    public static String referenceId(int transportStreamId, int originalNetworkId, int serviceId, int eventId)
    {
        return String.format("nvod.ref.evt.%d.%d.%d.%d",
                             transportStreamId, originalNetworkId, serviceId, eventId);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NVODEvent siEvent = (NVODEvent) o;
        return Objects.equals(id, siEvent.id) && Objects.equals(eventName, siEvent.eventName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, eventName);
    }
}
