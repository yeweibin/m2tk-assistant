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

package m2tk.assistant.template;

import cn.hutool.core.util.StrUtil;
import m2tk.assistant.template.definition.*;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class SyntaxDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(SyntaxDecoder.class);

    private static final DescriptorDecoder DESCRIPTOR_DECODER = new DescriptorDecoder();

    /**
     * 按照字段定义进行解码；解码器根据实际情况将解码后的字段添加到父节点上。
     *
     * @param definition 字段定义
     * @param encoding   数据编码
     * @param position   起始位置
     * @param bitOffset  位偏移量
     * @param limit      字段边界
     * @param parent     父节点
     * @return 解码器移动的位长度（字段编码所占的位长度）
     */
    public int decode(SyntaxFieldDefinition definition,
                      Encoding encoding, int position, int bitOffset, int limit,
                      SyntaxField parent)
    {
        if (definition == null)
            throw new IllegalArgumentException("无效的字段定义");
        if (encoding == null || encoding.size() == 0)
            throw new IllegalArgumentException("输入编码为空");
        if (position < 0 || position > encoding.size())
            throw new IllegalArgumentException("无效的起始位置：" + position);
        if (limit < position || limit > encoding.size())
            throw new IllegalArgumentException("无效的限制位置：" + limit);
        if (bitOffset < 0 || bitOffset > 7)
            throw new IllegalArgumentException("无效的位偏移量：" + bitOffset);

        switch (definition)
        {
            case DataFieldDefinition field ->
            {
                return switch (field.getEncoding())
                {
                    case "bslbf" -> decodeBitstream(field, encoding, position, bitOffset, limit, parent);
                    case "uimsbf" -> decodeUnsignedInteger(field, encoding, position, bitOffset, limit, parent);
                    case "checksum" -> decodeChecksum(field, encoding, position, bitOffset, limit, parent);
                    case "nibbles" -> decodeNibbleArray(field, encoding, position, bitOffset, limit, parent);
                    case "octets" -> decodeOctetArray(field, encoding, position, bitOffset, limit, parent);
                    case "text" -> decodeText(field, encoding, position, bitOffset, limit, parent);
                    default -> throw new IllegalArgumentException("无效的字段格式");
                };
            }
            case ConditionalFieldDefinition field ->
            {
                int totalBits = 0;

                List<SyntaxFieldDefinition> bodyFields = getConditionBodyFields(field, parent);
                for (SyntaxFieldDefinition bodyField : bodyFields)
                {
                    int decodedBits = decode(bodyField, encoding, position, bitOffset, limit, parent);

                    totalBits += decodedBits;
                    position = position + (bitOffset + decodedBits) / 8;
                    bitOffset = (bitOffset + decodedBits) % 8;
                }

                return totalBits;
            }
            case LoopFieldDefinition field ->
            {
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
                    logger.error("Loop字段未对齐：bitOffset={}", bitOffset);
                    throw new IllegalStateException("错误的Loop字段起始位置（未对齐）");
                }

                String lengthType = StrUtil.emptyToDefault(field.getLengthType(), "length_in_bytes");
                return lengthType.equals("count")
                       ? decodeLoopInCount(field, encoding, position, bitOffset, limit, parent)
                       : decodeLoopInLength(field, encoding, position, bitOffset, limit, parent);
            }
            case DescriptorFieldDefinition ignored ->
            {
                if (bitOffset != 0)
                {
                    logger.error("Descriptor字段未对齐：bitOffset={}", bitOffset);
                    throw new IllegalStateException("错误的Descriptor字段起始位置（未对齐）");
                }

                int len = encoding.readUINT8(position + 1);
                if (len + 2 > limit)
                {
                    logger.error("Descriptor字段超限：start={}, limit={}, field_size={}", position, limit, len + 2);
                    throw new IndexOutOfBoundsException("Descriptor字段超出可解码范围");
                }

                SyntaxField field = DESCRIPTOR_DECODER.decode(encoding, position, position + len + 2);
                if (parent != null)
                    parent.appendChild(field);

                return (len + 2) * 8;
            }
            default ->
            {
            }
        }

        throw new IllegalArgumentException("无效的定义类型：type=" + definition.type());
    }

    private SyntaxField findPrerequisiteField(SyntaxField parent, String field)
    {
        if (parent == null)
            return null;

        SyntaxField node = parent.findLastChild(field);
        return (node != null) ? node : parent.findUpstream(field);
    }

    private int decodeBitstream(DataFieldDefinition definition, Encoding encoding,
                                int position, int bitOffset, int limit,
                                SyntaxField parent)
    {
        // bslbf字段无需对齐，可以从字节的任意位开始，任意长度（不超过64位）。
        // bslbf字段定义要求：
        //    length表示位长度，并且length必须大于0，小于等于64。
        //    bslbf字段为确定长度字段。

        int bitLength = Integer.parseUnsignedInt(definition.getLength());
        if (bitLength > 64)
        {
            logger.error("bslbf字段长度超过可解析范围：length={}", bitLength);
            throw new IllegalArgumentException("bslbf字段长度超过可解析范围");
        }

        int bits = bitOffset + bitLength;
        int bytes = bits / 8 + ((bits % 8 > 0) ? 1 : 0); // 算上偏移量后的全部位长度，大小应在 1~9 字节之间。
        int tailZeros = bytes * 8 - bits; // 末位距离所在字节末尾的位数，大小应在 0~7 之间。
        if (position + bytes > limit)
        {
            logger.error("bslbf字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("bslbf字段超出可解码范围");
        }

        long byte1 = encoding.readBits(position, 0xFF >>> bitOffset);
        long value = byte1 >>> tailZeros; // 当长度小于一字节时，需要去掉末尾多余的位。
        if (bytes > 1)
        {
            // 剩余位
            long mask = (0xFFFFFFFFFFFFFFFFL >>> (72 - bits)) << tailZeros;
            long bitX = encoding.readBits(position + 1, mask);

            value = (byte1 << (bits - 8)) | bitX;
        }

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.BITS, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.BITS, definition.getName(), value,
                                                  presentation.hasValueMappings() ? mapIntToString(value, presentation.getValueMappings()) : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private int decodeUnsignedInteger(DataFieldDefinition definition, Encoding encoding,
                                      int position, int bitOffset, int limit,
                                      SyntaxField parent)
    {
        // uimsbf采用BigEndian序编码，且向右对齐，即最低位与所在字节的末位对齐。
        // uimsbf字段定义要求：
        //    length表示位长度，并且length必须大于0。
        //    uimsbf字段为确定长度字段。

        int bitLength = Integer.parseUnsignedInt(definition.getLength());
        if (bitLength > 64)
        {
            logger.error("uimsbf字段长度超过可解析范围：length={}", bitLength);
            throw new IllegalArgumentException("uimsbf字段长度超过可解析范围");
        }

        int bits = bitOffset + bitLength;
        if (bits % 8 != 0)
        {
            logger.error("uimsbf字段没有向右对齐：start={}, bits={}, unaligned={}", bitOffset, bitLength, bits % 8);
            throw new IllegalStateException("uimsbf字段没有向右对齐");
        }

        int bytes = bits / 8; // 算上偏移量后的全部位长度，大小应在 1~8 字节之间。
        if (position + bytes > limit)
        {
            logger.error("uimsbf字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("uimsbf字段超出可解码范围");
        }

        // 由于字段是向右对齐的（且小于等于64位），所以这里可以直接计算数值。
        long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - bitLength);
        long value = encoding.readBits(position, mask);

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.NUMBER, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.NUMBER, definition.getName(), value,
                                                  presentation.hasValueMappings() ? mapIntToString(value, presentation.getValueMappings()) : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private int decodeChecksum(DataFieldDefinition definition, Encoding encoding,
                               int position, int bitOffset, int limit,
                               SyntaxField parent)
    {
        // checksum采用BigEndian序编码，且按字节对齐。
        // checksum字段定义要求：
        //   字段长度必须为8、16、32、64中的一种（常用CRC算法长度）。
        //   按字节向左对齐

        int bitLength = definition.getLengthValue();
        if (bitLength != 8 && bitLength != 16 && bitLength != 32 && bitLength != 64)
        {
            logger.error("checksum无效的字段长度：length={}", bitLength);
            throw new IllegalArgumentException("checksum字段长度无效");
        }

        if (bitOffset != 0)
        {
            logger.error("checksum字段没有向左对齐");
            throw new IllegalStateException("checksum字段没有向左对齐");
        }

        int bytes = bitLength / 8; // 算上偏移量后的全部位长度，大小应在 1~8 字节之间。
        if (position + bytes > limit)
        {
            logger.error("checksum字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("checksum字段超出可解码范围");
        }

        // 由于字段是按字节对齐的（且小于等于64位），所以这里可以直接计算数值。
        long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - bitLength);
        long value = encoding.readBits(position, mask);
        String mapping = switch (bytes)
        {
            case 1 -> String.format("%02x", value);
            case 2 -> String.format("%04x", value);
            case 4 -> String.format("%08x", value);
            case 8 -> String.format("%016x", value);
            default -> "";
        };

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.CHECKSUM, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.CHECKSUM, definition.getName(), value,
                                                  mapping,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private int decodeNibbleArray(DataFieldDefinition definition, Encoding encoding,
                                  int position, int bitOffset, int limit,
                                  SyntaxField parent)
    {
        // Nibble（半字节）定义：
        //    以4比特为单位划分；每个字节分为高低两个半区（首位对齐）。
        //    字段可以从任意字节的任意半区开始，到任意字节的任意半区结束。
        //    字段长度不限。
        // 注意：
        //    length表示nibble个数。
        //    如果length=“n/a”，则尝试读取length_field属性，如果length_field属性为“implicit”，则以
        //    limit作为字段末尾。

        if (bitOffset != 0 && bitOffset != 4)
        {
            logger.error("nibbles字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的nibbles字段起始位置（未对齐）");
        }

        int nibbleCount = definition.getLengthValue();
        if (definition.isIndirectLength())
        {
            if (!definition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定nibbles字段长度");
                throw new IllegalArgumentException("无法确定nibbles字段长度");
            }

            if (definition.isImplicitLength())
            {
                nibbleCount = limit - position - bitOffset / 4;
            } else
            {
                SyntaxField refNode = findPrerequisiteField(parent, definition.getLengthField());
                if (refNode == null)
                {
                    logger.error("找不到nibbles引用的长度字段");
                    throw new IllegalArgumentException("无法确定nibbles字段长度");
                }
                nibbleCount = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            nibbleCount += definition.getLengthCorrectionValue();
        }

        int bitLength = nibbleCount * 4;

        nibbleCount = nibbleCount + bitOffset / 4;
        int bytes = nibbleCount / 2 + nibbleCount % 2;
        if (position + bytes > limit)
        {
            logger.error("nibbles字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("nibbles字段超出可解码范围");
        }

        int[] nibbles = encoding.readNibbles(position, nibbleCount);
        int[] value = Arrays.copyOfRange(nibbles,
                                         (bitOffset == 0) ? 0 : 1,
                                         (nibbleCount % 2 == 0) ? nibbles.length : nibbles.length - 1);

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.NIBBLES, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.NIBBLES, definition.getName(), value,
                                                  null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private int decodeOctetArray(DataFieldDefinition definition, Encoding encoding,
                                 int position, int bitOffset, int limit,
                                 SyntaxField parent)
    {
        // Octet（字节）定义：
        //    每8比特为一个字节；字段长度不限。
        //    起始偏移量必须为零。
        //    字段长度不限。
        // 注意：
        //    length表示octet个数。
        //    如果length=“n/a”，则尝试读取length_field属性，如果length_field属性为“implicit”，则以
        //    limit作为字段末尾。

        if (bitOffset != 0)
        {
            logger.error("octets字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的octets字段起始位置（未对齐）");
        }

        int octetCount = definition.getLengthValue();
        if (definition.isIndirectLength())
        {
            if (!definition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定octets字段长度");
                throw new IllegalArgumentException("无法确定octets字段长度");
            }

            if (definition.isImplicitLength())
            {
                octetCount = limit - position;
            } else
            {
                SyntaxField refNode = findPrerequisiteField(parent, definition.getLengthField());
                if (refNode == null)
                {
                    logger.error("找不到octets引用的长度字段");
                    throw new IllegalArgumentException("无法确定octets字段长度");
                }
                octetCount = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            octetCount += definition.getLengthCorrectionValue();
        }

        int bitLength = octetCount * 8;

        int bytes = octetCount;
        if (position + bytes > limit)
        {
            logger.error("octets字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("octets字段超出可解码范围");
        }

        int[] value = encoding.readOctets(position, bytes);

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.OCTETS, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.OCTETS, definition.getName(), value,
                                                  null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private int decodeText(DataFieldDefinition definition, Encoding encoding,
                           int position, int bitOffset, int limit,
                           SyntaxField parent)
    {
        // 字符串支持以下编码方式：
        //    dvb_text：符合DVB SI规范的字符串编码
        //    utf16：以UTF-16方式编码的字符串
        //    utf8：以UTF-8方式编码的字符串
        //    ascii：以ASCII方式编码的字符串
        //    gb2312/gbk/gb18030：中文编码
        // 字符串字段定义要求：
        //    length表示字符串长度。
        //    如果length=0，则尝试读取length_field属性，如果length_field属性为“implicit”，则以
        //    limit作为字段末尾。

        if (bitOffset != 0)
        {
            logger.error("text字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的text字段起始位置（未对齐）");
        }

        String stringType = definition.getStringType();
        if (!StrUtil.equalsAny(stringType, "dvb_test", "utf16", "utf8", "ascii", "gb2312", "gbk", "gb18030"))
        {
            logger.error("无效的字符编码类型：{}", stringType);
            throw new IllegalArgumentException("无效的字符编码类型");
        }

        int bytes = definition.getLengthValue();
        if (definition.isIndirectLength())
        {
            if (!definition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定text字段长度");
                throw new IllegalArgumentException("无法确定text字段长度");
            }

            if (definition.isImplicitLength())
            {
                bytes = limit - position;
            } else
            {
                SyntaxField refNode = findPrerequisiteField(parent, definition.getLengthField());
                if (refNode == null)
                {
                    logger.error("找不到text引用的长度字段");
                    throw new IllegalArgumentException("找不到text引用的长度字段");
                }
                bytes = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            bytes += definition.getLengthCorrectionValue();
        }

        if (position + bytes > limit)
        {
            logger.error("text字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("text字段超出可解码范围");
        }

        int bitLength = bytes * 8;
        String value = switch (stringType)
        {
            case "dvb_text" -> DVB.decodeString(encoding.getRange(position, position + bytes));
            case "utf16" -> new String(encoding.getRange(position, position + bytes), StandardCharsets.UTF_16);
            case "utf8" -> new String(encoding.getRange(position, position + bytes), StandardCharsets.UTF_8);
            case "ascii" -> new String(encoding.getRange(position, position + bytes), StandardCharsets.US_ASCII);
            case "gb2312" -> new String(encoding.getRange(position, position + bytes), Charset.forName("GB2312"));
            case "gbk" -> new String(encoding.getRange(position, position + bytes), Charset.forName("GBK"));
            case "gb18030" -> new String(encoding.getRange(position, position + bytes), Charset.forName("GB18030"));
            default -> "原始字节：" + encoding.toHexStringPrettyPrint(position, position + bytes);
        };

        FieldPresentation presentation = definition.getPresentation();
        SyntaxField field = (presentation == null)
                            ? SyntaxField.invisible(SyntaxField.Type.TEXT, definition.getName(), value)
                            : SyntaxField.visible(SyntaxField.Type.TEXT, definition.getName(), value,
                                                  null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getText() : null,
                                                  presentation.hasPrefix() ? presentation.getPrefix().getColor() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getText() : null,
                                                  presentation.hasFormat() ? presentation.getFormat().getColor() : null,
                                                  presentation.hasFormat() && presentation.getFormat().isBold());

        if (parent != null)
            parent.appendChild(field);

        return bitLength;
    }

    private String mapIntToString(long value, List<ValueMapping> valueMappings)
    {
        return valueMappings.stream()
                            .filter(mapping -> mapping.eval(value))
                            .map(ValueMapping::mapping)
                            .findFirst()
                            .orElse(Long.toString(value));
    }

    private List<SyntaxFieldDefinition> getConditionBodyFields(ConditionalFieldDefinition definition, SyntaxField parent)
    {
        Condition condition = definition.getCondition();

        SyntaxField refNode = findPrerequisiteField(parent, condition.getField());
        if (refNode == null)
        {
            logger.error("无法获取条件引用字段");
            throw new IllegalStateException("无法获取条件引用字段");
        }

        boolean matches = false;
        String compareType = condition.getType();

        if ("CompareWithConst".equals(compareType))
        {
            long fieldValue = refNode.getValueAsLong();
            int constValue = condition.getValue();

            matches = switch (condition.getOperation())
            {
                case "equals" -> fieldValue == constValue;
                case "not_equal" -> fieldValue != constValue;
                case "larger_than" -> fieldValue > constValue;
                case "smaller_than" -> fieldValue < constValue;
                default -> throw new IllegalArgumentException("无效的比较操作：" + condition.getOperation());
            };
        }

        if ("CompareWithConstMulti".equals(compareType))
        {
            long fieldValue = refNode.getValueAsLong();
            int[] constValues = condition.getValues();

            matches = switch (condition.getOperation())
            {
                case "equals_any" -> IntStream.of(constValues).anyMatch(c -> c == fieldValue);
                case "not_equal_all" -> IntStream.of(constValues).allMatch(c -> c != fieldValue);
                default -> throw new IllegalArgumentException("无效的比较操作：" + condition.getOperation());
            };
        }

        return matches ? definition.getThenPart() : definition.getElsePart();
    }

    private int decodeLoopInCount(LoopFieldDefinition definition,
                                  Encoding encoding, int position, int bitOffset, int limit,
                                  SyntaxField parent)
    {
        LoopPresentation presentation = definition.getPresentation();
        if (presentation == null)
        {
            // 默认循环展示方式
            presentation = new LoopPresentation();
            presentation.setLoopHeader(Label.plain(definition.getName()));
            presentation.setLoopEmpty(Label.plain("空循环"));
        }

        SyntaxField loopRoot;
        if (presentation.isNoLoopHeader())
            loopRoot = parent;
        else
        {
            loopRoot = SyntaxField.loopHeader(definition.getName(),
                                              presentation.getLoopHeader().getText());
            parent.appendChild(loopRoot);
        }

        SyntaxField refNode = findPrerequisiteField(parent, definition.getLengthField());
        if (refNode == null)
        {
            logger.error("无法获取循环次数");
            throw new IllegalStateException("无法获取循环次数");
        }

        int count = Math.toIntExact(refNode.getValueAsLong());
        if (count == 0)
        {
            parent.removeChild(loopRoot);
            SyntaxField loopEntry = SyntaxField.loopEntryHeader("loop_entry", presentation.getLoopEmpty().getText());
            parent.appendChild(loopEntry);
            return 0;
        }

        LoopEntryPresentation entryPresentation = presentation.getLoopEntryPresentation();
        Label fixedEntryLabel = (entryPresentation == null) ? null : entryPresentation.getFixed();
        Label indexEntryLabel = (entryPresentation == null) ? null : entryPresentation.getPrefix();

        int totalBits = 0;
        boolean isDescriptorLoop = (definition.getBody().getFirst() instanceof DescriptorFieldDefinition);
        if (isDescriptorLoop)
        {
            for (int i = 0; i < count; i++)
            {
                int decodedBits = decode(DescriptorFieldDefinition.INSTANCE,
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
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', fixedEntryLabel.getText());
                else if (indexEntryLabel != null)
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', indexEntryLabel.getText() + (i + 1));
                else
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', "循环体" + (i + 1));

                loopRoot.appendChild(entryRoot);

                for (SyntaxFieldDefinition entryFieldDefinition : definition.getBody())
                {
                    int decodedBits = decode(entryFieldDefinition, encoding, position, bitOffset, limit, entryRoot);

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
            presentation = new LoopPresentation();
            presentation.setLoopHeader(Label.plain(definition.getName()));
            presentation.setLoopEmpty(Label.plain("空循环"));
        }

        SyntaxField loopRoot;
        if (presentation.isNoLoopHeader())
            loopRoot = parent;
        else
        {
            loopRoot = SyntaxField.loopHeader(definition.getName(),
                                              presentation.getLoopHeader().getText());
            parent.appendChild(loopRoot);
        }

        // 默认边界为最大可解码范围
        int byteLength = limit - position;

        if (!definition.isImplicitLength())
        {
            // 指明了具体的字段长度字段
            SyntaxField refNode = findPrerequisiteField(parent, definition.getLengthField());
            if (refNode == null)
            {
                logger.error("无法获取循环长度");
                throw new IllegalStateException("无法获取循环长度");
            }
            byteLength = Math.toIntExact(refNode.getValueAsLong());
        }

        byteLength += definition.getLengthCorrectionValue();

        if (byteLength == 0)
        {
            parent.removeChild(loopRoot);
            SyntaxField loopEntry = SyntaxField.loopEntryHeader("loop_entry", presentation.getLoopEmpty().getText());
            parent.appendChild(loopEntry);
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
            while (position < finish)
            {
                int decodedBits = decode(DescriptorFieldDefinition.INSTANCE,
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
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', fixedEntryLabel.getText());
                else if (indexEntryLabel != null)
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', indexEntryLabel.getText() + (i + 1));
                else
                    entryRoot = SyntaxField.loopEntryHeader("loop_entry[" + i + ']', "循环体" + (i + 1));

                loopRoot.appendChild(entryRoot);
                i++;

                for (SyntaxFieldDefinition entryFieldDefinition : definition.getBody())
                {
                    int decodedBits = decode(entryFieldDefinition, encoding, position, bitOffset, limit, entryRoot);

                    totalBits += decodedBits;
                    position = position + (bitOffset + decodedBits) / 8;
                    bitOffset = (bitOffset + decodedBits) % 8;
                }
            }
        }

        return totalBits;
    }
}
