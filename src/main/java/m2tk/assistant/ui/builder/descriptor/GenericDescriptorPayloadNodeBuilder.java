package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class GenericDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        node.add(create(String.format("payload = [%s]", payload.toHexStringPrettyPrint())));
    }
}