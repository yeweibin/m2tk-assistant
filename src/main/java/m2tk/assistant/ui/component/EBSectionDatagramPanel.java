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
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EBSectionDatagramPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode groupEBIndex;
    private DefaultMutableTreeNode groupEBContent;
    private DefaultMutableTreeNode groupEBCertAuth;
    private DefaultMutableTreeNode groupEBConfigure;

    public EBSectionDatagramPanel()
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
        groupEBIndex = new DefaultMutableTreeNode("EBIndex");
        groupEBContent = new DefaultMutableTreeNode("EBContent");
        groupEBCertAuth = new DefaultMutableTreeNode("EBCertAuth");
        groupEBConfigure = new DefaultMutableTreeNode("EBConfigure");

        root.add(groupEBIndex);
        root.add(groupEBContent);
        root.add(groupEBCertAuth);
        root.add(groupEBConfigure);
        model.reload();
    }

    public void update(Map<String, List<SectionEntity>> sectionGroups)
    {
        addEBIndexSectionNodes(sectionGroups.getOrDefault("EBIndex", Collections.emptyList()));
        addEBContentSectionNodes(sectionGroups.getOrDefault("EBContent", Collections.emptyList()));
        addEBCertAuthSectionNodes(sectionGroups.getOrDefault("EBCertAuth", Collections.emptyList()));
        addEBConfigureSectionNodes(sectionGroups.getOrDefault("EBConfigure", Collections.emptyList()));
        model.reload();
    }

    public void reset()
    {
        groupEBIndex.removeAllChildren();
        groupEBContent.removeAllChildren();
        groupEBCertAuth.removeAllChildren();
        groupEBConfigure.removeAllChildren();
        model.reload();
    }

    private void addEBIndexSectionNodes(List<SectionEntity> sections)
    {
        EBIndexSectionNodeBuilder builder = new EBIndexSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEBIndex.removeAllChildren();
        for (SectionEntity section : sections)
            groupEBIndex.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEBIndex.setUserObject(String.format("EBIndex（%d）", groupEBIndex.getChildCount()));
    }

    private void addEBContentSectionNodes(List<SectionEntity> sections)
    {
        EBContentSectionNodeBuilder builder = new EBContentSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEBContent.removeAllChildren();
        for (SectionEntity section : sections)
            groupEBContent.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEBContent.setUserObject(String.format("EBContent（%d）", groupEBContent.getChildCount()));
    }

    private void addEBCertAuthSectionNodes(List<SectionEntity> sections)
    {
        EBCertAuthSectionNodeBuilder builder = new EBCertAuthSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEBCertAuth.removeAllChildren();
        for (SectionEntity section : sections)
            groupEBCertAuth.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEBCertAuth.setUserObject(String.format("EBCertAuth（%d）", groupEBCertAuth.getChildCount()));
    }

    private void addEBConfigureSectionNodes(List<SectionEntity> sections)
    {
        EBConfigureSectionNodeBuilder builder = new EBConfigureSectionNodeBuilder();
        sections.sort(Comparator.comparing(SectionEntity::getPosition));

        groupEBConfigure.removeAllChildren();
        for (SectionEntity section : sections)
            groupEBConfigure.add(builder.build(Encoding.wrap(section.getEncoding())));
        groupEBConfigure.setUserObject(String.format("EBConfigure（%d）", groupEBConfigure.getChildCount()));
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

            if (node == groupEBIndex || node == groupEBContent || node == groupEBCertAuth || node == groupEBConfigure)
                setIcon(SmallIcons.TABLE);
            else
                setIcon(SmallIcons.DOT_ORANGE);

            return this;
        }
    }
}
