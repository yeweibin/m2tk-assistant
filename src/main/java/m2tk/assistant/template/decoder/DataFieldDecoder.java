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
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class DataFieldDecoder implements SyntaxFieldDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(DataFieldDecoder.class);

    private int lastDecodedBits;
    private DataFieldDefinition fieldDefinition;

    @Override
    public int decode(SyntaxFieldDefinition definition,
                      Encoding encoding, int position, int bitOffset, int limit,
                      SyntaxField parent)
    {
        if (!(definition instanceof DataFieldDefinition))
            throw new IllegalArgumentException("无效的字段定义");
        if (encoding == null || encoding.size() == 0)
            throw new IllegalArgumentException("输入编码为空");
        if (position < 0 || position > encoding.size())
            throw new IllegalArgumentException("无效的起始位置：" + position);
        if (limit < position || limit > encoding.size())
            throw new IllegalArgumentException("无效的限制位置：" + limit);
        if (bitOffset < 0 || bitOffset > 7)
            throw new IllegalArgumentException("无效的位偏移量：" + bitOffset);

        fieldDefinition = (DataFieldDefinition) definition;
        lastDecodedBits = 0;

        SyntaxField node = switch (fieldDefinition.getEncoding())
        {
            case "bslbf" -> decodeBitstream(encoding, position, bitOffset, limit);
            case "uimsbf" -> decodeUnsignedInteger(encoding, position, bitOffset, limit);
            case "checksum" -> decodeChecksum(encoding, position, bitOffset, limit);
            case "nibbles" -> decodeNibbleArray(encoding, position, bitOffset, limit, parent);
            case "octets" -> decodeOctetArray(encoding, position, bitOffset, limit, parent);
            case "text" -> decodeText(encoding, position, bitOffset, limit, parent);
            default -> throw new IllegalArgumentException("无效的字段格式");
        };
        if (parent != null)
            parent.appendChild(node);

        return lastDecodedBits;
    }

    private SyntaxField decodeBitstream(Encoding encoding, int position, int bitOffset, int limit)
    {
        // bslbf字段无需对齐，可以从字节的任意位开始，任意长度（不超过64位）。
        // bslbf字段定义要求：
        //    length表示位长度，并且length必须大于0，小于等于64。
        //    bslbf字段为确定长度字段。

        int bitLength = Integer.parseUnsignedInt(fieldDefinition.getLength());
        if (bitLength > 64)
        {
            logger.error("字段长度超过可解析范围：length={}", bitLength);
            throw new IllegalArgumentException("字段长度超过可解析范围");
        }

        int bits = bitOffset + bitLength;
        int bytes = bits / 8 + ((bits % 8 > 0) ? 1 : 0); // 算上偏移量后的全部位长度，大小应在 1~9 字节之间。
        int tailZeros = bytes * 8 - bits; // 末位距离所在字节末尾的位数，大小应在 0~7 之间。
        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
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

        lastDecodedBits = bitLength;

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.BITS, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = "b'" + Long.toBinaryString(value);
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelText = presentation.hasValueMappings()
                        ? String.format(format.getText(), value, mapIntToString(value, presentation.getValueMappings()))
                        : String.format(format.getText(), value);
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.BITS, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private SyntaxField decodeUnsignedInteger(Encoding encoding, int position, int bitOffset, int limit)
    {
        // uimsbf采用BigEndian序编码，且向右对齐，即最低位与所在字节的末位对齐。
        // uimsbf字段定义要求：
        //    length表示位长度，并且length必须大于0。
        //    uimsbf字段为确定长度字段。

        int bitLength = Integer.parseUnsignedInt(fieldDefinition.getLength());
        if (bitLength > 64)
        {
            logger.error("字段长度超过可解析范围：length={}", bitLength);
            throw new IllegalArgumentException("字段长度超过可解析范围");
        }

        int bits = bitOffset + bitLength;
        if (bits % 8 != 0)
        {
            logger.error("字段没有向右对齐：start={}, bits={}, unaligned={}", bitOffset, bitLength, bits % 8);
            throw new IllegalStateException("字段没有向右对齐");
        }

        int bytes = bits / 8; // 算上偏移量后的全部位长度，大小应在 1~8 字节之间。
        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        // 由于字段是向右对齐的（且小于等于64位），所以这里可以直接计算数值。
        long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - bitLength);
        long value = encoding.readBits(position, mask);
        lastDecodedBits = bitLength;

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.NUMBER, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = Long.toString(value);
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelText = presentation.hasValueMappings()
                        ? String.format(format.getText(), value, mapIntToString(value, presentation.getValueMappings()))
                        : String.format(format.getText(), value);
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.NUMBER, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private SyntaxField decodeChecksum(Encoding encoding, int position, int bitOffset, int limit)
    {
        // checksum采用BigEndian序编码，且按字节对齐。
        // checksum字段定义要求：
        //   字段长度必须为8、16、32、64中的一种（常用CRC算法长度）。
        //   按字节向左对齐

        int bitLength = fieldDefinition.getLengthValue();
        if (bitLength != 8 && bitLength != 16 && bitLength != 32 && bitLength != 64)
        {
            logger.error("无效的字段长度：length={}", bitLength);
            throw new IllegalArgumentException("字段长度无效");
        }

        if (bitOffset != 0)
        {
            logger.error("字段没有向左对齐");
            throw new IllegalStateException("字段没有向左对齐");
        }

        int bytes = bitLength / 8; // 算上偏移量后的全部位长度，大小应在 1~8 字节之间。
        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        // 由于字段是按字节对齐的（且小于等于64位），所以这里可以直接计算数值。
        long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - bitLength);
        long value = encoding.readBits(position, mask);
        lastDecodedBits = bitLength;

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.CHECKSUM, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = switch (bytes)
        {
            // 默认按十六进制显示
            case 1 -> String.format("%02x", value);
            case 2 -> String.format("%04x", value);
            case 4 -> String.format("%08x", value);
            case 8 -> String.format("%016x", value);
            default -> String.format("%x", value);
        };
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelText = String.format(format.getText(), value);
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.CHECKSUM, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private SyntaxField decodeNibbleArray(Encoding encoding, int position, int bitOffset, int limit,
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
            logger.error("字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的字段起始位置（未对齐）");
        }

        int nibbleCount = fieldDefinition.getLengthValue();
        if (fieldDefinition.isIndirectLength())
        {
            if (!fieldDefinition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定字段长度");
                throw new IllegalArgumentException("无法确定字段长度");
            }

            if (fieldDefinition.isImplicitLength())
            {
                nibbleCount = limit - position - bitOffset / 4;
            } else
            {
                SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, fieldDefinition.getLengthField());
                if (refNode == null)
                {
                    logger.error("无法确定字段长度");
                    throw new IllegalArgumentException("无法确定字段长度");
                }
                nibbleCount = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            nibbleCount += fieldDefinition.getLengthCorrectionValue();
        }

        lastDecodedBits = nibbleCount * 4;

        nibbleCount = nibbleCount + bitOffset / 4;
        int bytes = nibbleCount / 2 + nibbleCount % 2;
        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        int[] nibbles = encoding.readNibbles(position, nibbleCount);
        int[] value = Arrays.copyOfRange(nibbles,
                                         (bitOffset == 0) ? 0 : 1,
                                         (nibbleCount % 2 == 0) ? nibbles.length : nibbles.length - 1);

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.NIBBLES, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = IntStream.of(value)
                                    .mapToObj(i -> String.format("'%X'", i))
                                    .collect(Collectors.joining(", ", "[", "]"));
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.NIBBLES, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private SyntaxField decodeOctetArray(Encoding encoding, int position, int bitOffset, int limit,
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
            logger.error("字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的字段起始位置（未对齐）");
        }

        int octetCount = fieldDefinition.getLengthValue();
        if (fieldDefinition.isIndirectLength())
        {
            if (!fieldDefinition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定字段长度");
                throw new IllegalArgumentException("无法确定字段长度");
            }

            if (fieldDefinition.isImplicitLength())
            {
                octetCount = limit - position;
            } else
            {
                SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, fieldDefinition.getLengthField());
                if (refNode == null)
                {
                    logger.error("无法确定字段长度");
                    throw new IllegalArgumentException("无法确定字段长度");
                }
                octetCount = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            octetCount += fieldDefinition.getLengthCorrectionValue();
        }

        lastDecodedBits = octetCount * 8;

        int bytes = octetCount;
        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        int[] value = encoding.readOctets(position, bytes);

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.OCTETS, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = IntStream.of(value)
                                    .mapToObj(i -> String.format("%02X", i))
                                    .collect(Collectors.joining(", ", "[", "]"));
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.OCTETS, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private SyntaxField decodeText(Encoding encoding, int position, int bitOffset, int limit,
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
            logger.error("字段未对齐：bitOffset={}", bitOffset);
            throw new IllegalStateException("错误的字段起始位置（未对齐）");
        }

        String stringType = fieldDefinition.getStringType();
        if (!StrUtil.equalsAny(stringType, "dvb_test", "utf16", "utf8", "ascii", "gb2312", "gbk", "gb18030"))
        {
            logger.error("无效的字符编码类型：{}", stringType);
            throw new IllegalArgumentException("无效的字符编码类型");
        }

        int bytes = fieldDefinition.getLengthValue();
        if (fieldDefinition.isIndirectLength())
        {
            if (!fieldDefinition.isImplicitLength() && parent == null)
            {
                logger.error("无法确定字段长度");
                throw new IllegalArgumentException("无法确定字段长度");
            }

            if (fieldDefinition.isImplicitLength())
            {
                bytes = limit - position;
            } else
            {
                SyntaxField refNode = SyntaxFieldDecoder.findPrerequisiteField(parent, fieldDefinition.getLengthField());
                if (refNode == null)
                {
                    logger.error("无法确定字段长度");
                    throw new IllegalArgumentException("无法确定字段长度");
                }
                bytes = Math.toIntExact(refNode.getValueAsLong());
            }

            // 当使用引用长度时，有时候需要做额外的调整（删除前导或后续内容）
            bytes += fieldDefinition.getLengthCorrectionValue();
        }

        if (position + bytes > limit)
        {
            logger.error("字段超限：start={}, limit={}, field_size={}", position, limit, bytes);
            throw new IndexOutOfBoundsException("字段超出可解码范围");
        }

        lastDecodedBits = bytes * 8;
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

        FieldPresentation presentation = fieldDefinition.getPresentation();
        if (presentation == null)
        {
            return SyntaxField.invisible(SyntaxField.Type.TEXT, fieldDefinition.getName(), value);
        }

        String prefixText = fieldDefinition.getName();
        String prefixColor = null;
        if (presentation.hasPrefix())
        {
            Label prefix = presentation.getPrefix();
            prefixText = prefix.getText();
            prefixColor = StrUtil.emptyToNull(prefix.getColor());
        }

        String labelText = StrUtil.truncateUtf8(value, 64);
        String labelColor = null;
        boolean bold = false;
        if (presentation.hasFormat())
        {
            Label format = presentation.getFormat();
            labelColor = StrUtil.emptyToNull(format.getColor());
            bold = format.isBold();
        }

        return SyntaxField.visible(SyntaxField.Type.TEXT, fieldDefinition.getName(), value,
                                   prefixText, prefixColor,
                                   labelText, labelColor, bold);
    }

    private String mapIntToString(long value, List<ValueMapping> valueMappings)
    {
        return valueMappings.stream()
                            .filter(mapping -> mapping.eval(value))
                            .map(ValueMapping::mapping)
                            .findFirst()
                            .orElse(Long.toString(value));
    }
}
