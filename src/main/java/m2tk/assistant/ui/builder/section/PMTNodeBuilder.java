package m2tk.assistant.ui.builder.section;

import m2tk.assistant.analyzer.presets.StreamTypes;
import m2tk.assistant.ui.builder.DescriptorNodeBuilders;
import m2tk.assistant.ui.builder.TreeNodeBuilder;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.DescriptorLoopDecoder;
import m2tk.mpeg2.decoder.element.ProgramElementDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class PMTNodeBuilder implements TreeNodeBuilder
{
    private final PMTSectionDecoder pmt = new PMTSectionDecoder();
    private final DescriptorLoopDecoder descriptorLoopDecoder = new DescriptorLoopDecoder();
    private final ProgramElementDecoder elementDecoder = new ProgramElementDecoder();

    @Override
    public MutableTreeNode build(Encoding encoding)
    {
        pmt.attach(encoding);

        String text = String.format("Section[%02X]（节目号：%d）",
                                    pmt.getSectionNumber(),
                                    pmt.getProgramNumber());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        node.add(create(String.format("table_id = 0x%02X", pmt.getTableID())));
        node.add(create(String.format("section_syntax_indicator = %d", pmt.getSyntaxIndicator())));
        node.add(create(String.format("section_length = %d", pmt.getSectionLength())));
        node.add(create(String.format("program_number = %d", pmt.getProgramNumber())));
        node.add(create(String.format("version_number = %d", pmt.getVersionNumber())));
        node.add(create(String.format("current_next_indicator = %d", pmt.getCurrentNextIndicator())));
        node.add(create(String.format("section_number = %d", pmt.getSectionNumber())));
        node.add(create(String.format("last_section_number = %d", pmt.getLastSectionNumber())));

        node.add(create(String.format("PCR_PID = 0x%X", pmt.getProgramClockReferencePID())));

        DefaultMutableTreeNode nodeProgramInfo = new DefaultMutableTreeNode();
        Encoding programInfo = pmt.getDescriptorLoop();
        if (programInfo.size() > 0)
        {
            descriptorLoopDecoder.attach(programInfo);
            descriptorLoopDecoder.forEach(descriptor -> {
                int tag = descriptor.readUINT8(0);
                TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                nodeProgramInfo.add(builder.build(descriptor));
            });
        }
        nodeProgramInfo.setUserObject(String.format("节目描述（%d）", nodeProgramInfo.getChildCount()));
        node.add(nodeProgramInfo);

        Encoding[] elements = pmt.getProgramElementList();
        for (int i = 0; i < elements.length; i++)
        {
            elementDecoder.attach(elements[i]);

            DefaultMutableTreeNode nodeElement = new DefaultMutableTreeNode(String.format("基本流%d", i + 1));
            nodeElement.add(create(String.format("stream_Type = 0x%02X（%s）",
                                                 elementDecoder.getStreamType(),
                                                 StreamTypes.description(elementDecoder.getStreamType()))));
            nodeElement.add(create(String.format("elementary_PID = 0x%X", elementDecoder.getElementaryPID())));

            DefaultMutableTreeNode nodeESInfo = new DefaultMutableTreeNode();
            Encoding esInfo = elementDecoder.getDescriptorLoop();
            if (esInfo.size() > 0)
            {
                DescriptorLoopDecoder loopDecoder = new DescriptorLoopDecoder();
                loopDecoder.attach(esInfo);
                loopDecoder.forEach(descriptor -> {
                    int tag = descriptor.readUINT8(0);
                    TreeNodeBuilder builder = DescriptorNodeBuilders.getBuilder(tag);
                    nodeESInfo.add(builder.build(descriptor));
                });
            }
            nodeESInfo.setUserObject(String.format("ES描述（%d）", nodeESInfo.getChildCount()));
            nodeElement.add(nodeESInfo);

            node.add(nodeElement);
        }

        node.add(create(String.format("CRC_32 = 0x%08X", pmt.getChecksum())));

        return node;
    }
}
