/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.ebs.component;

import m2tk.assistant.api.domain.PrivateSection;
import m2tk.assistant.api.template.RichTreeNodeSyntaxPresenter;
import m2tk.assistant.api.template.RichTreeNodeSyntaxPresenter.NodeContext;
import m2tk.assistant.api.template.SectionDecoder;
import m2tk.assistant.api.template.SyntaxField;
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
import java.util.List;
import java.util.*;

public class EBSectionDatagramPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode groupEBIndex;
    private DefaultMutableTreeNode groupEBContent;
    private DefaultMutableTreeNode groupEBCertAuth;
    private DefaultMutableTreeNode groupEBConfigure;

    private SectionDecoder decoder;
    private RichTreeNodeSyntaxPresenter presenter;
    private Map<TreeNode, PrivateSection> nodeSectionMap;

    public EBSectionDatagramPanel()
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

    public void update(Map<String, List<PrivateSection>> sectionGroups)
    {
        nodeSectionMap.clear();
        addEBIndexSectionNodes(sectionGroups.getOrDefault("EBSection.EBIndex", Collections.emptyList()));
        addEBContentSectionNodes(sectionGroups.getOrDefault("EBSection.EBContent", Collections.emptyList()));
        addEBCertAuthSectionNodes(sectionGroups.getOrDefault("EBSection.EBCertAuth", Collections.emptyList()));
        addEBConfigureSectionNodes(sectionGroups.getOrDefault("EBSection.EBConfigure", Collections.emptyList()));
        model.reload();
    }

    private void addEBIndexSectionNodes(List<PrivateSection> sections)
    {
        groupEBIndex.removeAllChildren();

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
            groupEBIndex.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEBIndex.setUserObject(String.format("EBIndex (%d)", groupEBIndex.getChildCount()));
    }

    private void addEBContentSectionNodes(List<PrivateSection> sections)
    {
        groupEBContent.removeAllChildren();

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
            groupEBContent.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEBContent.setUserObject(String.format("EBContent (%d)", groupEBContent.getChildCount()));
    }

    private void addEBCertAuthSectionNodes(List<PrivateSection> sections)
    {
        groupEBCertAuth.removeAllChildren();

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
            groupEBCertAuth.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEBCertAuth.setUserObject(String.format("EBCertAuth (%d)", groupEBCertAuth.getChildCount()));
    }

    private void addEBConfigureSectionNodes(List<PrivateSection> sections)
    {
        groupEBConfigure.removeAllChildren();

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
            groupEBConfigure.add(node);
            nodeSectionMap.put(node, section);
        }

        groupEBConfigure.setUserObject(String.format("EBConfigure (%d)", groupEBConfigure.getChildCount()));
    }

    private long getFieldValue(SyntaxField syntax, String name)
    {
        SyntaxField field = syntax.findLastChild(name);
        return (field != null) ? field.getValueAsLong() : 0;
    }

    class SectionDatagramTreeCellRenderer extends DefaultTreeCellRenderer
    {
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

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof NodeContext context)
            {
                String text = context.getLabel();
                setText(text);
                setToolTipText(text);
            }

            if (value == root)
                return this;

            if (root.isNodeChild(node))
                setIcon(TABLE);
            else if (groupEBIndex.isNodeChild(node) ||
                     groupEBContent.isNodeChild(node) ||
                     groupEBCertAuth.isNodeChild(node) ||
                     groupEBConfigure.isNodeChild(node))
                setIcon(DATA);
            else
                setIcon(DOT);

            return this;
        }
    }
}
