package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class DSNGDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final DescriptorDecoder decoder = new DescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("DSNG_descriptor");
        node.add(create("descriptor_tag = 0x68"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));

        if (decoder.getPayloadLength() > 0)
            node.add(create("descriptor_payload = " + decoder.getPayload().toHexStringPrettyPrint()));

        return node;
    }
}
