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
package m2tk.assistant.app.ui.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.app.ui.AssistantApp;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.Year;

@Slf4j
public class AboutDialog extends JDialog
{
    public AboutDialog(Frame parent)
    {
        super(parent);
        setupUI();
    }

    private void setupUI()
    {
        JPanel root = new JPanel(new MigLayout("gap 0", "[480!, fill, center]"));

        JLabel logoLabel = new JLabel();
        logoLabel.setText("M2TK");
        logoLabel.setIcon(new FlatSVGIcon("images/logo-a.svg", 60, 60));
        logoLabel.setFont(new Font("Wallpoet", Font.PLAIN, 42));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setForeground(FlatLaf.isLafDark() ? AssistantApp.M2TK_LIGHT : AssistantApp.M2TK_DARK);

        JLabel appNameLabel = new JLabel(AssistantApp.APP_NAME);
        appNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        appNameLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2.regular");

        JLabel appVersionLabel = new JLabel(AssistantApp.APP_VERSION);
        appVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        appVersionLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");

        JLabel copyrightLabel = new JLabel("版权所有 © 2020-" + Year.now() + "，" + AssistantApp.APP_VENDOR);
        copyrightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        copyrightLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4");

        String link = "https://gitee.com/craftworks/m2tk-assistant";
        JLabel linkLabel = new JLabel("<html><a href='#'>" + link + "</a>");
        linkLabel.setHorizontalAlignment(SwingConstants.CENTER);
        linkLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "h4");
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    Desktop.getDesktop().browse(new URI(link));
                } catch (Exception ex)
                {
                    log.warn("无法打开链接：{}", link);
                    log.error("内部异常：{}", ex.getMessage(), ex);
                }
            }
        });

        root.add(logoLabel, "gapy 15, wrap");
        root.add(appNameLabel, "gapy 15, wrap");
        root.add(appVersionLabel, "gapy 10, wrap");
        root.add(copyrightLabel, "gapy 10, wrap");
        root.add(linkLabel, "gapy 10, wrap");
        root.add(new JSeparator(SwingConstants.HORIZONTAL), "gapy 10, wrap");

        JButton close = new JButton("确定");
        close.addActionListener(e -> closeDialog());
        JPanel control = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        control.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 20));
        control.add(Box.createVerticalStrut(40));
        control.add(Box.createHorizontalGlue());
        control.add(close);
        root.add(control, "gapy 10, pushy");

        setContentPane(root);

        pack();
        setModal(true);
        setTitle("关于 " + AssistantApp.APP_NAME);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(e -> closeDialog(),
                                             KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                             JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void closeDialog()
    {
        setVisible(false);
        dispose();
    }
}
