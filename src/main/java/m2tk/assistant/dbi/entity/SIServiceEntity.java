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

package m2tk.assistant.dbi.entity;

import lombok.Data;

@Data
public class SIServiceEntity
{
    private long id;
    private long transactionId;
    private int transportStreamId;
    private int originalNetworkId;
    private int serviceId;
    private int referenceServiceId;
    private int serviceType;
    private String serviceTypeName;
    private String serviceName;
    private String serviceProvider;
    private String runningStatus;
    private boolean presentFollowingEITEnabled;
    private boolean scheduleEITEnabled;
    private boolean freeCAMode;
    private boolean actualTransportStream;
    private boolean nvodReferenceService;
    private boolean nvodTimeShiftedService;
}
