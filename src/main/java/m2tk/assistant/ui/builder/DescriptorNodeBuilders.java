package m2tk.assistant.ui.builder;

import m2tk.assistant.ui.builder.descriptor.CADescriptorNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DescriptorNodeBuilders
{
    private static final Map<Integer, Class<? extends TreeNodeBuilder>> builderClasses;
    static
    {
        builderClasses = new HashMap<>();
        builderClasses.put(0x09, CADescriptorNodeBuilder.class);
    }

    private DescriptorNodeBuilders()
    {}

    public static void registerBuilder(int tag, Class<? extends TreeNodeBuilder> builderClass)
    {
        Objects.requireNonNull(builderClass);
        builderClasses.put(tag, builderClass);
    }

    public static TreeNodeBuilder getBuilder(int tag)
    {
        try
        {
            Class<? extends TreeNodeBuilder> cls = builderClasses.getOrDefault(tag, GenericDescriptorNodeBuilder.class);
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception ex)
        {
            return new GenericDescriptorNodeBuilder();
        }
    }

    static class GenericDescriptorNodeBuilder implements TreeNodeBuilder
    {
        @Override
        public MutableTreeNode build(Encoding encoding)
        {
            DescriptorDecoder decoder = new DescriptorDecoder();
            decoder.attach(encoding);

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(String.format("Unknown_descriptor (%02X)",
                                                                                   decoder.getTag()));
            node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
            node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));
            if (decoder.getPayloadLength() > 0)
                node.add(create(String.format("descriptor_payload = %s", decoder.getPayload().toHexStringPrettyPrint())));
            return node;
        }
    }
}
