package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.LinkageDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class LinkageDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final LinkageDescriptorDecoder decoder = new LinkageDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("linkage_descriptor");

        node.add(create("descriptor_tag = 0x4A"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));

        node.add(create("transport_stream_id = " + decoder.getTransportStreamID()));
        node.add(create("original_network_id = " + decoder.getOriginalNetworkID()));
        node.add(create("service_id = " + decoder.getServiceID()));
        node.add(create("linkage_type = " + decoder.getLinkageType()));
        node.add(create("private_data = " + decoder.getSelector().toHexStringPrettyPrint()));

        return node;
    }
}
