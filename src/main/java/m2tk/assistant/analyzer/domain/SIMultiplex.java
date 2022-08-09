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
public class SIMultiplex
{
    private final int transportStreamId;
    private final int originalNetworkId;
    private final String networkName;
    private final String deliverySystemType;
    private final String transmitFrequency;
    private final int serviceCount;

    public SIMultiplex(int transportStreamId, int originalNetworkId, String networkName, String deliverySystemType, String transmitFrequency, int serviceCount)
    {
        this.transportStreamId = transportStreamId;
        this.originalNetworkId = originalNetworkId;
        this.networkName = networkName;
        this.deliverySystemType = deliverySystemType;
        this.transmitFrequency = transmitFrequency;
        this.serviceCount = serviceCount;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SIMultiplex that = (SIMultiplex) o;
        return transportStreamId == that.transportStreamId &&
               originalNetworkId == that.originalNetworkId &&
               serviceCount == that.serviceCount &&
               Objects.equals(networkName, that.networkName) &&
               Objects.equals(deliverySystemType, that.deliverySystemType) &&
               Objects.equals(transmitFrequency, that.transmitFrequency);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transportStreamId,
                            originalNetworkId,
                            networkName,
                            deliverySystemType,
                            transmitFrequency,
                            serviceCount);
    }
}
