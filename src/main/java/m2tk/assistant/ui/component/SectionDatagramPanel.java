/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

package m2tk.assistant.ui.component;

import m2tk.assistant.SmallIcons;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.assistant.template.PlainTreeNodeSyntaxPresenter;
import m2tk.assistant.template.SectionDecoder;
import m2tk.assistant.template.SyntaxField;
import m2tk.dvb.DVB;
import m2tk.encoding.Encoding;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

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
    private PlainTreeNodeSyntaxPresenter presenter;

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
        presenter = new PlainTreeNodeSyntaxPresenter();

        JTree tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new SectionDatagramTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(tree);

        setLayout(new BorderLayout());
        add(new JScrollPane(tree), BorderLayout.CENTER);

        constructTreeSkeleton();
    }

    private void constructTreeSkeleton()
    {
        groupPSI = new DefaultMutableTreeNode("PSI");
        groupSI = new DefaultMutableTreeNode("SI");
        groupPrivate = new DefaultMutableTreeNode("UserPrivate");

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

    public void update(Map<String, List<SectionEntity>> sectionGroups)
    {
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

    public void reset()
    {
        groupPAT.removeAllChildren();
        groupCAT.removeAllChildren();
        groupPMT.removeAllChildren();
        groupBAT.removeAllChildren();
        groupNITActual.removeAllChildren();
        groupNITOther.removeAllChildren();
        groupSDTActual.removeAllChildren();
        groupSDTOther.removeAllChildren();
        groupEITPFActual.removeAllChildren();
        groupEITPFOther.removeAllChildren();
        groupEITScheduleActual.removeAllChildren();
        groupEITScheduleOther.removeAllChildren();
        groupTDT.removeAllChildren();
        groupTOT.removeAllChildren();
        groupPrivate.removeAllChildren();
        model.reload();
    }

    private void addPATSectionNodes(List<SectionEntity> sections)
    {
        groupPAT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 传输流号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "transport_stream_id")));
            groupPAT.add(node);
        }

        groupPAT.setUserObject(String.format("PAT (%d)", groupPAT.getChildCount()));
    }

    private void addCATSectionNodes(List<SectionEntity> sections)
    {
        groupCAT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x]",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number")));
            groupCAT.add(node);
        }

        groupCAT.setUserObject(String.format("CAT (%d)", groupCAT.getChildCount()));
    }

    private void addPMTSectionNodes(List<SectionEntity> sections)
    {
        groupPMT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 节目号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "program_number")));
            groupPMT.add(node);
        }

        groupPMT.setUserObject(String.format("PMT (%d)", groupPMT.getChildCount()));
    }

    private void addBATSectionNodes(List<SectionEntity> sections)
    {
        groupBAT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 业务群号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "bouquet_id")));
            groupBAT.add(node);
        }

        groupBAT.setUserObject(String.format("BAT (%d)", groupBAT.getChildCount()));
    }

    private void addNITActualSectionNodes(List<SectionEntity> sections)
    {
        groupNITActual.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 网络号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "network_id")));
            groupNITActual.add(node);
        }

        groupNITActual.setUserObject(String.format("NIT_Actual (%d)", groupNITActual.getChildCount()));
    }

    private void addNITOtherSectionNodes(List<SectionEntity> sections)
    {
        groupNITOther.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 网络号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "network_id")));
            groupNITOther.add(node);
        }

        groupNITOther.setUserObject(String.format("NIT_Other (%d)", groupNITOther.getChildCount()));
    }

    private void addSDTActualSectionNodes(List<SectionEntity> sections)
    {
        groupSDTActual.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 传输流号：%d，原始网络号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "transport_stream_id"),
                                             getFieldValue(syntax, "original_network_id")));
            groupSDTActual.add(node);
        }

        groupSDTActual.setUserObject(String.format("SDT_Actual (%d)", groupSDTActual.getChildCount()));
    }

    private void addSDTOtherSectionNodes(List<SectionEntity> sections)
    {
        groupSDTOther.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 传输流号：%d，原始网络号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "transport_stream_id"),
                                             getFieldValue(syntax, "original_network_id")));
            groupSDTOther.add(node);
        }

        groupSDTOther.setUserObject(String.format("SDT_Other (%d)", groupSDTOther.getChildCount()));
    }

    private void addEITPFActualSectionNodes(List<SectionEntity> sections)
    {
        groupEITPFActual.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 业务号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "service_id")));
            groupEITPFActual.add(node);
        }

        groupEITPFActual.setUserObject(String.format("EIT_PF_Actual (%d)", groupEITPFActual.getChildCount()));
    }

    private void addEITPFOtherSectionNodes(List<SectionEntity> sections)
    {
        groupEITPFOther.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 业务号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "service_id")));
            groupEITPFOther.add(node);
        }

        groupEITPFOther.setUserObject(String.format("EIT_PF_Other (%d)", groupEITPFOther.getChildCount()));
    }

    private void addEITScheduleActualSectionNodes(List<SectionEntity> sections)
    {
        groupEITScheduleActual.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 业务号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "service_id")));
            groupEITScheduleActual.add(node);
        }

        groupEITScheduleActual.setUserObject(String.format("EIT_Schedule_Actual (%d)", groupEITScheduleActual.getChildCount()));
    }

    private void addEITScheduleOtherSectionNodes(List<SectionEntity> sections)
    {
        groupEITScheduleOther.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("[V:%02x, S:%02x, L:%02x] 业务号：%d",
                                             getFieldValue(syntax, "version_number"),
                                             getFieldValue(syntax, "section_number"),
                                             getFieldValue(syntax, "last_section_number"),
                                             getFieldValue(syntax, "service_id")));
            groupEITScheduleOther.add(node);
        }

        groupEITScheduleOther.setUserObject(String.format("EIT_Schedule_Other (%d)", groupEITScheduleOther.getChildCount()));
    }

    private void addTDTSectionNodes(List<SectionEntity> sections)
    {
        groupTDT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("本地时间：%s",
                                             translateTimepoint2Local(getFieldValue(syntax, "UTC_time"))));
            groupTDT.add(node);
        }

        groupTDT.setUserObject(String.format("TDT (%d)", groupTDT.getChildCount()));
    }

    private void addTOTSectionNodes(List<SectionEntity> sections)
    {
        groupTOT.removeAllChildren();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("本地时间：%s",
                                             translateTimepoint2Local(getFieldValue(syntax, "UTC_time"))));
            groupTOT.add(node);
        }

        groupTOT.setUserObject(String.format("TOT (%d)", groupTOT.getChildCount()));
    }

    private void addUserPrivateSectionNodes(List<SectionEntity> sections)
    {
        Map<String, DefaultMutableTreeNode> namedGroups = new HashMap<>();
        DefaultMutableTreeNode defaultGroup = new DefaultMutableTreeNode();

        for (SectionEntity section : sections)
        {
            Encoding encoding = Encoding.wrap(section.getEncoding());
            SyntaxField syntax = decoder.decode(encoding, 0, encoding.size());
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) presenter.render(syntax);
            if (node == null)
                continue;

            node.setUserObject(String.format("%s @ 0x%X",
                                             node.getUserObject(),
                                             section.getStream()));

            DefaultMutableTreeNode group = (syntax.getGroup() == null)
                                           ? defaultGroup
                                           : namedGroups.computeIfAbsent(syntax.getGroup(),
                                                                         any -> new DefaultMutableTreeNode(syntax.getGroup()));
            group.add(node);
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
        return DVB.decodeTimepointIntoLocalDateTime(timepoint).format(timeFormatter);
    }

    class SectionDatagramTreeCellRenderer extends DefaultTreeCellRenderer
    {
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

            if (value == root)
                return this;

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            setToolTipText((String) node.getUserObject());

            if (root.isNodeChild(node))
                setIcon(SmallIcons.NODE_TREE);
            else if (groupPrivate.isNodeChild(node))
                setIcon(SmallIcons.TABLE);
            else
                setIcon(SmallIcons.DOT_ORANGE);

            return this;
        }
    }
}
