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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class DataBroadcastDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int dataBroadcastId = payload.readUINT16(0);
        int componentTag = payload.readUINT8(2);
        int selectorLen = payload.readUINT8(3);
        String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(4 + selectorLen));
        int descLen = payload.readUINT8(4 + selectorLen + 3);
        String desc = DVB.decodeString(payload.getRange(4 + selectorLen + 4, 4 + selectorLen + 4 + descLen));

        node.add(create(String.format("data_broadcast_id = 0x%04X", dataBroadcastId)));
        node.add(create(String.format("component_tag = 0x%02X", componentTag)));
        node.add(create(String.format("selector_length = %d", selectorLen)));
        node.add(create(String.format("selector = [%s]", payload.toHexStringPrettyPrint(4, 4 + selectorLen))));
        node.add(create(String.format("ISO_639_language_code = '%s'", langCode)));
        node.add(create(String.format("text_length = %d", descLen)));
        node.add(create(String.format("text = '%s'（原始数据：[%s]）",
                                      desc,
                                      payload.toHexStringPrettyPrint(4 + selectorLen + 4, 4 + selectorLen + 4 + descLen))));
    }
}
