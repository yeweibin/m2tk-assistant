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

package m2tk.assistant.app.ui.builder.section;

import m2tk.assistant.app.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.util.Bytes;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.nio.charset.Charset;

public class EBContentSectionNodeBuilder implements TreeNodeBuilder
{
    private final ExtendedSectionDecoder decoder = new ExtendedSectionDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        String text = String.format("Section[s:%02X, v:%d]", decoder.getSectionNumber(), decoder.getVersionNumber());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", decoder.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", decoder.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", decoder.getSectionLength())));
        node.add(create(String.format("table_id_extension = %d", decoder.getTableIDExtension())));
        node.add(create(String.format("version_number = %d", decoder.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", decoder.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", decoder.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", decoder.getLastSectionNumber())));

        Encoding payload = decoder.getPayload();
        String ebmId = payload.toHexString(0, 18).substring(1);
        node.add(create(String.format("EBM_id = %s", ebmId)));

        int offset = 18;
        int contentNumber = payload.readUINT8(offset) & 0xF;
        offset += 1;

        DefaultMutableTreeNode nodeContentList = new DefaultMutableTreeNode(String.format("multilingual_content_list（%d）", contentNumber));
        node.add(nodeContentList);

        for (int i = 0; i < contentNumber; i++)
        {
            int contentLength = (int) payload.readUINT32(offset);
            int contentFinish = offset + 4 + contentLength;
            offset += 4;

            DefaultMutableTreeNode nodeContent = new DefaultMutableTreeNode(String.format("multilingual_content_%d（%d字节）", i + 1, contentLength));

            nodeContent.add(create(String.format("language_code = '%s'", DVB.decodeThreeLetterCode(payload.readUINT24(offset)))));
            offset += 3;

            int charset = payload.readUINT8(offset) & 0b111;
            nodeContent.add(create(String.format("code_character_set = %d", charset)));
            offset += 1;

            int msgLen = payload.readUINT16(offset);
            byte[] msgBytes = payload.getRange(offset + 2, offset + 2 + msgLen);
            String msgText = decodeText(charset, msgBytes);
            nodeContent.add(create(String.format("message_text = '%s'（原始编码：[%s]）",
                                                 msgText, Bytes.toHexStringPrettyPrint(msgBytes))));
            offset += 2 + msgLen;

            int nameLen = payload.readUINT8(offset);
            byte[] nameBytes = payload.getRange(offset + 1, offset + 1 + nameLen);
            String agencyName = decodeText(charset, nameBytes);
            nodeContent.add(create(String.format("agency_name = '%s'（原始编码：[%s]）",
                                                 agencyName, Bytes.toHexStringPrettyPrint(nameBytes))));
            offset += 1 + nameLen;

            int auxiliaryDataNumber = payload.readUINT8(offset) & 0xF;
            offset += 1;
            DefaultMutableTreeNode auxiliaryDataList = new DefaultMutableTreeNode(String.format("auxiliary_data_list（%d）", auxiliaryDataNumber));
            nodeContent.add(auxiliaryDataList);

            for (int j = 0; j < auxiliaryDataNumber; j ++)
            {
                DefaultMutableTreeNode auxiliaryData = new DefaultMutableTreeNode(String.format("auxiliary_data_%d", j + 1));

                auxiliaryData.add(create(String.format("auxiliary_data_type = %d", payload.readUINT8(offset))));
                offset += 1;
                int dataLen = payload.readUINT24(offset);
                offset += 3;
                auxiliaryData.add(create(String.format("auxiliary_data = [%s]", payload.toHexStringPrettyPrint(offset, offset + dataLen))));
                offset += dataLen;

                auxiliaryDataList.add(auxiliaryData);
            }

            if (offset != contentFinish)
            {
                nodeContent.removeAllChildren();
                nodeContent.add(new DefaultMutableTreeNode("错误的Content编码"));
            }

            nodeContentList.add(nodeContent);
            offset = contentFinish;
        }

        int signatureLength = payload.readUINT16(offset);
        node.add(create(String.format("signature = '%s'", payload.toHexString(offset + 2, offset + 2 + signatureLength))));

        node.add(create(String.format("CRC_32 = 0x%08X", decoder.getChecksum())));

        return node;
    }

    private String decodeText(int charset, byte[] bytes)
    {
        switch (charset)
        {
            case 1:
                return new String(bytes, Charset.forName("GB18030"));
            case 2:
            case 3:
                return new String(bytes, Charset.forName("GBK"));
            default:
                return new String(bytes, Charset.forName("GB2312"));
        }
    }
}
