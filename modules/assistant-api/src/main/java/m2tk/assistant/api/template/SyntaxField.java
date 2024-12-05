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
package m2tk.assistant.api.template;

import m2tk.util.Bytes;

import java.util.*;

public class SyntaxField
{
    public enum Type
    {
        NUMBER, CHECKSUM, TEXT, BITS, NIBBLES, OCTETS,
        LOOP_HEADER, LOOP_ENTRY_HEADER,
        DESCRIPTOR,
        SECTION
    }

    private final Type type;
    private final String name;
    private final String group;

    private final Object rawValue;
    private final String mappedValue;
    private final boolean visible;

    private final int position;
    private final int bitOffset;

    private final String prefixText;
    private final String prefixColor;
    private final String labelFormat;
    private final String labelColor;
    private final boolean bold;

    private int bitLength;
    private SyntaxField parent;
    private SyntaxField sibling;
    private LinkedList<SyntaxField> children;

    public static SyntaxField descriptor(String name, String label, int position)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(label);

        return new SyntaxField(Type.DESCRIPTOR, name, null, null, label, true,
                               position, 0,
                               null, null,
                               null, null, false);
    }

    public static SyntaxField section(String name, String label, String group, int position)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(label);

        return new SyntaxField(Type.SECTION, name, group, null, label, true,
                               position, 0,
                               null, null,
                               null, null, false);
    }

    public static SyntaxField loopHeader(String name, String label, int position, int bitOffset)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(label);

        return new SyntaxField(Type.LOOP_HEADER, name, null, null, label, true,
                               position, bitOffset,
                               null, null,
                               null, null, false);
    }

    public static SyntaxField loopEntryHeader(String name, String label, int position, int bitOffset)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(label);

        return new SyntaxField(Type.LOOP_ENTRY_HEADER, name, null, null, label, true,
                               position, bitOffset,
                               null, null,
                               null, null, false);
    }

    public static SyntaxField invisible(Type type, String name, Object rawValue, int position, int bitOffset)
    {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);
        Objects.requireNonNull(rawValue);

        return new SyntaxField(type, name, null, rawValue, null, false,
                               position, bitOffset,
                               null, null,
                               null, null, false);
    }

    public static SyntaxField visible(Type type, String name, Object rawValue, String mappedValue,
                                      String prefixText, String prefixColor,
                                      String labelFormat, String labelColor, boolean bold,
                                      int position, int bitOffset)
    {
        Objects.requireNonNull(type);
        Objects.requireNonNull(name);
        Objects.requireNonNull(rawValue);

        if (prefixColor != null && !prefixColor.matches("[0-9a-fA-F]{6}"))
            throw new IllegalArgumentException("无效的颜色参数：" + prefixColor);
        if (labelColor != null && !labelColor.matches("[0-9a-fA-F]{6}"))
            throw new IllegalArgumentException("无效的颜色参数：" + labelColor);

        return new SyntaxField(type, name, null, rawValue, mappedValue, true,
                               position, bitOffset,
                               prefixText, prefixColor,
                               labelFormat, labelColor, bold);
    }

    private SyntaxField(Type type,
                        String name,
                        String group,
                        Object rawValue,
                        String mappedValue,
                        boolean visible,
                        int position,
                        int bitOffset,
                        String prefixText,
                        String prefixColor,
                        String labelFormat,
                        String labelColor,
                        boolean bold)
    {
        if (type == Type.NIBBLES || type == Type.OCTETS ||
            type == Type.BITS || type == Type.TEXT || type == Type.NUMBER)
            Objects.requireNonNull(rawValue);

        this.type = type;
        this.name = name;
        this.group = group;
        this.rawValue = rawValue;
        this.mappedValue = mappedValue;
        this.visible = visible;

        this.position = position;
        this.bitOffset = bitOffset;

        this.prefixText = prefixText;
        this.prefixColor = prefixColor;
        this.labelFormat = labelFormat;
        this.labelColor = labelColor;
        this.bold = bold;
    }

    public Type getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getGroup()
    {
        return group;
    }

    public Object getRawValue()
    {
        return rawValue;
    }

    public String getMappedValue()
    {
        return mappedValue;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public int getPosition()
    {
        return position;
    }

    public int getBitOffset()
    {
        return bitOffset;
    }

    public int getBitLength()
    {
        return bitLength;
    }

    public String getPrefixText()
    {
        return prefixText;
    }

    public String getPrefixColor()
    {
        return prefixColor;
    }

    public String getLabelFormat()
    {
        return labelFormat;
    }

    public String getLabelColor()
    {
        return labelColor;
    }

    public boolean isBold()
    {
        return bold;
    }

    public String getValueAsString()
    {
        return formatRawValueAsString(type, rawValue);
    }

    public long getValueAsLong()
    {
        return (rawValue instanceof Number number) ? number.longValue() : 0;
    }

    public boolean getValueAsBoolean()
    {
        return (rawValue instanceof Boolean bool) ? bool : (rawValue != null);
    }

    public boolean hasParent()
    {
        return parent != null;
    }

    public SyntaxField getParent()
    {
        return parent;
    }

    public boolean hasSibling()
    {
        return sibling != null;
    }

    public SyntaxField getSibling()
    {
        return sibling;
    }

    public boolean hasChild()
    {
        return !(children == null || children.isEmpty());
    }

    public List<SyntaxField> getChildren()
    {
        return !hasChild()
               ? Collections.emptyList()
               : Collections.unmodifiableList(children);
    }

    public void setBitLength(int bitLength)
    {
        this.bitLength = bitLength;
    }

    public void setParent(SyntaxField parent)
    {
        this.parent = parent;
    }

    public void setSibling(SyntaxField sibling)
    {
        this.sibling = sibling;
    }

    public void appendChild(SyntaxField child)
    {
        Objects.requireNonNull(child);

        if (child.getParent() != null)
            throw new IllegalStateException("当前节点不是自由节点");

        if (children == null)
            children = new LinkedList<>();

        // child放到children队列的末尾，sibling是child的前一个对象。
        child.setSibling(children.isEmpty() ? null : children.getLast());
        child.setParent(this);
        children.addLast(child);
    }

    public void removeChild(SyntaxField child)
    {
        Objects.requireNonNull(child);

        if (child.getParent() != this)
            throw new IllegalArgumentException("该节点不是当前节点的子节点");

        int index = children.indexOf(child);
        ListIterator<SyntaxField> iterator = children.listIterator(index);
        if (iterator.hasNext())
        {
            // 调整后续child的sibling指向。
            SyntaxField next = iterator.next();
            next.setSibling(child.getSibling());
        }

        iterator.remove();
        child.setParent(null);
        child.setSibling(null);
    }

    public void isolate()
    {
        if (parent != null)
            parent.removeChild(this);
    }

    public SyntaxField findUpstream(String name)
    {
        Objects.requireNonNull(name);

        // 首先在同层级向前寻找
        SyntaxField node = getSibling();
        while (node != null)
        {
            if (node.getName().equals(name))
                return node;
            node = node.getSibling();
        }

        // 找不到再向上级寻找
        if (hasParent())
            return getParent().findUpstream(name);

        // 没有上级，返回空（无结果）
        return null;
    }

    public SyntaxField findLastChild(String name)
    {
        Objects.requireNonNull(name);

        SyntaxField child = hasChild() ? children.getLast() : null;
        while (child != null)
        {
            if (child.getName().equals(name))
                return child;
            child = child.getSibling();
        }
        return null;
    }

    @Override
    public String toString()
    {
        if (!visible)
            return getName() + ": [***]";

        if (labelFormat == null || labelFormat.isEmpty())
            return getName() + ": " + getValueAsString();

        return (prefixText == null || prefixText.isEmpty())
               ? labelFormat
               : prefixText + ": " + labelFormat;
    }

    private static String formatRawValueAsString(Type type, Object rawValue)
    {
        return switch (type)
        {
            case NIBBLES ->
            {
                int[] nibbles = (int[]) rawValue;
                char[] chars = new char[nibbles.length];
                for (int i = 0; i < nibbles.length; i++)
                    chars[i] = Character.forDigit(nibbles[i], 16);
                yield new String(chars);
            }
            case OCTETS ->
            {
                int[] hex = (int[]) rawValue;
                byte[] bytes = new byte[hex.length];
                for (int i = 0; i < hex.length; i++)
                    bytes[i] = (byte) hex[i];
                yield '[' + Bytes.toHexStringPrettyPrint(bytes) + ']';
            }
            case TEXT -> (String) rawValue;
            case LOOP_HEADER, LOOP_ENTRY_HEADER, DESCRIPTOR, SECTION -> "";
            default -> String.valueOf(rawValue);
        };
    }
}
