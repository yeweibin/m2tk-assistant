package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.MPEG2;

import javax.swing.tree.DefaultMutableTreeNode;

public class CADescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        node.add(create(String.format("CA_system_id = 0x%04X", payload.readUINT16(0))));
        node.add(create(String.format("CA_PID = 0x%04X", payload.readUINT16(2) & MPEG2.PID_MASK)));

        Encoding privateData = payload.readSelector(4);
        if (privateData.size() > 0)
        {
            String text = String.format("private_data = [%s]", privateData.toHexStringPrettyPrint());
            node.add(create(text));
        }
    }
}
