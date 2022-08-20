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
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.descriptor.ISO639LanguageDescriptorDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ISO639LanguageDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ISO639LanguageDescriptorDecoder decoder = new ISO639LanguageDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("ISO_639_language_descriptor");
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        int count = decoder.getDescriptionCount();
        for (int i = 0; i < count; i++)
        {
            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode(String.format("描述%d", i + 1));
            nodeDesc.add(create(String.format("ISO_639_language_code = '%s'", decoder.getLanguageCode(i))));
            nodeDesc.add(create(String.format("audio_type = %s", translateAudioType(decoder.getAudioType(i)))));
            node.add(nodeDesc);
        }

        return node;
    }

    private String translateAudioType(int audioType)
    {
        switch (audioType)
        {
            case 0x00:
                return "未定义";
            case 0x01:
                return "无特效";
            case 0x02:
                return "针对听力障碍";
            case 0x03:
                return "针对视力障碍";
            default:
                return "预留";
        }
    }
}
