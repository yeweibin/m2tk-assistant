/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package m2tk.assistant.app.ui.component;

import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.domain.PrivateSection;
import m2tk.assistant.api.template.RichTreeNodeSyntaxPresenter;
import m2tk.assistant.api.template.RichTreeNodeSyntaxPresenter.NodeContext;
import m2tk.assistant.api.template.SectionDecoder;
import m2tk.assistant.api.template.SyntaxField;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;
import net.miginfocom.swing.MigLayout;
import org.exbin.auxiliary.binary_data.ByteArrayData;
import org.exbin.bined.CodeType;
import org.exbin.bined.basic.BasicBackgroundPaintMode;
import org.exbin.bined.swing.basic.CodeArea;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

@Slf4j
public class SectionDatagramPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode groupPSI;
    private DefaultMutableTreeNode groupSI;
    private DefaultMutableTreeNode groupPrivate;
    private DefaultMutableTreeNode groupPAT;
    private DefaultMutableTreeNode groupCAT;
    private DefaultMutableTreeNode groupPMT;
    private DefaultMutableTreeNode groupNITActual;
    private DefaultMutableTreeNode groupNITOther;
    private DefaultMutableTreeNode groupBAT;
    private DefaultMutableTreeNode groupSDTActual;
    private DefaultMutableTreeNode groupSDTOther;
    private DefaultMutableTreeNode groupEITPFActual;
    private DefaultMutableTreeNode groupEITPFOther;
    private DefaultMutableTreeNode groupEITScheduleActual;
    private DefaultMutableTreeNode groupEITScheduleOther;
    private DefaultMutableTreeNode groupTDT;
    private DefaultMutableTreeNode groupTOT;

    private SectionDecoder decoder;
    private RichTreeNodeSyntaxPresenter presenter;
    private Map<TreeNode, PrivateSection> nodeSectionMap;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public SectionDatagramPanel()
    {
        initUI();
    }

    private void initUI()
    {
        root = new DefaultMutableTreeNode("/");
        model = new DefaultTreeModel(root);

        decoder = new SectionDecoder();
        presenter = new RichTreeNodeSyntaxPresenter();
        nodeSectionMap = new HashMap<>();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        CodeArea codeArea = new CodeArea();
        codeArea.setBackgroundPaintMode(BasicBackgroundPaintMode.PLAIN);
        codeArea.setCodeType(CodeType.HEXADECIMAL);
        codeArea.setVisible(true);
        CodeArea fieldArea = new CodeArea();
        fieldArea.setBackgroundPaintMode(BasicBackgroundPaintMode.PLAIN);
        fieldArea.setCodeType(CodeType.BINARY);
        fieldArea.setVisible(false);
        JPanel dataPanel = new JPanel(new MigLayout("insets 0, width 450!", "[grow]", "[grow][]"));
        dataPanel.add(codeArea, "grow, wrap");
        dataPanel.add(fieldArea, "hidemode 3, grow, height 200!, wrap");
        dataPanel.setVisible(false);

        JTree tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new SectionDatagramTreeCellRenderer());
        tree.addTreeSelectionListener(e -> {
            TreePath path = tree.getSelectionPath();
            if (path == null)
            {
                dataPanel.setVisible(false);
                splitPane.setDividerLocation(1.0);
                return;
            }

            // 选中列表即显示HexView，但不一定有数据。
            splitPane.setDividerLocation(0.45);
            dataPanel.setVisible(true);

            PrivateSection section = null;
            Object[] nodes = path.getPath();
            for (int i = 2; i < nodes.length; i++)
            {
                section = nodeSectionMap.get((TreeNode) nodes[i]);
                if (section != null)
                    break;
            }
            if (section == null)
            {
                codeArea.setContentData(null);
                fieldArea.setVisible(false);
            } else
            {
                byte[] encoding = section.getEncoding();
                codeArea.setContentData(new ByteArrayData(encoding));

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof NodeContext context)
                {
                    SyntaxField syntax = context.getSyntax();
                    if (syntax.getBitLength() == 0)
                    {
                        codeArea.clearSelection();
                    } else
                    {
                        int start = syntax.getPosition();
                        int end = syntax.getPosition() + (syntax.getBitOffset() + syntax.getBitLength()) / 8;
                        if (end == start || (syntax.getBitOffset() + syntax.getBitLength()) % 8 != 0)
                            end++;
                        codeArea.setSelection(start, end);

                        int lastBitOffset = syntax.getBitOffset() + syntax.getBitLength();
                        codeArea.setCaretPosition(end - 1,
                                                  (lastBitOffset % 8 == 0 || lastBitOffset % 8 > 4) ? 1 : 0);
                        codeArea.revealCursor(); // 注意：当codeArea获得焦点后才会显示Cursor。

                        if (syntax.getType() == SyntaxField.Type.NUMBER ||
                            syntax.getType() == SyntaxField.Type.BITS)
                        {
                            fieldArea.setContentData(new ByteArrayData(Arrays.copyOfRange(encoding, start, end)));
                            fieldArea.setVisible(true);
                        } else
                        {
                            fieldArea.setVisible(false);
                        }
                    }
                }
            }
        });

        ToolTipManager.sharedInstance().registerComponent(tree);

        splitPane.setLeftComponent(new JScrollPane(tree));
        splitPane.setRightComponent(dataPanel);
        splitPane.setDividerLocation(1.0);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        constructTreeSkeleton();
    }

    private void constructTreeSkeleton()
    {
        groupPSI = new DefaultMutableTreeNode("MPEG标准段");
        groupSI = new DefaultMutableTreeNode("DVB标准段");
        groupPrivate = new DefaultMutableTreeNode("自定义私有段");

        groupPAT = new DefaultMutableTreeNode("PAT");
        groupCAT = new DefaultMutableTreeNode("CAT");
        groupPMT = new DefaultMutableTreeNode("PMT");
        groupBAT = new DefaultMutableTreeNode("BAT");
        groupNITActual = new DefaultMutableTreeNode("NIT_Actual");
        groupNITOther = new DefaultMutableTreeNode("NIT_Other");
        groupSDTActual = new DefaultMutableTreeNode("SDT_Actual");
        groupSDTOther = new DefaultMutableTreeNode("SDT_Other");
        groupEITPFActual = new DefaultMutableTreeNode("EIT_PF_Actual");
        groupEITPFOther = new DefaultMutableTreeNode("EIT_PF_Other");
        groupEITScheduleActual = new DefaultMutableTreeNode("EIT_Schedule_Actual");
        groupEITScheduleOther = new DefaultMutableTreeNode("EIT_Schedule_Other");
        groupTDT = new DefaultMutableTreeNode("TDT");
        groupTOT = new DefaultMutableTreeNode("TOT");

        root.add(groupPSI);
        root.add(groupSI);
        root.add(groupPrivate);
        groupPSI.add(groupPAT);
        groupPSI.add(groupCAT);
        groupPSI.add(groupPMT);
        groupSI.add(groupBAT);
        groupSI.add(groupNITActual);
        groupSI.add(groupNITOther);
        groupSI.add(groupSDTActual);
        groupSI.add(groupSDTOther);
        groupSI.add(groupEITPFActual);
        groupSI.add(groupEITPFOther);
        groupSI.add(groupEITScheduleActual);
        groupSI.add(groupEITScheduleOther);
        groupSI.add(groupTDT);
        groupSI.add(groupTOT);

        model.reload();
    }

    public void update(Map<String, List<PrivateSection>> sectionGroups)
    {
        nodeSectionMap.clear();
        addPATSectionNodes(sectionGroups.getOrDefault("PAT", Collections.emptyList()));
        addCATSectionNodes(sectionGroups.getOrDefault("CAT", Collections.emptyList()));
        addPMTSectionNodes(sectionGroups.getOrDefault("PMT", Collections.emptyList()));
        addBATSectionNodes(sectionGroups.getOrDefault("BAT", Collections.emptyList()));
        addNITActualSectionNodes(sectionGroups.getOrDefault("NIT_Actual", Collections.emptyList()));
        addNITOtherSectionNodes(sectionGroups.getOrDefault("NIT_Other", Collections.emptyList()));
        addSDTActualSectionNodes(sectionGroups.getOrDefault("SDT_Actual", Collections.emptyList()));
        addSDTOtherSectionNodes(sectionGroups.getOrDefault("SDT_Other", Collections.emptyList()));
        addEITPFActualSectionNodes(sectionGroups.getOrDefault("EIT_PF_Actual", Collections.emptyList()));
        addEITPFOtherSectionNodes(sectionGroups.getOrDefault("EIT_PF_Other", Collections.emptyList()));
        addEITScheduleActualSectionNodes(sectionGroups.getOrDefault("EIT_Schedule_Actual", Collections.emptyList()));
        addEITScheduleOtherSectionNodes(sectionGroups.getOrDefault("EIT_Schedule_Other", Collections.emptyList()));
        addTDTSectionNodes(sectionGroups.getOrDefault("TDT", Collections.emptyList()));
        addTOTSectionNodes(sectionGroups.getOrDefault("TOT", Collections.emptyList()));
        addUserPrivateSectionNodes(sectionGroups.getOrDefault("UserPrivate", Collections.emptyList()));
        model.reload();
    }

    private void addPATSectionNodes(List<PrivateSection> sections)
    {
        groupPAT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 传输流号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "transport_stream_id")));
            groupPAT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupPAT.setUserObject(String.format("PAT (%d)", groupPAT.getChildCount()));
    }

    private void addCATSectionNodes(List<PrivateSection> sections)
    {
        groupCAT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X]",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number")));
            groupCAT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupCAT.setUserObject(String.format("CAT (%d)", groupCAT.getChildCount()));
    }

    private void addPMTSectionNodes(List<PrivateSection> sections)
    {
        groupPMT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 节目号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "program_number")));
            groupPMT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupPMT.setUserObject(String.format("PMT (%d)", groupPMT.getChildCount()));
    }

    private void addBATSectionNodes(List<PrivateSection> sections)
    {
        groupBAT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 业务群号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "bouquet_id")));
            groupBAT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupBAT.setUserObject(String.format("BAT (%d)", groupBAT.getChildCount()));
    }

    private void addNITActualSectionNodes(List<PrivateSection> sections)
    {
        groupNITActual.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 网络号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "network_id")));
            groupNITActual.add(node);
            nodeSectionMap.put(node, section);
        }

        groupNITActual.setUserObject(String.format("NIT_Actual (%d)", groupNITActual.getChildCount()));
    }

    private void addNITOtherSectionNodes(List<PrivateSection> sections)
    {
        groupNITOther.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 网络号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "network_id")));
            groupNITOther.add(node);
            nodeSectionMap.put(node, section);
        }

        groupNITOther.setUserObject(String.format("NIT_Other (%d)", groupNITOther.getChildCount()));
    }

    private void addSDTActualSectionNodes(List<PrivateSection> sections)
    {
        groupSDTActual.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 传输流号：%d，原始网络号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "transport_stream_id"),
                                           getFieldValue(syntax, "original_network_id")));
            groupSDTActual.add(node);
            nodeSectionMap.put(node, section);
        }

        groupSDTActual.setUserObject(String.format("SDT_Actual (%d)", groupSDTActual.getChildCount()));
    }

    private void addSDTOtherSectionNodes(List<PrivateSection> sections)
    {
        groupSDTOther.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 传输流号：%d，原始网络号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "transport_stream_id"),
                                           getFieldValue(syntax, "original_network_id")));
            groupSDTOther.add(node);
            nodeSectionMap.put(node, section);
        }

        groupSDTOther.setUserObject(String.format("SDT_Other (%d)", groupSDTOther.getChildCount()));
    }

    private void addEITPFActualSectionNodes(List<PrivateSection> sections)
    {
        groupEITPFActual.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 业务号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "service_id")));
            groupEITPFActual.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEITPFActual.setUserObject(String.format("EIT_PF_Actual (%d)", groupEITPFActual.getChildCount()));
    }

    private void addEITPFOtherSectionNodes(List<PrivateSection> sections)
    {
        groupEITPFOther.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 业务号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "service_id")));
            groupEITPFOther.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEITPFOther.setUserObject(String.format("EIT_PF_Other (%d)", groupEITPFOther.getChildCount()));
    }

    private void addEITScheduleActualSectionNodes(List<PrivateSection> sections)
    {
        groupEITScheduleActual.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 业务号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "service_id")));
            groupEITScheduleActual.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEITScheduleActual.setUserObject(String.format("EIT_Schedule_Actual (%d)", groupEITScheduleActual.getChildCount()));
    }

    private void addEITScheduleOtherSectionNodes(List<PrivateSection> sections)
    {
        groupEITScheduleOther.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("[V:%02X, S:%02X, L:%02X] 业务号：%d",
                                           getFieldValue(syntax, "version_number"),
                                           getFieldValue(syntax, "section_number"),
                                           getFieldValue(syntax, "last_section_number"),
                                           getFieldValue(syntax, "service_id")));
            groupEITScheduleOther.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEITScheduleOther.setUserObject(String.format("EIT_Schedule_Other (%d)", groupEITScheduleOther.getChildCount()));
    }

    private void addTDTSectionNodes(List<PrivateSection> sections)
    {
        groupTDT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("时间：%s",
                                           translateTimepoint2Local(getFieldValue(syntax, "UTC_time"))));
            groupTDT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupTDT.setUserObject(String.format("TDT (%d)", groupTDT.getChildCount()));
    }

    private void addTOTSectionNodes(List<PrivateSection> sections)
    {
        groupTOT.removeAllChildren();

        for (PrivateSection section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            NodeContext context = (NodeContext) node.getUserObject();
            context.setLabel(String.format("时间：%s",
                                           translateTimepoint2Local(getFieldValue(syntax, "UTC_time"))));
            groupTOT.add(node);
            nodeSectionMap.put(node, section);
        }

        groupTOT.setUserObject(String.format("TOT (%d)", groupTOT.getChildCount()));
    }

    private void addUserPrivateSectionNodes(List<PrivateSection> sections)
    {
        Map<String, DefaultMutableTreeNode> namedGroups = new HashMap<>();
        DefaultMutableTreeNode defaultGroup = new DefaultMutableTreeNode();

        for (PrivateSection section : sections)
        {
            try
            {
                Encoding encoding = Encoding.wrap(section.getEncoding());
                SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
                if (node == null)
                    continue;

                NodeContext context = (NodeContext) node.getUserObject();
                context.setLabel(String.format("[PS]%s @ 0x%X:%d",
                                               node.getUserObject(),
                                               section.getPid(),
                                               section.getPosition()));

                DefaultMutableTreeNode group = (syntax.getGroup() == null)
                                               ? defaultGroup
                                               : namedGroups.computeIfAbsent(syntax.getGroup(),
                                                                             any -> new DefaultMutableTreeNode(syntax.getGroup()));
                group.add(node);
                nodeSectionMap.put(node, section);
            } catch (Exception ex)
            {
                log.error("解码私有段时出现异常：{}", ex.getMessage());
            }
        }

        List<String> groupNames = new ArrayList<>(namedGroups.keySet());
        groupNames.sort(Comparator.naturalOrder());

        groupPrivate.removeAllChildren();
        for (String name : groupNames)
        {
            DefaultMutableTreeNode groupNode = namedGroups.get(name);
            groupNode.setUserObject(String.format("%s（%d）", name, groupNode.getChildCount()));
            groupPrivate.add(groupNode);
        }

        if (!defaultGroup.isLeaf())
        {
            defaultGroup.setUserObject(String.format("未命名分组（%d）", defaultGroup.getChildCount()));
            groupPrivate.add(defaultGroup);
        }
    }

    private long getFieldValue(SyntaxField syntax, String name)
    {
        SyntaxField field = syntax.findLastChild(name);
        return (field != null) ? field.getValueAsLong() : 0;
    }

    private String translateTimepoint2Local(long timepoint)
    {
        return DVB.decodeTimepointIntoLocalDateTime(timepoint).format(timeFormatter) +
               " [" + ZoneId.systemDefault().getId() + ']';
    }

    class SectionDatagramTreeCellRenderer extends DefaultTreeCellRenderer
    {
        final Icon GROUP = FontIcon.of(FluentUiFilledAL.GROUP_24, 20, Color.decode("#89D3DF"));
        final Icon TABLE = FontIcon.of(FluentUiFilledMZ.TABLE_24, 20, Color.decode("#89D3DF"));
        final Icon DATA = FontIcon.of(FluentUiFilledMZ.TEXT_BULLET_LIST_TREE_24, 20, Color.decode("#89D3DF"));
        final Icon DOT = FontIcon.of(FluentUiFilledAL.CIRCLE_SMALL_24, 20, Color.decode("#89D3DF"));

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            boolean isPrivateSectionNode = false;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof NodeContext context)
            {
                String text = context.getLabel();
                isPrivateSectionNode = text.startsWith("[PS]");
                if (isPrivateSectionNode)
                    text = text.substring("[PS]".length());
                setText(text);
                setToolTipText(text);
            }

            if (value == root)
                return this;

            if (root.isNodeChild(node))
                setIcon(GROUP);
            else if (groupPSI.isNodeChild(node) ||
                     groupSI.isNodeChild(node) ||
                     groupPrivate.isNodeChild(node))
                setIcon(TABLE);
            else if (groupPAT.isNodeChild(node) ||
                     groupCAT.isNodeChild(node) ||
                     groupPMT.isNodeChild(node) ||
                     groupBAT.isNodeChild(node) ||
                     groupNITActual.isNodeChild(node) || groupNITOther.isNodeChild(node) ||
                     groupSDTActual.isNodeChild(node) || groupSDTOther.isNodeChild(node) ||
                     groupEITPFActual.isNodeChild(node) || groupEITPFOther.isNodeChild(node) ||
                     groupEITScheduleActual.isNodeChild(node) || groupEITScheduleOther.isNodeChild(node) ||
                     groupTDT.isNodeChild(node) ||
                     groupTOT.isNodeChild(node) ||
                     isPrivateSectionNode)
                setIcon(DATA);
            else
                setIcon(DOT);

            return this;
        }
    }
}
