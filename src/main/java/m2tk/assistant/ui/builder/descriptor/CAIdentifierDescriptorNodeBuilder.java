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
import m2tk.dvb.decoder.descriptor.CAIdentifierDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class CAIdentifierDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final CAIdentifierDescriptorDecoder decoder = new CAIdentifierDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("CA_identifier_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        int count = decoder.getIdentifierCount();
        for (int i = 0; i < count; i++)
        {
            node.add(create(String.format("CA系统号%d = 0x%04X", i + 1, decoder.getCASystemID(i))));
        }

        return node;
    }
}
