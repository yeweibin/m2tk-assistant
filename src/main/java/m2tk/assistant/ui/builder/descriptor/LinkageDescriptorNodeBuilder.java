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
        node.add(create(String.format("linkage_type = 0x%02X（%s）",
                                      decoder.getLinkageType(),
                                      translateLinkageType(decoder.getLinkageType()))));

        int offset = 9;
        if (decoder.getLinkageType() == 0x08)
        {
            Encoding handover = decoder.getMobileHandoverInfo();

            int handoverType = handover.readUINT8(0) >> 4;
            node.add(create(String.format("漫游类型 = 0x%02X（%s）",
                                          handoverType,
                                          translateHandoverType(handoverType))));

            int originType = handover.readUINT8(0) & 0b1;
            node.add(create(String.format("原始类型 = 0x%02X（%s）",
                                          originType,
                                          translateOriginType(originType))));

            int off = 1;
            if (handoverType == 0x01 || handoverType == 0x02 || handoverType == 0x03)
            {
                node.add(create("network_id = " + handover.readUINT16(1)));
                off += 2;
            }
            if (originType == 0x00)
            {
                node.add(create("initial_service_id = " + handover.readUINT16(off)));
            }
            offset += handover.size();
        }

        node.add(create("private_data = " + encoding.readSelector(offset).toHexStringPrettyPrint()));
        return node;
    }

    private String translateLinkageType(int type)
    {
        if (0x80 <= type && type <= 0xFE)
            return "用户定义";

        switch (type)
        {
            case 0x01:
                return "信息服务";
            case 0x02:
                return "EPG";
            case 0x03:
                return "CA替换功能";
            case 0x04:
                return "包含了完整网络/业务群SI描述的传输流";
            case 0x05:
                return "业务替换功能";
            case 0x06:
                return "RCS映射";
            case 0x07:
                return "移动漫游";
            default:
                return "预留使用";
        }
    }

    private String translateHandoverType(int type)
    {
        switch (type)
        {
            case 0x01:
                return "漫游至邻国的同一业务";
            case 0x02:
                return "漫游至同一业务的本地变更";
            case 0x03:
                return "漫游至关联业务";
            default:
                return "预留使用";
        }
    }

    private String translateOriginType(int type)
    {
        switch (type)
        {
            case 0x00:
                return "NIT";
            case 0x01:
                return "SDT";
            default:
                return "";
        }
    }
}
