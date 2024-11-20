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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

public class SystemInfoDialog extends JDialog
{
    private JTextArea textArea;

    public SystemInfoDialog(Frame parent)
    {
        super(parent);
        setupUI(parent);
    }

    private void closeDialog()
    {
        setVisible(false);
        dispose();
    }

    private void exportInfo()
    {
        String info = textArea.getText();
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(Paths.get(System.getProperty("user.dir")).toFile());
        chooser.setSelectedFile(new File("system-info.txt"));
        int retCode = chooser.showSaveDialog(this);
        if (retCode == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                File target = chooser.getSelectedFile();
                Files.writeString(target.toPath(),
                                  info,
                                  StandardOpenOption.CREATE,
                                  StandardOpenOption.TRUNCATE_EXISTING);
                JOptionPane.showMessageDialog(null,
                                              "导出成功",
                                              "导出系统环境信息",
                                              JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex)
            {
                JOptionPane.showMessageDialog(null,
                                              "导出失败",
                                              "导出系统环境信息",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupUI(Frame parent)
    {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        textArea = new JTextArea();
        textArea.setEditable(false);
        root.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton export = new JButton("导出到文件");
        export.addActionListener(e -> exportInfo());
        JButton close = new JButton("确定");
        close.addActionListener(e -> closeDialog());
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        control.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        control.add(export);
        control.add(Box.createHorizontalGlue());
        control.add(close);
        root.add(control, BorderLayout.SOUTH);

        setContentPane(root);

        // Collect System Info
        appendTextArea("------------System.getenv---------------");
        Map<String, String> map = System.getenv();
        for (Map.Entry<String, String> envEntry : map.entrySet())
        {
            appendTextArea(envEntry.getKey() + "=" + envEntry.getValue());
        }

        appendTextArea("------------System.getProperties---------------");
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet())
        {
            appendTextArea(objectObjectEntry.getKey() + "=" + objectObjectEntry.getValue());
        }

        pack();
        setModal(true);
        setTitle("系统环境变量");
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(e -> closeDialog(),
                                             KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                             JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void appendTextArea(String str)
    {
        textArea.append(str);
        textArea.append(System.lineSeparator());
    }
}
