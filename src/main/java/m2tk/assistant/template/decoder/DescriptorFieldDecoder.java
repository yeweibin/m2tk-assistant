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

package m2tk.assistant.template.decoder;

import m2tk.assistant.template.definition.DescriptorFieldDefinition;
import m2tk.assistant.template.definition.SyntaxFieldDefinition;
import m2tk.encoding.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescriptorFieldDecoder implements SyntaxFieldDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(DescriptorFieldDecoder.class);
    private static final DescriptorDecoder decoder = new DescriptorDecoder();

    @Override
    public int decode(SyntaxFieldDefinition definition,
                      Encoding encoding, int position, int bitOffset, int limit,
                      SyntaxField parent)
    {
        if (!(definition instanceof DescriptorFieldDefinition fieldDefinition))
            throw new IllegalArgumentException("无效的字段定义");
        if (encoding == null || encoding.size() == 0)
            throw new IllegalArgumentException("输入编码为空");
        if (position < 0 || position > encoding.size())
            throw new IllegalArgumentException("无效的起始位置：" + position);
        if (limit <= position || limit > encoding.size())
            throw new IllegalArgumentException("无效的限制位置：" + limit);
        if (bitOffset < 0 || bitOffset > 7)
            throw new IllegalArgumentException("无效的位偏移量：" + bitOffset);

        if (bitOffset != 0)
        {
            logger.error("字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的字段起始位置（未对齐）");
        }

        int tag = encoding.readUINT8(position);
        int len = encoding.readUINT8(position + 1);
        if (len + 2 > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, len + 2);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        SyntaxField node = decoder.decode(encoding, position, position + len + 2);
        if (parent != null)
            parent.appendChild(node);

        return (len + 2) * 8;
    }
}
