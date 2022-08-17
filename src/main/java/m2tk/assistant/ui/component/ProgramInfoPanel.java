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

import cn.hutool.core.util.StrUtil;
import m2tk.assistant.SmallIcons;
import m2tk.assistant.analyzer.domain.CASystemStream;
import m2tk.assistant.analyzer.domain.ElementaryStream;
import m2tk.assistant.analyzer.domain.MPEGProgram;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ProgramInfoPanel extends JPanel
{
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private JTree tree;
    private final List<MPEGProgram> currentPrograms;

    public ProgramInfoPanel()
    {
        currentPrograms = new ArrayList<>();
        initUI();
    }

    private void initUI()
    {
        root = new DefaultMutableTreeNode("/");
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ProgramTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(tree);

        setLayout(new BorderLayout());
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void resetProgramList()
    {
        root.removeAllChildren();
        model.reload();
        currentPrograms.clear();
    }

    public void updateProgramList(List<MPEGProgram> programs)
    {
        if (isSame(currentPrograms, programs))
            return;

        root.removeAllChildren();
        for (MPEGProgram program : programs)
        {
            root.add(createProgramNode(program));
        }
        tree.expandPath(new TreePath(root));
        model.reload();

        currentPrograms.clear();
        currentPrograms.addAll(programs);
    }

    private boolean isSame(List<MPEGProgram> current, List<MPEGProgram> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparingInt(MPEGProgram::getProgramNumber));

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            MPEGProgram p1 = current.get(i);
            MPEGProgram p2 = incoming.get(i);

            if (p1.getTransportStreamId() != p2.getTransportStreamId() ||
                p1.getProgramNumber() != p2.getProgramNumber() ||
                p1.getPmtVersion() != p2.getPmtVersion() ||
                !Objects.equals(p1.getProgramName(), p2.getProgramName()))
                return false;

            if (p1.getEcmList().size() != p2.getEcmList().size() ||
                p1.getElementList().size() != p2.getElementList().size())
                return false;

            int m = p1.getEcmList().size();
            for (int j = 0; j < m; j++)
            {
                CASystemStream ecm1 = p1.getEcmList().get(j);
                CASystemStream ecm2 = p2.getEcmList().get(j);
                if (ecm1.getStreamPid() != ecm2.getStreamPid())
                    return false;
            }

            m = p1.getElementList().size();
            for (int j = 0; j < m; j++)
            {
                ElementaryStream es1 = p1.getElementList().get(j);
                ElementaryStream es2 = p2.getElementList().get(j);
                if (es1.getStreamPid() != es2.getStreamPid() ||
                    es1.getPacketCount() != es2.getPacketCount())
                    return false;
            }
        }

        return true;
    }

    private DefaultMutableTreeNode createProgramNode(MPEGProgram program)
    {
        String text = String.format("节目号：%d（PMT：0x%04X",
                                    program.getProgramNumber(),
                                    program.getPmtPid());
        if (StrUtil.isEmpty(program.getProgramName()))
            text += "）";
        else
            text += " 节目名称：" + program.getProgramName() + "）";
        if (program.isFreeAccess())
            text = "[P]" + text;
        else
            text = "[P*]" + text;

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(text);

        text = "[BW]带宽：" + formatBitrate(program.getBandwidth());
        DefaultMutableTreeNode nodeBW = new DefaultMutableTreeNode();
        nodeBW.setUserObject(text);
        node.add(nodeBW);

        for (CASystemStream ecm : program.getEcmList())
        {
            text = String.format("[ECM]PID：0x%04X，%s",
                                 ecm.getStreamPid(),
                                 ecm.getStreamDescription());

            DefaultMutableTreeNode nodeECM = new DefaultMutableTreeNode();
            nodeECM.setUserObject(text);
            node.add(nodeECM);
        }

        for (ElementaryStream es : program.getElementList())
        {
            text = String.format("[%s]PID：0x%04X，%s",
                                 es.getCategory(),
                                 es.getStreamPid(),
                                 es.getDescription());
            if (es.isScrambled())
                text += "，加密";
            if (!es.isPresent())
                text += "（未出现）";

            DefaultMutableTreeNode nodeES = new DefaultMutableTreeNode();
            nodeES.setUserObject(text);
            node.add(nodeES);
        }

        if (program.getPcrPid() != 0x1fff)
        {
            text = String.format("[PCR]PCR：0x%04X", program.getPcrPid());
            DefaultMutableTreeNode nodePCR = new DefaultMutableTreeNode();
            nodePCR.setUserObject(text);
            node.add(nodePCR);
        }

        return node;
    }

    private String formatBitrate(int bps)
    {
        if (bps > 1000_000)
            return String.format("%.2f Mbps", 1.0d * bps / 1000000);
        if (bps > 1000)
            return String.format("%.2f Kbps", 1.0d * bps / 1000);
        else
            return bps + " bps";
    }

    class ProgramTreeCellRenderer extends DefaultTreeCellRenderer
    {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value != root)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                String text = (String) node.getUserObject();
                if (text.startsWith("[P]"))
                {
                    text = text.substring("[P]".length());
                    setIcon(SmallIcons.FILM);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[P*]"))
                {
                    text = text.substring("[P*]".length());
                    setIcon(SmallIcons.FILM_KEY);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[BW]"))
                {
                    text = text.substring("[BW]".length());
                    setIcon(SmallIcons.CHART_BAR);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[PCR]"))
                {
                    text = text.substring("[PCR]".length());
                    setIcon(SmallIcons.CLOCK);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[ECM]"))
                {
                    text = text.substring("[ECM]".length());
                    setIcon(SmallIcons.KEY);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[V]"))
                {
                    text = text.substring("[V]".length());
                    setIcon(SmallIcons.VIDEO);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[A]"))
                {
                    text = text.substring("[A]".length());
                    setIcon(SmallIcons.SOUND);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[D]"))
                {
                    text = text.substring("[D]".length());
                    setIcon(SmallIcons.TEXT);
                    setText(text);
                    setToolTipText(text);
                } else if (text.startsWith("[U]"))
                {
                    text = text.substring("[U]".length());
                    setIcon(SmallIcons.PAGE_WHITE);
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
