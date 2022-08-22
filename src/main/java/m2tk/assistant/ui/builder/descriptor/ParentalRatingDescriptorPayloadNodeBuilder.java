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

public class ParentalRatingDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    private static final int BLOCK_SIZE = 4;

    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / BLOCK_SIZE;
        for (int i = 0; i < count; i++)
        {
            String countryCode = DVB.decodeThreeLetterCode(payload.readUINT24(i * BLOCK_SIZE));
            int rating = payload.readUINT8(i * BLOCK_SIZE + 3);

            String text = String.format("成人级设置%d = （'%s'）%s",
                                        i + 1,
                                        countryCode,
                                        translateRatingCode(rating));
            node.add(create(text));
        }
    }

    private String translateRatingCode(int rating)
    {
        if (rating == 0x00)
            return "未定义";
        if (0x01 <= rating && rating <= 0x0F)
            return String.format("%d岁及以上观看", rating + 3);

        return String.format("运营商指定代码：%d", rating);
    }
}
