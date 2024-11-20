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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeOffsetDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final int BLOCK_SIZE = 13;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / BLOCK_SIZE;
        for (int i = 0; i < count; i++)
        {
            String countryCode = DVB.decodeThreeLetterCode(payload.readUINT24(i * BLOCK_SIZE));
            int countryRegionId = (payload.readUINT8(i * BLOCK_SIZE + 3) >> 2) & 0b111111;
            int localTimeOffsetPolarity = payload.readUINT8(i * BLOCK_SIZE + 3) & 0b1;
            int localTimeOffset = payload.readUINT16(i * BLOCK_SIZE + 4);
            long timeOfChange = payload.readUINT40(i * BLOCK_SIZE + 6);
            int nextTimeOffset = payload.readUINT16(i * BLOCK_SIZE + 11);

            DefaultMutableTreeNode nodeDesc = new DefaultMutableTreeNode(String.format("本地时间偏移描述%d", i + 1));
            nodeDesc.add(create(String.format("country_code = '%s'", countryCode)));
            nodeDesc.add(create(String.format("country_region_id = '%s'（%s）",
                                              sixBits(countryRegionId),
                                              translateCountryRegionID(countryRegionId))));
            nodeDesc.add(create(String.format("local_time_offset_polarity = %d", localTimeOffsetPolarity)));
            nodeDesc.add(create(String.format("local_time_offset = %04X", localTimeOffset)));
            nodeDesc.add(create(String.format("time_of_change = (UTC) %s", translateTimepoint2UTC(timeOfChange))));
            nodeDesc.add(create(String.format("next_time_offset = %04X", nextTimeOffset)));

            node.add(nodeDesc);
        }
    }

    private String sixBits(int value)
    {
        String bits = "000000";
        String binary= Integer.toBinaryString(value);
        return bits.substring(binary.length()) + binary;
    }

    private String translateCountryRegionID(int regionID)
    {
        if (regionID == 0b000000)
            return "未使用扩展时区";
        if (0b000001 <= regionID && regionID <= 0b111100)
            return "（东起）时区" + regionID;
        return "预留使用";
    }

    private String translateTimepoint2UTC(long timepoint)
    {
        return LocalDateTime.of(DVB.decodeDate((int) (timepoint >> 24)),
                                DVB.decodeTime((int) (timepoint & 0xFFFFFF)))
                            .format(timeFormatter);
    }
}
