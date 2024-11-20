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
public class SIService
{
    private int id;

    private String name;
    private String provider;

    private int transportStreamId;
    private int originalNetworkId;
    private int serviceId;
    private int serviceType;
    private String serviceTypeName;
    private int runningStatus;
    private String runningStatusName;
    private boolean freeAccess;
    private boolean presentFollowingEITEnabled;
    private boolean scheduleEITEnabled;
    private boolean actualTransportStream;

    private int referenceServiceId;

    public boolean isNVODReferenceService()
    {
        return serviceType == 0x04;
    }

    public boolean isNVODTimeShiftedService()
    {
        return serviceType == 0x05;
    }

    public SIServiceLocator locator()
    {
        return new SIServiceLocator(originalNetworkId, transportStreamId, serviceId);
    }
}
