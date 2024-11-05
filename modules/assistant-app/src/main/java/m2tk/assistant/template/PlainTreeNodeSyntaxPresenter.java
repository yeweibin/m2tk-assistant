package m2tk.assistant.template;

import cn.hutool.core.util.StrUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于Swing TreeNode实现的语法绘制器，仅实现前缀与格式化效果。
 */
public class PlainTreeNodeSyntaxPresenter
{
    private static final String FORMAT_SPECIFIER = "%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static final Pattern FORMAT_SPECIFIER_PATTERN = Pattern.compile(FORMAT_SPECIFIER);

    public MutableTreeNode render(SyntaxField syntax)
    {
        if (!syntax.isVisible())
            return null;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        root.setUserObject(renderLabel(syntax));

        for (SyntaxField child : syntax.getChildren())
        {
            MutableTreeNode node = render(child);
            if (node != null)
                root.add(node);
        }

        return root;
    }

    private String renderLabel(SyntaxField field)
    {
        return switch (field.getType())
        {
            case BITS -> renderBits(field);
            case NUMBER -> renderNumber(field);
            case CHECKSUM -> renderChecksum(field);
            case NIBBLES -> renderNibbles(field);
            case OCTETS -> renderOctets(field);
            case TEXT -> renderText(field);
            default -> field.getMappedValue();
        };
    }

    private int countRequiredArguments(String fmt)
    {
        Matcher matcher = FORMAT_SPECIFIER_PATTERN.matcher(fmt);

        int counter = 0;
        while (matcher.find())
        {
            counter++;
        }

        return counter;
    }

    private String renderBits(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        String format = field.getLabelFormat();

        if (format == null)
        {
            String mappedValue = field.getMappedValue();
            return (mappedValue != null)
                   ? prefix + ": " + mappedValue
                   : prefix + ": " + String.format("b'%s", Long.toBinaryString(field.getValueAsLong()));
        }

        List<Object> args = new ArrayList<>();
        int count = countRequiredArguments(format);
        while (count > 0)
        {
            args.add(field.getRawValue());
            count--;
        }
        String text = prefix + ": " + String.format(format, args.toArray());
        String mappedValue = field.getMappedValue();
        if (mappedValue != null)
            text += String.format(" (%s)", mappedValue);
        return text;
    }

    private String renderNumber(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        String format = field.getLabelFormat();

        if (format == null)
        {
            String mappedValue = field.getMappedValue();
            return (mappedValue != null)
                   ? prefix + ": " + mappedValue
                   : prefix + ": " + String.format("%d", field.getValueAsLong());
        }

        List<Object> args = new ArrayList<>();
        int count = countRequiredArguments(format);
        while (count > 0)
        {
            args.add(field.getRawValue());
            count--;
        }
        String text = prefix + ": " + String.format(format, args.toArray());
        String mappedValue = field.getMappedValue();
        if (mappedValue != null)
            text += String.format(" (%s)", mappedValue);
        return text;
    }

    private String renderChecksum(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        String format = field.getLabelFormat();

        if (format == null)
        {
            String mappedValue = field.getMappedValue();
            return (mappedValue != null)
                   ? prefix + ": " + mappedValue
                   : prefix + ": " + String.format("%x", field.getValueAsLong());
        }

        List<Object> args = new ArrayList<>();
        int count = countRequiredArguments(format);
        while (count > 0)
        {
            args.add(field.getRawValue());
            count--;
        }
        return prefix + ": " + String.format(format, args.toArray());
    }

    private String renderNibbles(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        String format = field.getLabelFormat();

        // 无指定格式，采用“[X X X]”样式
        if (format == null)
        {
            String text = IntStream.of((int[]) field.getRawValue())
                                   .mapToObj(i -> String.format("%X", i))
                                   .collect(Collectors.joining(" ", "[", "]"));

            return prefix + ": " + text;
        }

        // BCD格式，每个‘#’代表一个BCD数字，中间可以添加小数点或任意连接符号。
        if (format.contains("#"))
        {
            try
            {
                StringBuilder sbuf = new StringBuilder();
                sbuf.append(prefix).append(": ");

                int[] nibbles = (int[]) field.getRawValue();
                int offset = 0;
                for (int i = 0; i < format.length(); i++)
                {
                    char c = format.charAt(i);
                    if (c == '#')
                    {
                        sbuf.append(Character.forDigit(nibbles[offset], 16));
                        offset ++;
                    } else
                        sbuf.append(c);
                }

                return sbuf.toString();
            } catch (IndexOutOfBoundsException ex)
            {
                return prefix + ": " + "***无法匹配格式***";
            }
        } else
        {
            // 普通格式

            Set<String> options = new HashSet<>(StrUtil.split(format, ",", true, true));
            boolean compact = options.contains("compact");
            boolean lowercase = options.contains("lowercase");

            String s0 = lowercase ? "%x" : "%X";
            String s1 = compact ? "" : " ";
            String s2 = compact ? "" : "[";
            String s3 = compact ? "" : "]";
            String text = IntStream.of((int[]) field.getRawValue())
                                   .mapToObj(i -> String.format(s0, i))
                                   .collect(Collectors.joining(s1, s2, s3));

            return prefix + ": " + text;
        }
    }

    private String renderOctets(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        String format = field.getLabelFormat();

        boolean compact = false;
        boolean lowercase = false;
        if (format != null)
        {
            Set<String> options = new HashSet<>(StrUtil.split(format, ",", true, true));
            compact = options.contains("compact");
            lowercase = options.contains("lowercase");
        }

        String s0 = lowercase ? "%02x" : "%02X";
        String s1 = compact ? "" : " ";
        String s2 = compact ? "" : "[";
        String s3 = compact ? "" : "]";
        String text = IntStream.of((int[]) field.getRawValue())
                               .mapToObj(i -> String.format(s0, i))
                               .collect(Collectors.joining(s1, s2, s3));

        return prefix + ": " + text;
    }

    private String renderText(SyntaxField field)
    {
        String prefix = Optional.ofNullable(field.getPrefixText()).orElse(field.getName());
        return prefix + ": " + field.getValueAsString();
    }
}
