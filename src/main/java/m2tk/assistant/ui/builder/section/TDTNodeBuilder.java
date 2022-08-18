package m2tk.assistant.ui.builder.section;

import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.dvb.DVB;
import m2tk.dvb.decoder.section.TDTSectionDecoder;
import m2tk.encoding.Encoding;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TDTNodeBuilder implements TreeNodeBuilder
{
    private final TDTSectionDecoder tdt = new TDTSectionDecoder();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        tdt.attach(encoding);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(String.format("Section（本地时间：%s）",
                                                                               translateTimepoint2Local(tdt.getUTCTime())));
        node.add(create(String.format("table_id = 0x%02X", tdt.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", tdt.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", tdt.getSectionLength())));
        node.add(create(String.format("UTC_time = %s（原始数据：%s）",
                                      translateTimepoint2UTC(tdt.getUTCTime()),
                                      encoding.toHexStringPrettyPrint(3, 8))));

        return node;
    }

    private String translateTimepoint2UTC(long timepoint)
    {
        return LocalDateTime.of(DVB.decodeDate((int) (timepoint >> 24)),
                                DVB.decodeTime((int) (timepoint & 0xFFFFFF)))
                            .format(timeFormatter);
    }

    private String translateTimepoint2Local(long timepoint)
    {
        return DVB.decodeTimepointIntoLocalDateTime(timepoint).format(timeFormatter);
    }
}
