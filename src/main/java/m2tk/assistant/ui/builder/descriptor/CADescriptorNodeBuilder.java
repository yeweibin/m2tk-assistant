package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.descriptor.CADescriptorDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class CADescriptorNodeBuilder implements TreeNodeBuilder
{
    private final CADescriptorDecoder cad = new CADescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        cad.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("CA_descriptor");
        node.add(create("descriptor_tag = 0x09"));
        node.add(create("descriptor_length = " + cad.getPayloadLength()));
        node.add(create(String.format("CA_system_id = 0x%04X", cad.getConditionalAccessSystemID())));
        node.add(create(String.format("CA_PID = 0x%04X", cad.getConditionalAccessStreamPID())));

        Encoding privateData = cad.getPrivateData();
        if (privateData.size() > 0)
        {
            String text = String.format("private_data = [%s]",
                                        privateData.toHexStringPrettyPrint());
            node.add(create(text));
        }

        return node;
    }
}
