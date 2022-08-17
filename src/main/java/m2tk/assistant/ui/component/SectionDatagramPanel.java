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

import m2tk.assistant.LargeIcons;
import m2tk.assistant.SmallIcons;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.encoding.Encoding;
import m2tk.mpeg2.decoder.section.CATSectionDecoder;
import m2tk.mpeg2.decoder.section.PATSectionDecoder;
import m2tk.mpeg2.decoder.section.PMTSectionDecoder;
import m2tk.util.Bytes;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;

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
    private DefaultMutableTreeNode groupEMM;
    private JTree tree;
    private final List<SectionEntity> currentSections;

    public SectionDatagramPanel()
    {
        currentSections = new ArrayList<>();
        initUI();
    }

    private void initUI()
    {
        root = new DefaultMutableTreeNode("/");
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
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
        groupPrivate = new DefaultMutableTreeNode("私有数据");

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
        groupEMM = new DefaultMutableTreeNode("EMM");

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
        groupPrivate.add(groupEMM);
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
        addEMMSectionNodes(sectionGroups.getOrDefault("EMM", Collections.emptyList()));
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
        groupEMM.removeAllChildren();
        model.reload();
    }

    private void addPATSectionNodes(List<SectionEntity> sections)
    {
        PATSectionDecoder pat = new PATSectionDecoder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));
        for (SectionEntity section : sections)
        {
            pat.attach(Encoding.wrap(section.getEncoding()));
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            node.setUserObject(String.format("Section (transport_stream_id = %d, version_number = %d, section_number = %d)",
                                             pat.getTransportStreamID(), pat.getVersionNumber(), pat.getSectionNumber()));

            DefaultMutableTreeNode content = new DefaultMutableTreeNode();
            content.setUserObject(Bytes.toHexString(section.getEncoding()));

            node.add(content);
            groupPAT.add(node);
        }
    }

    private void addCATSectionNodes(List<SectionEntity> sections)
    {
        CATSectionDecoder cat = new CATSectionDecoder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));
        for (SectionEntity section : sections)
        {
            cat.attach(Encoding.wrap(section.getEncoding()));
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            node.setUserObject(String.format("Section (version_number = %d, section_number = %d)",
                                             cat.getVersionNumber(), cat.getSectionNumber()));

            DefaultMutableTreeNode content = new DefaultMutableTreeNode();
            content.setUserObject(Bytes.toHexString(section.getEncoding()));

            node.add(content);
            groupCAT.add(node);
        }
    }

    private void addPMTSectionNodes(List<SectionEntity> sections)
    {
        PMTSectionDecoder pmt = new PMTSectionDecoder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));
        for (SectionEntity section : sections)
        {
            pmt.attach(Encoding.wrap(section.getEncoding()));
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            node.setUserObject(String.format("Section (program_number = %d, version_number = %d)",
                                             pmt.getProgramNumber(), pmt.getVersionNumber()));

            DefaultMutableTreeNode content = new DefaultMutableTreeNode();
            content.setUserObject(Bytes.toHexString(section.getEncoding()));

            node.add(content);
            groupPMT.add(node);
        }
    }

    private void addBATSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addNITActualSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addNITOtherSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addSDTActualSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addSDTOtherSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addEITPFActualSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addEITPFOtherSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addEITScheduleActualSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addEITScheduleOtherSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addTDTSectionNodes(List<SectionEntity> sections)
    {
    }

    private void addEMMSectionNodes(List<SectionEntity> sections)
    {
    }

    class SectionDatagramTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value == groupPSI || value == groupSI || value == groupPrivate)
                setIcon(SmallIcons.NODE_TREE);
            else if (value == groupPAT || value == groupCAT || value == groupPMT ||
                     value == groupBAT || value == groupNITActual || value == groupNITOther ||
                     value == groupSDTActual || value == groupSDTOther ||
                     value == groupEITPFActual || value == groupEITPFOther ||
                     value == groupEITScheduleActual || value == groupEITScheduleOther ||
                     value == groupTDT || value == groupEMM)
                setIcon(SmallIcons.TABLE);
            else
                setIcon(SmallIcons.DOT_WHITE);
            return this;
        }
    }
}
