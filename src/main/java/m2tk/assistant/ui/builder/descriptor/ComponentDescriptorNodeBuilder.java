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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.ComponentDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ComponentDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ComponentDescriptorDecoder decoder = new ComponentDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("component_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));
        node.add(create(String.format("stream_content = 0x%X", decoder.getStreamContentType())));
        node.add(create(String.format("component_type = 0x%X", decoder.getComponentType())));
        node.add(create(String.format("component_tag = 0x%02X", decoder.getComponentTag())));
        node.add(create(String.format("ISO_639_language_code = '%s'", decoder.getLanguageCode())));

        node.add(create(String.format("text = '%s'（原始数据：%s）",
                                      decoder.getText(),
                                      encoding.toHexStringPrettyPrint(8, encoding.size()))));

        return node;
    }
}
