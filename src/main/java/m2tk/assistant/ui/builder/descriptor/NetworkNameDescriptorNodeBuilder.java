package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.NetworkNameDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NetworkNameDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final NetworkNameDescriptorDecoder decoder = new NetworkNameDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("network_name_descriptor");
        node.add(create("descriptor_tag = 0x40"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));
        node.add(create(String.format("network name = %s（原始数据：%s）",
                                      decoder.getNetworkName(),
                                      decoder.getPayload().toHexStringPrettyPrint())));

        return node;
    }
}
