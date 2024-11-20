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

public class ComponentDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int streamContent = payload.readUINT8(0) & 0b1111;
        int componentType = payload.readUINT8(1);
        int componentTag = payload.readUINT8(2);
        String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(3));
        String text = DVB.decodeString(payload.getRange(6, payload.size()));

        node.add(create(String.format("stream_content = 0x%X", streamContent)));
        node.add(create(String.format("component_type = 0x%X", componentType)));
        node.add(create(String.format("component_tag = 0x%02X", componentTag)));
        node.add(create(String.format("ISO_639_language_code = '%s'", langCode)));
        node.add(create(String.format("text = '%s'（原始数据：[%s]）",
                                      text,
                                      payload.toHexStringPrettyPrint(6, payload.size()))));
    }
}
