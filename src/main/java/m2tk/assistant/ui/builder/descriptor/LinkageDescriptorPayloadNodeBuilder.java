package m2tk.assistant.ui.builder.descriptor;

import m2tk.assistant.ui.builder.DescriptorPayloadNodeBuilder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;

public class LinkageDescriptorPayloadNodeBuilder extends DescriptorPayloadNodeBuilder
{
    @Override
    public void build(DefaultMutableTreeNode node, Encoding payload)
    {
        int tsid = payload.readUINT16(0);
        int onid = payload.readUINT16(2);
        int sid = payload.readUINT16(4);
        int linkageType = payload.readUINT8(6);

        node.add(create(String.format("transport_stream_id = %d", tsid)));
        node.add(create(String.format("original_network_id = %d", onid)));
        node.add(create(String.format("service_id = %d", sid)));
        node.add(create(String.format("linkage_type = 0x%02X（%s）",
                                      linkageType,
                                      translateLinkageType(linkageType))));

        int offset = 7;
        if (linkageType == 0x08)
            offset = decodeMobileHandoverInfo(node, payload);
        if (linkageType == 0x0D)
            offset = decodeEventLinkageInfo(node, payload);
        if (linkageType == 0x0E)
            offset = decodeExtendedEventLinkageInfo(node, payload);

        node.add(create(String.format("private_data = [%s]",
                                      payload.toHexStringPrettyPrint(offset, payload.size()))));
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
                return "数据广播";
            case 0x07:
                return "RCS映射";
            case 0x08:
                return "移动漫游";
            default:
                return "预留使用";
        }
    }

    private int decodeMobileHandoverInfo(DefaultMutableTreeNode node, Encoding payload)
    {
        int offset = 7;

        int handoverType = (payload.readUINT8(offset) >> 4) & 0xF;
        int originType = payload.readUINT8(offset) & 0b1;

        node.add(create(String.format("漫游类型 = 0x%02X（%s）",
                                      handoverType,
                                      translateHandoverType(handoverType))));
        node.add(create(String.format("原始类型 = 0x%02X（%s）",
                                      originType,
                                      translateOriginType(originType))));

        offset += 1;
        if (handoverType == 0x01 || handoverType == 0x02 || handoverType == 0x03)
        {
            int networkId = payload.readUINT16(offset);
            node.add(create(String.format("network_id = %d", networkId)));
            offset += 2;
        }
        if (originType == 0x00)
        {
            int initialServiceId = payload.readUINT16(offset);
            node.add(create(String.format("initial_service_id = %d", initialServiceId)));
            offset += 2;
        }

        return offset;
    }

    private int decodeEventLinkageInfo(DefaultMutableTreeNode node, Encoding payload)
    {
        int offset = 7;

        int targetEventId = payload.readUINT16(offset);
        int targetListed = (payload.readUINT8(offset + 2) >> 7) & 0b1;
        int eventSimulcast = (payload.readUINT8(offset + 2) >> 6) & 0b1;
        offset += 3;

        node.add(create(String.format("target_event_id = %d", targetEventId)));
        node.add(create(String.format("target_listed = %d", targetListed)));
        node.add(create(String.format("event_simulcast = %d", eventSimulcast)));

        return offset;
    }

    private int decodeExtendedEventLinkageInfo(DefaultMutableTreeNode node, Encoding payload)
    {
        int offset = 7;

        int limit = offset + payload.readUINT8(offset);
        offset += 1;

        while (offset < limit)
        {
            int targetEventId = payload.readUINT16(offset);
            int targetListed = (payload.readUINT8(offset + 2) >> 7) & 0b1;
            int eventSimulcast = (payload.readUINT8(offset + 2) >> 6) & 0b1;
            int linkType= (payload.readUINT8(offset + 2) >> 4) & 0b11;
            int targetIdType = (payload.readUINT8(offset + 2) >> 2) & 0b11;
            int originalNetworkIdFlag = (payload.readUINT8(offset + 2) >> 1) & 0b1;
            int serviceIdFlag = payload.readUINT8(offset + 2) & 0b1;
            offset += 3;

            node.add(create(String.format("target_event_id = %d", targetEventId)));
            node.add(create(String.format("target_listed = %d", targetListed)));
            node.add(create(String.format("event_simulcast = %d", eventSimulcast)));
            node.add(create(String.format("link_type = %d", linkType)));
            node.add(create(String.format("target_id_type = %d", targetIdType)));
            node.add(create(String.format("original_network_id_flag = %d", originalNetworkIdFlag)));
            node.add(create(String.format("service_id_flag = %d", serviceIdFlag)));

            if (targetIdType == 3)
            {
                node.add(create(String.format("user_defined_id = %d",
                                              payload.readUINT16(offset))));
                offset += 2;
            } else
            {
                if (targetIdType == 1)
                {
                    node.add(create(String.format("target_transport_stream_id = %d",
                                                  payload.readUINT16(offset))));
                    offset += 2;
                }

                if (originalNetworkIdFlag == 1)
                {
                    node.add(create(String.format("target_original_network_id = %d",
                                                  payload.readUINT16(offset))));
                    offset += 2;
                }

                if (serviceIdFlag == 1)
                {
                    node.add(create(String.format("target_service_id = %d",
                                                  payload.readUINT16(offset))));
                    offset += 2;
                }
            }
        }

        return offset;
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
