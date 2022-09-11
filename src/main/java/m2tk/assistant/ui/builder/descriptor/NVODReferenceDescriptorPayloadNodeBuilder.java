package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class NVODReferenceDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int count = payload.size() / 6;
        for (int i = 0; i < count; i++)
        {
            String text = String.format("时移业务%d：tsid = %d, onid = %d, sid = %d",
                                        i + 1,
                                        payload.readUINT16(i * 6),
                                        payload.readUINT16(i * 6 + 2),
                                        payload.readUINT16(i * 6 + 4));
            node.add(create(text));
        }
    }
}
