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

import java.util.Objects;

@Getter
public class NVODService implements Comparable<NVODService>
{
    private final String id;
    private final String referenceId;
    private final int transportStreamId;
    private final int originalNetworkId;
    private final int serviceId;
    private final int referenceServiceId;
    private final boolean referenceService;
    private final boolean timeShiftedService;

    private NVODService(String id,
                        String referenceId,
                        int transportStreamId,
                        int originalNetworkId,
                        int serviceId,
                        int referenceServiceId,
                        boolean referenceService,
                        boolean timeShiftedService)
    {
        this.id = id;
        this.referenceId = referenceId;
        this.transportStreamId = transportStreamId;
        this.originalNetworkId = originalNetworkId;
        this.serviceId = serviceId;
        this.referenceServiceId = referenceServiceId;
        this.referenceService = referenceService;
        this.timeShiftedService = timeShiftedService;
    }

    public static NVODService ofReference(int transportStreamId, int originalNetworkId, int serviceId)
    {
        return new NVODService(String.format("nvod.ref.srv.%d.%d.%d", transportStreamId, originalNetworkId, serviceId),
                               null,
                               transportStreamId, originalNetworkId, serviceId,
                               -1,
                               true, false);
    }

    public static NVODService ofTimeShifted(int transportStreamId,
                                            int originalNetworkId,
                                            int serviceId,
                                            int referenceServiceId)
    {
        return new NVODService(String.format("nvod.shift.srv.%d.%d.%d", transportStreamId, originalNetworkId, serviceId),
                               String.format("nvod.ref.srv.%d.%d.%d", transportStreamId, originalNetworkId, referenceServiceId),
                               transportStreamId, originalNetworkId, serviceId,
                               referenceServiceId,
                               false, true);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NVODService that = (NVODService) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    public static boolean isSameReferenceService(NVODService service1, NVODService service2)
    {
        return service1.isReferenceService() &&
               service2.isReferenceService() &&
               service1.serviceId == service2.serviceId;
    }

    @Override
    public int compareTo(NVODService other)
    {
        return (other == null) ? 1 : Integer.compare(this.serviceId, other.serviceId);
    }
}
