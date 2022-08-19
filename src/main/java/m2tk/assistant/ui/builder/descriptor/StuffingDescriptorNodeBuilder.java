package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.StuffingDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class StuffingDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final StuffingDescriptorDecoder decoder = new StuffingDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("stuffing_descriptor");
        node.add(create("descriptor_tag = 0x42"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));

        if (decoder.getPayloadLength() > 0)
            node.add(create("填充字节 = " + decoder.getPayload().toHexStringPrettyPrint()));

        return node;
    }
}
