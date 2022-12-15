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

package m2tk.assistant.ui.builder.section;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.ExtendedSectionDecoder;
import m2tk.util.Bytes;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class EBCertAuthSectionNodeBuilder implements TreeNodeBuilder
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
        int offset = 0;

        int certAuthNumber = payload.readUINT8(offset);
        offset += 1;

        DefaultMutableTreeNode nodeCertAuthList = new DefaultMutableTreeNode(String.format("证书授权列表（%d）", certAuthNumber));
        for (int i = 0; i < certAuthNumber; i ++)
        {
            int dataLen = payload.readUINT16(offset);
            byte[] certAuth = payload.getRange(offset + 2, offset + 2 + dataLen);
            nodeCertAuthList.add(create(String.format("cert_auth_%d = [%s]", i + 1, Bytes.toHexStringPrettyPrint(certAuth))));
            offset += 2 + dataLen;
        }
        node.add(nodeCertAuthList);

        int certNumber = payload.readUINT8(offset);
        offset += 1;

        DefaultMutableTreeNode nodeCertList = new DefaultMutableTreeNode(String.format("证书列表（%d）", certAuthNumber));
        for (int i = 0; i < certAuthNumber; i ++)
        {
            int dataLen = payload.readUINT8(offset);
            byte[] cert = payload.getRange(offset + 1, offset + 1 + dataLen);
            nodeCertList.add(create(String.format("cert_%d = [%s]", i + 1, Bytes.toHexStringPrettyPrint(cert))));
            offset += 1 + dataLen;
        }
        node.add(nodeCertList);

        int signatureLength = payload.readUINT16(offset);
        node.add(create(String.format("signature = '%s'", payload.toHexString(offset + 2, offset + 2 + signatureLength))));

        node.add(create(String.format("CRC_32 = 0x%08X", decoder.getChecksum())));

        return node;
    }
}
