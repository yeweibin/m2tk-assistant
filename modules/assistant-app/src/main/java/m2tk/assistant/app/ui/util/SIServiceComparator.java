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

package m2tk.assistant.app.ui.util;

import m2tk.assistant.api.domain.SIService;

import java.util.Comparator;

public class SIServiceComparator implements Comparator<SIService>
{
    @Override
    public int compare(SIService s1, SIService s2)
    {
        if (s1.getOriginalNetworkId() != s2.getOriginalNetworkId())
            return Integer.compare(s1.getOriginalNetworkId(), s2.getOriginalNetworkId());
        if (s1.getTransportStreamId() != s2.getTransportStreamId())
            return Integer.compare(s1.getTransportStreamId(), s2.getTransportStreamId());
        return Integer.compare(s1.getServiceId(), s2.getServiceId());
    }
}
