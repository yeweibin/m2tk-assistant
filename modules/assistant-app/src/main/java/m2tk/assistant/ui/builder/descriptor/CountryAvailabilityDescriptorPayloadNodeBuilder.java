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

package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Arrays;

public class CountryAvailabilityDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final int BLOCK_SIZE = 3;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int availabilityFlag = (payload.readUINT8(0) >> 7) & 0b1;
        int count = (payload.size() - 1) / BLOCK_SIZE;
        String[] countryCodes = new String[count];
        for (int i = 0; i < count; i++)
        {
            int code = payload.readUINT24(1 + i * BLOCK_SIZE);
            countryCodes[i] = String.format("'%s'", DVB.decodeThreeLetterCode(code));
        }

        node.add(create(String.format("country_availability_flag = %d", availabilityFlag)));
        node.add(create(String.format("country_code_list = %s", Arrays.toString(countryCodes))));
    }
}
