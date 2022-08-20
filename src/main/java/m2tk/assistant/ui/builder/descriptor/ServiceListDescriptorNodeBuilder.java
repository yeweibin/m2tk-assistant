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
        node.add(create(String.format("descriptor_tag = 0x%02X", decoder.getTag())));
        node.add(create(String.format("descriptor_length = %d", decoder.getPayloadLength())));

        int[] serviceIDList = decoder.getServiceIDList();
        int[] serviceTypeList = decoder.getServiceTypeList();
        for (int i = 0; i < serviceTypeList.length; i++)
        {
            String text = String.format("业务说明%d：[业务号：%d，业务类型：（0x%02X）%s]",
                                        i + 1,
                                        serviceIDList[i],
                                        serviceTypeList[i],
                                        ServiceTypes.name(serviceTypeList[i]));
            node.add(create(text));
        }

        return node;
    }
}
