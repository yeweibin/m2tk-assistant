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

package m2tk.assistant.core.util;

import m2tk.assistant.core.domain.SIEvent;

import java.time.OffsetDateTime;
import java.util.Comparator;

public class SIEventComparator implements Comparator<SIEvent>
{
    @Override
    public int compare(SIEvent e1, SIEvent e2)
    {
        if (e1.getOriginalNetworkId() != e2.getOriginalNetworkId())
            return Integer.compare(e1.getOriginalNetworkId(), e2.getOriginalNetworkId());
        if (e1.getTransportStreamId() != e2.getTransportStreamId())
            return Integer.compare(e1.getTransportStreamId(), e2.getTransportStreamId());
        if (e1.getServiceId() != e2.getServiceId())
            return Integer.compare(e1.getServiceId(), e2.getServiceId());

        return Comparator.<OffsetDateTime>naturalOrder()
                         .compare(e1.getStartTime(), e2.getStartTime());
    }
}
