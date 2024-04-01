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

import cn.hutool.core.util.StrUtil;
import m2tk.assistant.template.definition.*;
import m2tk.encoding.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopFieldDecoder implements SyntaxFieldDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(LoopFieldDecoder.class);

    @Override
    public int decode(SyntaxFieldDefinition definition,
                      Encoding encoding, int position, int bitOffset, int limit,
                      SyntaxField parent)
    {
        if (!(definition instanceof LoopFieldDefinition fieldDefinition))
            throw new IllegalArgumentException("无效的字段定义");
        if (fieldDefinition.getBody() == null || fieldDefinition.getBody().isEmpty())
            throw new IllegalArgumentException("无效的字段定义");
        if (encoding == null || encoding.size() == 0)
            throw new IllegalArgumentException("输入编码为空");
        if (position < 0 || position > encoding.size())
            throw new IllegalArgumentException("无效的起始位置：" + position);
        if (limit < position || limit > encoding.size())
            throw new IllegalArgumentException("无效的限制位置：" + limit);
        if (bitOffset < 0 || bitOffset > 7)
            throw new IllegalArgumentException("无效的位偏移量：" + bitOffset);

        // Loop解码规则
        //    length_type：表明当前循环的length字段的值类型：字节长度（length_in_bytes）或循环次数（count）。
        //                 如果length_type未定义（空值），则按字节长度处理。
        //    length_field：提供字段长度的字段名。如果length_field="implicit"，则假设当前循环到limit位置结束。
        //    循环体可以为空，空循环的返回内容需根据loop_presentation定义确定。
        //    loop_presentation：循环头标签（LoopHeader）仅在非空循环时且存在LoopHeader定义时使用。
        //                 循环体标签（LoopEntryHeader），由固定标签（Fixed）或字段标签（EntryField）决定。
        //                  EntryField是LoopBody中（可能的多个Field中的）某个Field，由field_name标识。
        //                  如果定义了LoopEntry，则需要为循环体外包一层Node；没有则不用外包。
        //                 对于描述符循环，描述符的显示由描述符模板确定，建议不要定义LoopEntry，但这里不做强制约束。

        if (bitOffset != 0)
        {
            logger.error("字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的字段起始位置（未对齐）");
        }

        String lengthType = StrUtil.emptyToDefault(fieldDefinition.getLengthType(), "length_in_bytes");
        return lengthType.equals("count")
               ? decodeLoopInCount(fieldDefinition, encoding, position, bitOffset, limit, parent)
               : decodeLoopInLength(fieldDefinition, encoding, position, bitOffset, limit, parent);
    }

    private int decodeLoopInCount(LoopFieldDefinition definition,
                                  Encoding encoding, int position, int bitOffset, int limit,
                                  SyntaxField parent)
    {
        LoopPresentation presentation = definition.getPresentation();
        if (presentation == null)
        {
            // 默认循环展示方式
            presentation = LoopPresentation.forEmptyLoop("空循环");
        }

        SyntaxField loopRoot = SyntaxField.loopHeader(definition.getName(),
                                                      presentation.isNoLoopHeader() ? definition.getName()
                                                                                    : presentation.getLoopHeader().getText());
        if (parent != null)
            parent.appendChild(loopRoot);

        SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, definition.getLengthField());
        if (refNode == null || refNode.getType() != SyntaxField.Type.NUMBER)
        {
            logger.error("无法获取前置条件字段");
            throw new IllegalStateException("无法获取前置条件字段");
        }

        int count = Math.toIntExact(refNode.getValueAsLong());
        if (count == 0)
        {
            SyntaxField loopEntry = SyntaxField.loopEntry("loop_entry", presentation.getLoopEmpty().getText());
            loopRoot.appendChild(loopEntry);
            return 0;
        }

        LoopEntryPresentation entryPresentation = presentation.getLoopEntryPresentation();
        Label fixedEntryLabel = (entryPresentation == null) ? null : entryPresentation.getFixed();
        Label indexEntryLabel = (entryPresentation == null) ? null : entryPresentation.getPrefix();

        int totalBits = 0;
        boolean isDescriptorLoop = (definition.getBody().getFirst() instanceof DescriptorFieldDefinition);
        if (isDescriptorLoop)
        {
            DescriptorFieldDecoder decoder = new DescriptorFieldDecoder();
            for (int i = 0; i < count; i++)
            {
                int decodedBits = decoder.decode(DescriptorFieldDefinition.INSTANCE,
                                                 encoding, position, bitOffset, limit, loopRoot);

                totalBits += decodedBits;
                position = position + (bitOffset + decodedBits) / 8;
                bitOffset = (bitOffset + decodedBits) % 8;
            }
        } else
        {
            for (int i = 0; i < count; i++)
            {
                SyntaxField entryRoot;
                if (fixedEntryLabel != null)
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', fixedEntryLabel.getText());
                else if (indexEntryLabel != null)
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', indexEntryLabel.getText() + (i + 1));
                else
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', "循环体" + (i + 1));

                loopRoot.appendChild(entryRoot);

                for (SyntaxFieldDefinition entryFieldDefinition : definition.getBody())
                {
                    SyntaxFieldDecoder decoder = SyntaxFieldDecoder.of(entryFieldDefinition);
                    int decodedBits = decoder.decode(entryFieldDefinition, encoding, position, bitOffset, limit, entryRoot);

                    totalBits += decodedBits;
                    position = position + (bitOffset + decodedBits) / 8;
                    bitOffset = (bitOffset + decodedBits) % 8;
                }
            }
        }

        return totalBits;
    }

    private int decodeLoopInLength(LoopFieldDefinition definition,
                                   Encoding encoding, int position, int bitOffset, int limit,
                                   SyntaxField parent)
    {
        LoopPresentation presentation = definition.getPresentation();
        if (presentation == null)
        {
            // 默认循环展示方式
            presentation = LoopPresentation.forEmptyLoop("空循环");
        }

        SyntaxField loopRoot = SyntaxField.loopHeader(definition.getName(),
                                                      presentation.isNoLoopHeader() ? definition.getName()
                                                                                    : presentation.getLoopHeader().getText());
        if (parent != null)
            parent.appendChild(loopRoot);

        // 默认边界为最大可解码范围
        int byteLength = limit - position;

        if (!definition.isImplicitLength())
        {
            // 指明了具体的字段长度字段
            SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, definition.getLengthField());
            if (refNode == null || refNode.getType() != SyntaxField.Type.NUMBER)
            {
                logger.error("无法获取前置条件字段");
                throw new IllegalStateException("无法获取前置条件字段");
            }
            byteLength = Math.toIntExact(refNode.getValueAsLong());
        }

        byteLength += definition.getLengthCorrectionValue();

        if (byteLength == 0)
        {
            SyntaxField loopEntry = SyntaxField.loopEntry("loop_entry", presentation.getLoopEmpty().getText());
            loopRoot.appendChild(loopEntry);
            return 0;
        }

        LoopEntryPresentation entryPresentation = presentation.getLoopEntryPresentation();
        Label fixedEntryLabel = (entryPresentation == null) ? null : entryPresentation.getFixed();
        Label indexEntryLabel = (entryPresentation == null) ? null : entryPresentation.getPrefix();

        int totalBits = 0;
        int finish = position + byteLength;
        int i = 0;

        boolean isDescriptorLoop = (definition.getBody().getFirst() instanceof DescriptorFieldDefinition);
        if (isDescriptorLoop)
        {
            DescriptorFieldDecoder decoder = new DescriptorFieldDecoder();
            while (position < finish)
            {
                int decodedBits = decoder.decode(DescriptorFieldDefinition.INSTANCE,
                                                 encoding, position, bitOffset, limit, loopRoot);

                totalBits += decodedBits;
                position = position + (bitOffset + decodedBits) / 8;
                bitOffset = (bitOffset + decodedBits) % 8;
            }
        } else
        {
            while (position < finish)
            {
                SyntaxField entryRoot;
                if (fixedEntryLabel != null)
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', fixedEntryLabel.getText());
                else if (indexEntryLabel != null)
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', indexEntryLabel.getText() + (i + 1));
                else
                    entryRoot = SyntaxField.loopEntry("loop_entry[" + i + ']', "循环体" + (i + 1));

                loopRoot.appendChild(entryRoot);
                i++;

                for (SyntaxFieldDefinition entryFieldDefinition : definition.getBody())
                {
                    SyntaxFieldDecoder decoder = SyntaxFieldDecoder.of(entryFieldDefinition);
                    int decodedBits = decoder.decode(entryFieldDefinition, encoding, position, bitOffset, limit, entryRoot);

                    totalBits += decodedBits;
                    position = position + (bitOffset + decodedBits) / 8;
                    bitOffset = (bitOffset + decodedBits) % 8;
                }
            }
        }

        return totalBits;
    }
}
