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

import m2tk.assistant.template.definition.SyntaxFieldDefinition;
import m2tk.encoding.Encoding;

public interface SyntaxFieldDecoder
{
    static SyntaxFieldDecoder of(SyntaxFieldDefinition definition)
    {
        return switch (definition.type())
        {
            case "DataFieldDefinition" -> new DataFieldDecoder();
            case "ConditionalFieldDefinition" -> new ConditionFieldDecoder();
            case "LoopFieldDefinition" -> new LoopFieldDecoder();
            case "DescriptorFieldDefinition" -> new DescriptorFieldDecoder();
            default -> throw new IllegalArgumentException("无效的定义类型：type=" + definition.type());
        };
    }

    static SyntaxField findPrerequisiteField(SyntaxField parent, String field)
    {
        if (parent == null)
            return null;

        SyntaxField node = parent.findLastChild(field);
        return (node != null) ? node : parent.findUpstream(field);
    }

    /**
     * 按照字段定义进行解码；解码器根据实际情况将解码后的字段添加到父节点上。
     *
     * @param definition 字段定义
     * @param encoding 数据编码
     * @param position 起始位置
     * @param bitOffset 位偏移量
     * @param limit 字段边界
     * @param parent 父节点
     * @return 解码器移动的位长度（字段编码所占的位长度）
     */
    int decode(SyntaxFieldDefinition definition,
               Encoding encoding, int position, int bitOffset, int limit,
               SyntaxField parent);
}
