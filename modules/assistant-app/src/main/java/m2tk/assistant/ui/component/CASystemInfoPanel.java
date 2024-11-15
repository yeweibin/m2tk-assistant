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

package m2tk.assistant.ui.component;

import m2tk.assistant.SmallIcons;
import m2tk.assistant.core.domain.CASystemStream;
import m2tk.assistant.core.presets.CASystems;
import m2tk.assistant.kernel.entity.CAStreamEntity;
import m2tk.util.Bytes;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class CASystemInfoPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private JTree tree;
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
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new CAStreamTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(tree);

        setLayout(new BorderLayout());
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void resetStreamList()
    {
        root.removeAllChildren();
        model.reload();
    }

    public void updateStreamList(List<CASystemStream> streams)
    {
        if (streams == null || streams.isEmpty() || isSame(currentStreams, streams))
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

        incoming = new ArrayList<>(incoming);
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
        String text = String.format("[CAS]系统号：%04X", systemId);
        String vendor = CASystems.vendor(systemId);
        if (!vendor.isEmpty())
            text += "（" + vendor + "）";

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
            String type = stream.getStreamType() == CAStreamEntity.TYPE_ECM ? "ECM" : "EMM";
            text = String.format("[%s]%s：0x%04X", type, type, stream.getStreamPid());
            if (type.equals("ECM"))
            {
                text += String.format("，关联节目：%d", stream.getProgramNumber());
                if (stream.getElementaryStreamPid() != 8191)
                    text += String.format("，目标ES：%X", stream.getElementaryStreamPid());
            }

            DefaultMutableTreeNode nodeStream = new DefaultMutableTreeNode();
            nodeStream.setUserObject(text);
            node.add(nodeStream);

            byte[] privateData = stream.getStreamPrivateData();
            if (privateData != null && privateData.length > 0)
            {
                text = "[PD]" + Bytes.toHexString(privateData).toUpperCase();
                DefaultMutableTreeNode nodeData = new DefaultMutableTreeNode();
                nodeData.setUserObject(text);
                nodeStream.add(nodeData);
            }
        }

        return node;
    }

    class CAStreamTreeCellRenderer extends DefaultTreeCellRenderer
    {

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
                    setIcon(SmallIcons.SHIELD);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[EMM]"))
                {
                    text = text.substring("[EMM]".length());
                    setIcon(SmallIcons.LICENSE);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[ECM]"))
                {
                    text = text.substring("[ECM]".length());
                    setIcon(SmallIcons.KEY);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[PD]"))
                {
                    text = text.substring("[PD]".length());
                    setIcon(SmallIcons.COMPILE);
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
