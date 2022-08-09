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
public class SIService
{
    private final int transportStreamId;
    private final int originalNetworkId;
    private final int serviceId;
    private final String serviceType;
    private final String serviceName;
    private final String serviceProvider;

    public SIService(int transportStreamId, int originalNetworkId, int serviceId, String serviceType, String serviceName, String serviceProvider)
    {
        this.transportStreamId = transportStreamId;
        this.originalNetworkId = originalNetworkId;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.serviceProvider = serviceProvider;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SIService siService = (SIService) o;
        return transportStreamId == siService.transportStreamId &&
               originalNetworkId == siService.originalNetworkId &&
               serviceId == siService.serviceId &&
               Objects.equals(serviceType, siService.serviceType) &&
               Objects.equals(serviceName, siService.serviceName) &&
               Objects.equals(serviceProvider, siService.serviceProvider);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transportStreamId,
                            originalNetworkId,
                            serviceId,
                            serviceType,
                            serviceName,
                            serviceProvider);
    }
}
