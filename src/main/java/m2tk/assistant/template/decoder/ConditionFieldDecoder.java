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

import m2tk.assistant.template.definition.Condition;
import m2tk.assistant.template.definition.ConditionalFieldDefinition;
import m2tk.assistant.template.definition.SyntaxFieldDefinition;
import m2tk.encoding.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

public class ConditionFieldDecoder implements SyntaxFieldDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(ConditionFieldDecoder.class);

    public int decode(SyntaxFieldDefinition definition,
                      Encoding encoding, int position, int bitOffset, int limit,
                      SyntaxField parent)
    {
        if (!(definition instanceof ConditionalFieldDefinition fieldDefinition))
            throw new IllegalArgumentException("无效的字段定义");
        if (encoding == null || encoding.size() == 0)
            throw new IllegalArgumentException("输入编码为空");
        if (position < 0 || position > encoding.size())
            throw new IllegalArgumentException("无效的起始位置：" + position);
        if (limit < position || limit > encoding.size())
            throw new IllegalArgumentException("无效的限制位置：" + limit);
        if (bitOffset < 0 || bitOffset > 7)
            throw new IllegalArgumentException("无效的位偏移量：" + bitOffset);

        List<SyntaxFieldDefinition> bodyFields = matches(fieldDefinition.getCondition(), parent)
                                                 ? fieldDefinition.getThenPart()
                                                 : fieldDefinition.getElsePart();

        int totalBits = 0;
        for (SyntaxFieldDefinition bodyField : bodyFields)
        {
            SyntaxFieldDecoder decoder = SyntaxFieldDecoder.of(bodyField);
            int decodedBits = decoder.decode(bodyField, encoding, position, bitOffset, limit, parent);

            totalBits += decodedBits;
            position = position + (bitOffset + decodedBits) / 8;
            bitOffset = (bitOffset + decodedBits) % 8;
        }

        return totalBits;
    }

    private boolean matches(Condition condition, SyntaxField parent)
    {
        return switch (condition.getType())
        {
            case "CompareWithConst" -> compareWithSingleValue(condition, parent);
            case "CompareWithConstMulti" -> compareWithMultipleValues(condition, parent);
            default -> false;
        };
    }

    private boolean compareWithSingleValue(Condition condition, SyntaxField parent)
    {
        SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, condition.getField());
        if (refNode == null || refNode.getType() != SyntaxField.Type.NUMBER)
        {
            logger.error("无法获取前置条件字段");
            throw new IllegalStateException("无法获取前置条件字段");
        }

        long fieldValue = refNode.getValueAsLong();
        int constValue = condition.getValue();

        return switch (condition.getOperation())
        {
            case "equals" -> fieldValue == constValue;
            case "not_equal" -> fieldValue != constValue;
            case "larger_than" -> fieldValue > constValue;
            case "smaller_than" -> fieldValue < constValue;
            default -> throw new IllegalArgumentException("无效的比较操作：" + condition.getOperation());
        };
    }

    private boolean compareWithMultipleValues(Condition condition, SyntaxField parent)
    {
        SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, condition.getField());
        if (refNode == null || refNode.getType() != SyntaxField.Type.NUMBER)
        {
            logger.error("无法获取前置条件字段");
            throw new IllegalStateException("无法获取前置条件字段");
        }

        long fieldValue = refNode.getValueAsLong();
        int[] constValues = condition.getValues();

        return switch (condition.getOperation())
        {
            case "equals_any" -> IntStream.of(constValues).anyMatch(c -> c == fieldValue);
            case "not_equal_all" -> IntStream.of(constValues).allMatch(c -> c != fieldValue);
            default -> throw new IllegalArgumentException("无效的比较操作：" + condition.getOperation());
        };
    }
}
