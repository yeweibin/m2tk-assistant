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

import cn.hutool.core.util.StrUtil;
import m2tk.assistant.api.domain.CASystemStream;
import m2tk.assistant.api.presets.CASystems;
import m2tk.util.Bytes;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class CASystemInfoPanel extends JPanel
{
    private JTree tree;
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private final List<CASystemStream> currentStreams;

    public CASystemInfoPanel()
    {
        currentStreams = new ArrayList<>();
        initUI();
    }

    private void initUI()
    {
        root = new DefaultMutableTreeNode("/");
        model = new DefaultTreeModel(root);

        tree = new JTree(model);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new CAStreamTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(tree);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.putClientProperty("FlatLaf.style",
                                     """
                                     borderWidth: 0.75;
                                     focusWidth: 0; innerFocusWidth: 0.5; innerOutlineWidth: 0.5;
                                     """);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateStreamList(List<CASystemStream> streams)
    {
        if (isSame(currentStreams, streams))
            return;

        root.removeAllChildren();
        Map<Integer, List<CASystemStream>> groups = streams.stream()
                                                           .collect(groupingBy(CASystemStream::getSystemId));
        List<Integer> systemIds = new ArrayList<>(groups.keySet());
        systemIds.sort(Comparator.naturalOrder());

        for (Integer systemId : systemIds)
        {
            root.add(createCASystemNode(systemId, groups.get(systemId)));
        }
        tree.expandPath(new TreePath(root));
        model.reload();

        currentStreams.clear();
        currentStreams.addAll(streams);
    }

    private boolean isSame(List<CASystemStream> current, List<CASystemStream> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparingInt(CASystemStream::getStreamPid));

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            CASystemStream s1 = current.get(i);
            CASystemStream s2 = incoming.get(i);

            if (s1.getSystemId() != s2.getSystemId() ||
                s1.getStreamPid() != s2.getStreamPid() ||
                s1.getStreamType() != s2.getStreamType())
                return false;
        }

        return true;
    }

    private DefaultMutableTreeNode createCASystemNode(int systemId, List<CASystemStream> streams)
    {
        String vendor = StrUtil.emptyToDefault(CASystems.vendor(systemId), "未知提供商");
        String text = String.format("[CAS]%s（系统号：%04X）", vendor, systemId);

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(text);

        streams.sort((s1, s2) -> {
            if (s1.getStreamType() == s2.getStreamType())
                return Integer.compare(s1.getStreamPid(), s2.getStreamPid());
            else
                return Integer.compare(s1.getStreamType(), s2.getStreamType());
        });

        for (CASystemStream stream : streams)
        {
            String type = stream.getStreamType() == CASystemStream.TYPE_ECM ? "ECM" : "EMM";
            text = String.format("[%s]%s：0x%04X", type, type, stream.getStreamPid());
            if (type.equals("ECM"))
            {
                text += String.format("，关联节目：%d", stream.getProgramNumber());
                if (1 < stream.getElementaryStreamPid() && stream.getElementaryStreamPid() < 8191)
                    text += String.format("，关联基本流：%d", stream.getElementaryStreamPid());
            }

            DefaultMutableTreeNode nodeStream = new DefaultMutableTreeNode();
            nodeStream.setUserObject(text);
            node.add(nodeStream);

            byte[] privateData = stream.getStreamPrivateData();
            if (privateData != null && privateData.length > 0)
            {
                text = "[PD][" + Bytes.toHexStringPrettyPrint(privateData).toUpperCase() + "]";
                DefaultMutableTreeNode nodeData = new DefaultMutableTreeNode();
                nodeData.setUserObject(text);
                nodeStream.add(nodeData);
            }
        }

        return node;
    }

    class CAStreamTreeCellRenderer extends DefaultTreeCellRenderer
    {
        final Color ORANGE = Color.decode("#F25022");
        final Color YELLOW = Color.decode("#FCAF45");
        final Color LIGHT_BLUE = Color.decode("#89D3DF");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value != root)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                String text = (String) node.getUserObject();
                if (text.startsWith("[CAS]"))
                {
                    text = text.substring("[CAS]".length());
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[EMM]"))
                {
                    text = text.substring("[EMM]".length());
                    setIcon(FontIcon.of(FluentUiFilledMZ.PAYMENT_24, 20, ORANGE));
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[ECM]"))
                {
                    text = text.substring("[ECM]".length());
                    setIcon(FontIcon.of(FluentUiFilledAL.KEY_24, 20, YELLOW));
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[PD]"))
                {
                    text = text.substring("[PD]".length());
                    setIcon(FontIcon.of(FluentUiFilledMZ.SLIDE_TEXT_24, 20, LIGHT_BLUE));
                    setText(text);
                    setToolTipText(text);
                } else
                {
                    setIcon(null);
                    setText(null);
                    setToolTipText(null);
                }
            }

            return this;
        }
    }
}
