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

import m2tk.assistant.app.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class MultilingualServiceNameDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int offset = 0;
        int i = 0;
        while (offset < payload.size())
        {
            i += 1;
            String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(offset));
            int providerNameLen = payload.readUINT8(offset + 3);
            String providerName = DVB.decodeString(payload.getRange(offset + 4, offset + 4 + providerNameLen));
            int serviceNameLen = payload.readUINT8(offset + 4 + providerNameLen);
            String serviceName = DVB.decodeString(payload.getRange(offset + 4 + providerNameLen + 1,
                                                                   offset + 4 + providerNameLen + 1 + serviceNameLen));

            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode("业务描述" + i);
            nodeDesc.add(create(String.format("业务提供商 = （%s）'%s'（原始数据：[%s]）",
                                              langCode,
                                              providerName,
                                              payload.toHexStringPrettyPrint(offset + 4, offset + 4 + providerNameLen))));
            nodeDesc.add(create(String.format("业务名 = （%s）'%s'（原始数据：[%s]）",
                                              langCode,
                                              serviceName,
                                              payload.toHexStringPrettyPrint(offset + 4 + providerNameLen + 1,
                                                                             offset + 4 + providerNameLen + 1 + serviceNameLen))));

            node.add(nodeDesc);
            offset += 4 + providerNameLen + 1 + serviceNameLen;
        }
    }
}
