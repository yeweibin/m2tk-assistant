package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.analyzer.presets.ServiceTypes;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.decoder.descriptor.ServiceListDescriptorDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ServiceListDescriptorNodeBuilder implements TreeNodeBuilder
{
    private final ServiceListDescriptorDecoder decoder = new ServiceListDescriptorDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        decoder.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode("service_list_descriptor");
        node.add(create("descriptor_tag = 0x41"));
        node.add(create("descriptor_length = " + decoder.getPayloadLength()));

        int[] serviceIDList = decoder.getServiceIDList();
        int[] serviceTypeList = decoder.getServiceTypeList();
        for (int i = 0; i < serviceTypeList.length; i++)
        {
            DefaultMutableTreeNode nodeItem = new DefaultMutableTreeNode("业务说明" + (i + 1));
            nodeItem.add(create("service_id = " + serviceIDList[i]));
            nodeItem.add(create(String.format("service_type = 0x%02X（%s）",
                                              serviceTypeList[i],
                                              ServiceTypes.name(serviceTypeList[i]))));
            node.add(nodeItem);
        }

        return node;
    }
}
