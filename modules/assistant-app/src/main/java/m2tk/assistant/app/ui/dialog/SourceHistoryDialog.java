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

package m2tk.assistant.app.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SourceHistoryDialog extends JDialog
{
    private DefaultListModel<String> model;
    private JList<String> list;
    private String source;

    public SourceHistoryDialog(Frame parent)
    {
        super(parent);
        setupUI(parent);
    }

    public String selectFromSourceHistory()
    {
//        model.addAll(Global.getSourceHistory());
        setVisible(true);
        return source;
    }

    private void closeDialog()
    {
        source = null;
        setVisible(false);
        dispose();
    }

    private void openSource()
    {
        source = list.getSelectedValue();
        setVisible(false);
        dispose();
    }

    private void setupUI(Frame parent)
    {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int clicks = e.getClickCount();
                if (clicks > 1)
                    openSource();
            }
        });
        root.add(new JScrollPane(list), BorderLayout.CENTER);

        JButton open = new JButton("选择");
        open.addActionListener(e -> openSource());
        open.setEnabled(false);
        JButton close = new JButton("取消");
        close.addActionListener(e -> closeDialog());
        ButtonGroup group = new ButtonGroup();
        group.add(open);
        group.add(close);

        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        control.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        control.add(open);
        control.add(Box.createHorizontalGlue());
        control.add(close);
        root.add(control, BorderLayout.SOUTH);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
            {
                boolean selectable = list.getSelectedValue() != null;
                open.setEnabled(selectable);
                group.setSelected(open.getModel(), selectable);
                if (selectable)
                    open.requestFocus(true);
            }
        });

        setContentPane(root);

        pack();
        setModal(true);
        setTitle("选择历史输入源");
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(e -> closeDialog(),
                                             KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                             JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
