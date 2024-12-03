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
package m2tk.assistant.app.ui.component;

import cn.hutool.core.collection.CollUtil;
import m2tk.assistant.api.domain.SIEvent;
import m2tk.assistant.api.domain.SIService;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

public class NVODServiceEventGuidePanel extends ServiceEventGuidePanel
{
    public NVODServiceEventGuidePanel()
    {
        super();
    }

    @Override
    protected void onServiceSelected(TreeSelectionEvent e)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) serviceTree.getLastSelectedPathComponent();
        if (node == null)
            return;

        Map<SIService, List<SIEvent>> registry = eventRegistry;
        if (node.getUserObject() instanceof String groupName)
        {
            // NVOD的事件描述（名称，简介……）全部集中于索引业务关联的EIT中。
            // 但具体事件的开始时间在对应的时移事件中。
            // NVOD索引业务会出现在每一个携带时移业务的TS中，所以会有不同的传输流号和原始网络号，但它们的
            // 业务号（索引业务号）都相同。也就是说，NVOD索引业务的业务号是全局唯一的。
            int refServiceId = Integer.parseInt(groupName.substring("索引业务：".length()));
            for (SIService service : registry.keySet())
            {
                if (service.getServiceId() == refServiceId)
                {
                    List<SIEvent> events = registry.get(service);
                    if (CollUtil.isNotEmpty(events))
                    {
                        eventTableModel.update(events);
                        return;
                    }
                }
            }
            eventTableModel.update(Collections.emptyList());
        }

        if (node.getUserObject() instanceof SIService service)
        {
            List<SIEvent> events = registry.getOrDefault(service, Collections.emptyList());
            eventTableModel.update(events);
        }
    }

    @Override
    protected Map<String, List<SIService>> groupServices(Collection<SIService> services)
    {
        Map<String, List<SIService>> groups = new TreeMap<>();
        for (SIService service : services)
        {
            if (service.isNVODReferenceService())
            {
                String groupName = String.format("索引业务：%d", service.getServiceId());
                groups.putIfAbsent(groupName, new ArrayList<>());
            }
            if (service.isNVODTimeShiftedService())
            {
                service.setName(String.format("时移业务：%d（传输流号：%d，原始网络号：%d）",
                                              service.getServiceId(),
                                              service.getTransportStreamId(),
                                              service.getOriginalNetworkId()));

                String groupName = String.format("索引业务：%d", service.getReferenceServiceId());
                groups.putIfAbsent(groupName, new ArrayList<>());
                List<SIService> group = groups.get(groupName);
                group.add(service);
                group.sort(Comparator.comparing(SIService::getOriginalNetworkId)
                                     .thenComparing(SIService::getTransportStreamId)
                                     .thenComparing(SIService::getServiceId));
            }
        }
        return groups;
    }
}
