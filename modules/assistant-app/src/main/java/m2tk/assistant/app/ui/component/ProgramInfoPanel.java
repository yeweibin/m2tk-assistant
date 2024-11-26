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
import m2tk.assistant.api.domain.ElementaryStream;
import m2tk.assistant.api.domain.MPEGProgram;
import m2tk.assistant.api.presets.CASystems;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.assistant.app.ui.util.FormatUtil;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ProgramInfoPanel extends JPanel
{
    private JTree tree;
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private BiConsumer<MouseEvent, MPEGProgram> popupListener;
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
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ProgramTreeCellRenderer());
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    TreePath path = tree.getSelectionPath();
                    if (path == null)
                        return;

                    Object[] nodes = path.getPath();
                    if (nodes.length > 1 && popupListener != null)
                    {
                        try
                        {
                            int idx = root.getIndex((TreeNode) nodes[1]);
                            popupListener.accept(e, currentPrograms.get(idx));
                        } catch (Exception ignored)
                        {
                        }
                    }
                }
            }
        });

        ToolTipManager.sharedInstance().registerComponent(tree);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.putClientProperty("FlatLaf.style",
                                     """
                                     arc: 10;
                                     borderWidth: 0.75;
                                     focusWidth: 0; innerFocusWidth: 0.5; innerOutlineWidth: 0.5;
                                     """);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setPopupListener(BiConsumer<MouseEvent, MPEGProgram> listener)
    {
        popupListener = listener;
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
                !Objects.equals(p1.getName(), p2.getName()))
                return false;

            if (p1.getEcmStreams().size() != p2.getEcmStreams().size() ||
                p1.getElementaryStreams().size() != p2.getElementaryStreams().size())
                return false;

            int m = p1.getEcmStreams().size();
            for (int j = 0; j < m; j++)
            {
                CASystemStream ecm1 = p1.getEcmStreams().get(j);
                CASystemStream ecm2 = p2.getEcmStreams().get(j);
                if (ecm1.getStreamPid() != ecm2.getStreamPid())
                    return false;
            }

            m = p1.getElementaryStreams().size();
            for (int j = 0; j < m; j++)
            {
                ElementaryStream es1 = p1.getElementaryStreams().get(j);
                ElementaryStream es2 = p2.getElementaryStreams().get(j);
                if (es1.getStreamPid() != es2.getStreamPid() ||
                    es1.getPacketCount() != es2.getPacketCount())
                    return false;
            }
        }

        return true;
    }

    private DefaultMutableTreeNode createProgramNode(MPEGProgram program)
    {
        String text = String.format("%s（节目号：%d）",
                                    StrUtil.isEmpty(program.getName()) ? "未命名节目" : program.getName(),
                                    program.getProgramNumber());
        if (program.isFreeAccess())
            text = "[P]" + text;
        else
            text = "[P*]" + text;

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(text);

        text = "[BW]带宽：" + FormatUtil.formatBitrate(program.getBandwidth());
        DefaultMutableTreeNode nodeBW = new DefaultMutableTreeNode();
        nodeBW.setUserObject(text);
        node.add(nodeBW);

        text = String.format("[PMT]PMT：0x%04X", program.getPmtPid());
        DefaultMutableTreeNode nodePMT = new DefaultMutableTreeNode();
        nodePMT.setUserObject(text);
        node.add(nodePMT);

        if (program.getPcrPid() != 0x1fff)
        {
            text = String.format("[PCR]PCR：0x%04X", program.getPcrPid());
            DefaultMutableTreeNode nodePCR = new DefaultMutableTreeNode();
            nodePCR.setUserObject(text);
            node.add(nodePCR);
        }

        for (CASystemStream ecm : program.getEcmStreams())
        {
            String vendorName = CASystems.vendor(ecm.getSystemId());
            if (vendorName.isEmpty())
                vendorName = String.format("系统号：%04X", ecm.getSystemId());
            text = String.format("[ECM]PID：0x%04X，%s", ecm.getStreamPid(), vendorName);
            DefaultMutableTreeNode nodeECM = new DefaultMutableTreeNode();
            nodeECM.setUserObject(text);
            node.add(nodeECM);
        }

        for (ElementaryStream es : program.getElementaryStreams())
        {
            text = String.format("[%s]PID：0x%04X，%s",
                                 es.getCategory(),
                                 es.getStreamPid(),
                                 StreamTypes.description(es.getStreamType()));
            if (es.isScrambled())
                text += "，加密";
            if (es.getPacketCount() == 0)
                text += "（未出现）";

            DefaultMutableTreeNode nodeES = new DefaultMutableTreeNode();
            nodeES.setUserObject(text);
            node.add(nodeES);
        }

        return node;
    }

    class ProgramTreeCellRenderer extends DefaultTreeCellRenderer
    {
        final Color GREEN = Color.decode("#7FBA00");
        final Color RED = Color.decode("#FD1D1D");
        final Color ORANGE = Color.decode("#F25022");
        final Color YELLOW = Color.decode("#FCAF45");
        final Color LIGHT_BLUE = Color.decode("#89D3DF");
        final Color BRIGHT_BLUE = Color.decode("#4285F4");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value == root)
                return this;

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            String text = (String) node.getUserObject();
            if (text.startsWith("[P]"))
            {
                text = text.substring("[P]".length());
                setIcon(FontIcon.of(FluentUiFilledAL.EYE_SHOW_24, 20, GREEN));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[P*]"))
            {
                text = text.substring("[P*]".length());
                setIcon(FontIcon.of(FluentUiFilledAL.EYE_HIDE_24, 20, RED));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[BW]"))
            {
                text = text.substring("[BW]".length());
                setIcon(FontIcon.of(FluentUiFilledAL.DATA_HISTOGRAM_24, 20, BRIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[PMT]"))
            {
                text = text.substring("[PMT]".length());
                setIcon(FontIcon.of(FluentUiFilledMZ.TABLE_24, 20, BRIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[PCR]"))
            {
                text = text.substring("[PCR]".length());
                setIcon(FontIcon.of(FluentUiFilledAL.CLOCK_24, 20, BRIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[ECM]"))
            {
                text = text.substring("[ECM]".length());
                setIcon(FontIcon.of(FluentUiFilledAL.KEY_24, 20, YELLOW));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[Video]"))
            {
                text = text.substring("[Video]".length());
                setIcon(FontIcon.of(FluentUiFilledMZ.VIDEO_24, 20, LIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[Audio]"))
            {
                text = text.substring("[Audio]".length());
                setIcon(FontIcon.of(FluentUiFilledMZ.SPEAKER_24, 20, LIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[Data]"))
            {
                text = text.substring("[Data]".length());
                setIcon(FontIcon.of(FluentUiFilledMZ.WINDOW_20, 20, LIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else if (text.startsWith("[UserPrivate]"))
            {
                text = text.substring("[UserPrivate]".length());
                setIcon(FontIcon.of(FluentUiFilledMZ.SLIDE_TEXT_24, 20, LIGHT_BLUE));
                setText(text);
                setToolTipText(text);
            } else
            {
                setIcon(null);
                setText(null);
                setToolTipText(null);
            }

            return this;
        }
    }
}
