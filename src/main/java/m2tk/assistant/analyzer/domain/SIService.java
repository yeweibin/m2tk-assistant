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
    private final String id;
    private final int transportStreamId;
    private final int originalNetworkId;
    private final int serviceId;
    private final String serviceType;
    private final String serviceName;
    private final String serviceProvider;

    public SIService(int transportStreamId, int originalNetworkId, int serviceId, String serviceType, String serviceName, String serviceProvider)
    {
        this.id = String.format("%05d.%05d.%05d",
                                originalNetworkId,
                                transportStreamId,
                                serviceId);

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
        return Objects.equals(id, siService.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
