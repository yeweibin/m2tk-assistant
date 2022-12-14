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
import m2tk.mpeg2.decoder.section.PATSectionDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class PATNodeBuilder implements TreeNodeBuilder
{
    private final PATSectionDecoder pat = new PATSectionDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        pat.attach(encoding);

        String text = String.format("Section[%02X]（传输流号：%d）",
                                    pat.getSectionNumber(),
                                    pat.getTransportStreamID());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", pat.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", pat.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", pat.getSectionLength())));
        node.add(create(String.format("transport_stream_id = %d", pat.getTransportStreamID())));
        node.add(create(String.format("version_number = %d", pat.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", pat.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", pat.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", pat.getLastSectionNumber())));

        if (pat.containsNetworkInformationPID())
            node.add(create(String.format("NIT PID = 0x%X", pat.getNetworkInformationPID())));

        int[] prgnumList = pat.getProgramNumberList();
        int[] pmtpidList = pat.getProgramMapPIDList();
        DefaultMutableTreeNode nodePrgList = new DefaultMutableTreeNode(String.format("节目关联（包含%d路PMT）", prgnumList.length));
        for (int i = 0; i < prgnumList.length; i++)
        {
            text = String.format("节目%d（program_number = %d，PMT PID = 0x%X）",
                                        i + 1, prgnumList[i], pmtpidList[i]);
            nodePrgList.add(create(text));
        }
        node.add(nodePrgList);

        node.add(create(String.format("CRC_32 = 0x%08X", pat.getChecksum())));

        return node;
    }
}
