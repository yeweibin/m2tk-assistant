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
import m2tk.assistant.ui.builder.section.*;
import m2tk.encoding.Encoding;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SectionDatagramPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
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
    private DefaultMutableTreeNode groupEMMPersonal;
    private DefaultMutableTreeNode groupEMMGlobal;
    private DefaultMutableTreeNode groupEMMActive;
    private DefaultMutableTreeNode groupUserPrivate;

    private Map<String, Set<TreeNode>> groups;

    public SectionDatagramPanel()
    {
        initUI();
    }

    private void initUI()
    {
        root = new DefaultMutableTreeNode("/");
        model = new DefaultTreeModel(root);

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
        DefaultMutableTreeNode groupPSI = new DefaultMutableTreeNode("PSI");
        DefaultMutableTreeNode groupSI = new DefaultMutableTreeNode("SI");
        DefaultMutableTreeNode groupPrivate = new DefaultMutableTreeNode("私有数据");

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
        groupEMMPersonal = new DefaultMutableTreeNode("个人EMM");
        groupEMMGlobal = new DefaultMutableTreeNode("全局EMM");
        groupEMMActive = new DefaultMutableTreeNode("激活EMM");
        groupUserPrivate = new DefaultMutableTreeNode("其他");

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
        groupPrivate.add(groupEMMPersonal);
        groupPrivate.add(groupEMMGlobal);
        groupPrivate.add(groupEMMActive);
        groupPrivate.add(groupUserPrivate);

        groups = Map.of("first-class", Set.of(groupPSI, groupSI, groupPrivate),
                        "second-class", Set.of(groupPAT, groupCAT, groupPMT, groupBAT,
                                               groupNITActual, groupNITOther,
                                               groupSDTActual, groupSDTOther,
                                               groupEITPFActual, groupEITPFOther,
                                               groupEITScheduleActual, groupEITScheduleOther,
                                               groupTDT, groupTOT,
                                               groupEMMPersonal, groupEMMGlobal, groupEMMActive, groupUserPrivate));

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
        addEMMPersonalSectionNodes(sectionGroups.getOrDefault("EMM.Personal", Collections.emptyList()));
        addEMMGlobalSectionNodes(sectionGroups.getOrDefault("EMM.Global", Collections.emptyList()));
        addEMMActiveSectionNodes(sectionGroups.getOrDefault("EMM.Active", Collections.emptyList()));
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
        groupEMMPersonal.removeAllChildren();
        groupEMMGlobal.removeAllChildren();
        groupEMMActive.removeAllChildren();
        groupUserPrivate.removeAllChildren();
        model.reload();
    }

    private void addPATSectionNodes(List<SectionEntity> sections)
    {
        PATNodeBuilder builder = new PATNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupPAT.removeAllChildren();
        for (SectionEntity section : sections)
            groupPAT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupPAT.setUserObject(String.format("PAT（%d）", groupPAT.getChildCount()));
    }

    private void addCATSectionNodes(List<SectionEntity> sections)
    {
        CATNodeBuilder builder = new CATNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupCAT.removeAllChildren();
        for (SectionEntity section : sections)
            groupCAT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupCAT.setUserObject(String.format("CAT（%d）", groupCAT.getChildCount()));
    }

    private void addPMTSectionNodes(List<SectionEntity> sections)
    {
        PMTNodeBuilder builder = new PMTNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupPMT.removeAllChildren();
        for (SectionEntity section : sections)
            groupPMT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupPMT.setUserObject(String.format("PMT（%d）", groupPMT.getChildCount()));
    }

    private void addBATSectionNodes(List<SectionEntity> sections)
    {
        BATNodeBuilder builder = new BATNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupBAT.removeAllChildren();
        for (SectionEntity section : sections)
            groupBAT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupBAT.setUserObject(String.format("BAT（%d）", groupBAT.getChildCount()));
    }

    private void addNITActualSectionNodes(List<SectionEntity> sections)
    {
        NITNodeBuilder builder = new NITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupNITActual.removeAllChildren();
        for (SectionEntity section : sections)
            groupNITActual.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupNITActual.setUserObject(String.format("NIT_Actual（%d）", groupNITActual.getChildCount()));
    }

    private void addNITOtherSectionNodes(List<SectionEntity> sections)
    {
        NITNodeBuilder builder = new NITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupNITOther.removeAllChildren();
        for (SectionEntity section : sections)
            groupNITOther.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupNITOther.setUserObject(String.format("NIT_Other（%d）", groupNITOther.getChildCount()));
    }

    private void addSDTActualSectionNodes(List<SectionEntity> sections)
    {
        SDTNodeBuilder builder = new SDTNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupSDTActual.removeAllChildren();
        for (SectionEntity section : sections)
            groupSDTActual.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupSDTActual.setUserObject(String.format("SDT_Actual（%d）", groupSDTActual.getChildCount()));
    }

    private void addSDTOtherSectionNodes(List<SectionEntity> sections)
    {
        SDTNodeBuilder builder = new SDTNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupSDTOther.removeAllChildren();
        for (SectionEntity section : sections)
            groupSDTOther.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupSDTOther.setUserObject(String.format("SDT_Other（%d）", groupSDTOther.getChildCount()));
    }

    private void addEITPFActualSectionNodes(List<SectionEntity> sections)
    {
        EITNodeBuilder builder = new EITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEITPFActual.removeAllChildren();
        for (SectionEntity section : sections)
            groupEITPFActual.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEITPFActual.setUserObject(String.format("EIT_PF_Actual（%d）", groupEITPFActual.getChildCount()));
    }

    private void addEITPFOtherSectionNodes(List<SectionEntity> sections)
    {
        EITNodeBuilder builder = new EITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEITPFOther.removeAllChildren();
        for (SectionEntity section : sections)
            groupEITPFOther.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEITPFOther.setUserObject(String.format("EIT_PF_Other（%d）", groupEITPFOther.getChildCount()));
    }

    private void addEITScheduleActualSectionNodes(List<SectionEntity> sections)
    {
        EITNodeBuilder builder = new EITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEITScheduleActual.removeAllChildren();
        for (SectionEntity section : sections)
            groupEITScheduleActual.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEITScheduleActual.setUserObject(String.format("EIT_Schedule_Actual（%d）", groupEITScheduleActual.getChildCount()));
    }

    private void addEITScheduleOtherSectionNodes(List<SectionEntity> sections)
    {
        EITNodeBuilder builder = new EITNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEITScheduleOther.removeAllChildren();
        for (SectionEntity section : sections)
            groupEITScheduleOther.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEITScheduleOther.setUserObject(String.format("EIT_Schedule_Other（%d）", groupEITScheduleOther.getChildCount()));
    }

    private void addTDTSectionNodes(List<SectionEntity> sections)
    {
        TDTNodeBuilder builder = new TDTNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupTDT.removeAllChildren();
        for (SectionEntity section : sections)
            groupTDT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupTDT.setUserObject(String.format("TDT（%d）", groupTDT.getChildCount()));
    }

    private void addTOTSectionNodes(List<SectionEntity> sections)
    {
        TOTNodeBuilder builder = new TOTNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupTOT.removeAllChildren();
        for (SectionEntity section : sections)
            groupTOT.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupTOT.setUserObject(String.format("TOT（%d）", groupTOT.getChildCount()));
    }

    private void addEMMPersonalSectionNodes(List<SectionEntity> sections)
    {
        PrivateSectionNodeBuilder builder = new PrivateSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEMMPersonal.removeAllChildren();
        for (SectionEntity section : sections)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) builder.build(Encoding.wrap(section.getEncoding()));
            node.setUserObject(String.format("%s @ pid = 0x%X",
                                             node.getUserObject(),
                                             section.getStream()));
            groupEMMPersonal.add(node);
        }
        groupEMMPersonal.setUserObject(String.format("个人EMM（%d）", groupEMMPersonal.getChildCount()));
    }

    private void addEMMGlobalSectionNodes(List<SectionEntity> sections)
    {
        PrivateSectionNodeBuilder builder = new PrivateSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEMMGlobal.removeAllChildren();
        for (SectionEntity section : sections)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) builder.build(Encoding.wrap(section.getEncoding()));
            node.setUserObject(String.format("%s @ pid = 0x%X",
                                             node.getUserObject(),
                                             section.getStream()));
            groupEMMGlobal.add(node);
        }
        groupEMMGlobal.setUserObject(String.format("全局EMM（%d）", groupEMMGlobal.getChildCount()));
    }

    private void addEMMActiveSectionNodes(List<SectionEntity> sections)
    {
        PrivateSectionNodeBuilder builder = new PrivateSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEMMActive.removeAllChildren();
        for (SectionEntity section : sections)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) builder.build(Encoding.wrap(section.getEncoding()));
            node.setUserObject(String.format("%s @ pid = 0x%X",
                                             node.getUserObject(),
                                             section.getStream()));
            groupEMMActive.add(node);
        }
        groupEMMActive.setUserObject(String.format("激活EMM（%d）", groupEMMActive.getChildCount()));
    }

    private void addUserPrivateSectionNodes(List<SectionEntity> sections)
    {
        PrivateSectionNodeBuilder builder = new PrivateSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupUserPrivate.removeAllChildren();
        for (SectionEntity section : sections)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) builder.build(Encoding.wrap(section.getEncoding()));
            node.setUserObject(String.format("%s @ pid = 0x%X",
                                             node.getUserObject(),
                                             section.getStream()));
            groupUserPrivate.add(node);
        }
        groupUserPrivate.setUserObject(String.format("其他（%d）", groupUserPrivate.getChildCount()));
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

            if (groups.get("first-class").contains(node))
                setIcon(SmallIcons.NODE_TREE);
            else if (groups.get("second-class").contains(node))
                setIcon(SmallIcons.TABLE);
            else
                setIcon(SmallIcons.DOT_ORANGE);

            return this;
        }
    }
}
