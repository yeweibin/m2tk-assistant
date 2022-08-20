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
import m2tk.dvb.decoder.descriptor.SubtitlingDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SubtitlingDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final SubtitlingDescriptorDecoder decoder = new SubtitlingDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("subtitling_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        int count = decoder.getSubtitlingDescriptionCount();
        for (int i = 0; i < count; i++)
        {
            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode(String.format("字幕描述%d", i + 1));
            nodeDesc.add(create(String.format("ISO_639_language_code = '%s'", decoder.getLanguageCode(i))));
            nodeDesc.add(create(String.format("subtitling_type = 0x%02X", decoder.getSubtitlingType(i))));
            nodeDesc.add(create(String.format("composition_page_id = 0x%04X", decoder.getCompositionPageID(i))));
            nodeDesc.add(create(String.format("ancillary_page_id = 0x%04X", decoder.getAncillaryPageID(i))));
            node.add(nodeDesc);
        }

        return node;
    }
}
