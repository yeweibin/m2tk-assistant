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

package m2tk.assistant;

import cn.hutool.core.thread.ThreadUtil;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.ui.MainViewController;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

@Slf4j
public final class AssistantApp extends SingleFrameApplication
{
    public static AssistantApp getInstance()
    {
        return SingleFrameApplication.getInstance(AssistantApp.class);
    }

    public static void main(String[] args)
    {
        Global.init();
        SingleFrameApplication.launch(AssistantApp.class, args);
    }

    public static final String APP_NAME = "M2TK码流分析助手";
    public static final String APP_VERSION = "1.6.5.1413";
    public static final String APP_COPYRIGHT_YEAR = "2024";
    public static final String APP_VENDOR = "M2TK项目组";
    private MainViewController controller;
    private MPEGTSPlayer player;

    @Override
    protected void initialize(String[] args)
    {
        Font treeFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Tree.font").getSize());
        Font tableFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Table.font").getSize());
        FlatMacDarkLaf.setup();
        UIManager.put("TitlePane.showIconBesideTitle", true);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Table.paintOutsideAlternateRows", true);
        UIManager.put("Tree.font", treeFont);
        UIManager.put("Table.font", tableFont);
        UIManager.put("Table.cellMargins", new Insets(4, 6, 4, 6));
//        UIManager.put("Table.alternateRowColor", FlatLaf.isLafDark() ? new Color(0x626262) : new Color(0xEEEEEE));
    }

    @Override
    protected void startup()
    {
        FrameView frameView = getMainView();
        controller = new MainViewController(frameView);
        player = new MPEGTSPlayer();
        show(frameView);
    }

    @Override
    protected void shutdown()
    {
        player.stop();
        controller.setWillQuit();
        Global.getStreamAnalyser().shutdown();
        super.shutdown();
    }

    public void playVideoAndAudio(InputStream in, int videoPid, int audioPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playVideoAndAudio(in, videoPid, audioPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playVideo(InputStream in, int videoPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playVideo(in, videoPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playAudio(InputStream in, int audioPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playAudio(in, audioPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }
}
