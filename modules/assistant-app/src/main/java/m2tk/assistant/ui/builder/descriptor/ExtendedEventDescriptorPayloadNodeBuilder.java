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

public class ExtendedEventDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int descriptorNumber = (payload.readUINT8(0) >> 4) & 0b1111;
        int lastDescriptorNumber = payload.readUINT8(0) & 0b1111;
        String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(1));

        node.add(create(String.format("descriptor_number = %d", descriptorNumber)));
        node.add(create(String.format("last_descriptor_number = %d", lastDescriptorNumber)));
        node.add(create(String.format("ISO_639_language_code = '%s'", langCode)));

        int limit = payload.readUINT8(4);
        int offset = 5;
        int i = 0;
        while (offset < limit)
        {
            i += 1;
            DefaultMutableTreeNode nodeItem = new DefaultMutableTreeNode("条目" + i);

            int descLen = payload.readUINT8(offset);
            String desc = DVB.decodeString(payload.getRange(offset + 1, offset + 1 + descLen));

            nodeItem.add(create(String.format("item_description_length = %d", descLen)));
            nodeItem.add(create(String.format("item_description = '%s'（原始数据：[%s]）",
                                              desc,
                                              payload.toHexStringPrettyPrint(offset + 1, offset + 1 + descLen))));
            offset += 1 + descLen;

            int itemLen = payload.readUINT8(offset);
            String item = DVB.decodeString(payload.getRange(offset + 1, offset + 1 + itemLen));

            nodeItem.add(create(String.format("item_length = %d", itemLen)));
            nodeItem.add(create(String.format("item = '%s'（原始数据：[%s]）",
                                              item,
                                              payload.toHexStringPrettyPrint(offset + 1, offset + 1 + itemLen))));
            offset += 1 + itemLen;

            node.add(nodeItem);
        }

        int textLen = payload.readUINT8(offset);
        String text = DVB.decodeString(payload.getRange(offset + 1, offset + 1 + textLen));
        node.add(create(String.format("text_length = %d", textLen)));
        node.add(create(String.format("text = '%s'（原始数据：[%s]）",
                                      text,
                                      payload.toHexStringPrettyPrint(offset + 1, offset + 1 + textLen))));
    }
}
