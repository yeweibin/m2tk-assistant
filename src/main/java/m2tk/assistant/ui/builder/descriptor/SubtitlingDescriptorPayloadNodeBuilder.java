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

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class SubtitlingDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final int BLOCK_SIZE = 8;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / BLOCK_SIZE;
        for (int i = 0; i < count; i++)
        {
            String langCode = DVB.decodeThreeLetterCode(payload.readUINT24(i * BLOCK_SIZE));
            int subtitlingType = payload.readUINT8(i * BLOCK_SIZE + 3);
            int compositionPageId = payload.readUINT16(i * BLOCK_SIZE + 4);
            int ancillaryPageId = payload.readUINT16(i * BLOCK_SIZE + 6);

            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode(String.format("字幕描述%d", i + 1));
            nodeDesc.add(create(String.format("ISO_639_language_code = '%s'", langCode)));
            nodeDesc.add(create(String.format("subtitling_type = 0x%02X", subtitlingType)));
            nodeDesc.add(create(String.format("composition_page_id = 0x%04X", compositionPageId)));
            nodeDesc.add(create(String.format("ancillary_page_id = 0x%04X", ancillaryPageId)));
            node.add(nodeDesc);
        }
    }
}
