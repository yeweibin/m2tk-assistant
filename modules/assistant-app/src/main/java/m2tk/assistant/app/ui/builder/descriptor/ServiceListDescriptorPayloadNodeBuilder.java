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

package m2tk.assistant.app.ui.builder.descriptor;

import m2tk.assistant.api.presets.ServiceTypes;
import m2tk.assistant.app.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class ServiceListDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final int BLOCK_SIZE = 3;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / BLOCK_SIZE;
        for (int i = 0; i < count; i++)
        {
            int serviceId = payload.readUINT16(i * BLOCK_SIZE);
            int serviceType = payload.readUINT8(i * BLOCK_SIZE + 2);

            String text = String.format("业务说明%d：[业务号：%d，业务类型：（0x%02X）%s]",
                                        i + 1,
                                        serviceId,
                                        serviceType,
                                        ServiceTypes.name(serviceType));
            node.add(create(text));
        }
    }
}
