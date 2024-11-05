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

package m2tk.assistant.ui;

import cn.hutool.core.thread.ThreadUtil;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.Global;
import m2tk.assistant.MPEGTSPlayer;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public final class AssistantApp extends SingleFrameApplication
{
    public static AssistantApp getInstance()
    {
        return SingleFrameApplication.getInstance(AssistantApp.class);
    }

    public static final String APP_NAME = "M2TK传输流分析助手";
    public static final String APP_VERSION = "2.0.0-dev";
    public static final String APP_COPYRIGHT_YEAR = "2024";
    public static final String APP_VENDOR = "M2TK项目组";

    private MainViewController controller;
    private MPEGTSPlayer player;
    private String[] cmdArgs;

    @Override
    protected void initialize(String[] args)
    {
        cmdArgs = args;

        // 加载必要的自定义字体
        registerCustomFont("fonts/SourceHanSansCN-Regular.otf");
        registerCustomFont("fonts/InterVariable.ttf");
        registerCustomFont("fonts/Wallpoet.ttf");

        FlatLaf.registerCustomDefaultsSource("m2tk.assistant.theme");
        FlatMacDarkLaf.setup();
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
    protected void ready()
    {
        log.info("启动服务内核...");

//        Task<SolonApp, Void> kernelStarter = new Task<>(getInstance())
//        {
//            @Override
//            protected SolonApp doInBackground()
//            {
//                return Solon.start(KernelEntry.class, cmdArgs);
//            }
//
//            @Override
//            protected void succeeded(SolonApp solon)
//            {
//                controller.kernelLoaded(solon);
//                log.info("服务内核已就绪");
//            }
//
//            @Override
//            protected void failed(Throwable cause)
//            {
//                JOptionPane.showMessageDialog(getMainFrame(),
//                                              "服务内核无法启动，请关闭当前程序。",
//                                              "警告",
//                                              JOptionPane.ERROR_MESSAGE);
//                exit();
//            }
//        };
//        kernelStarter.execute();
    }

    @Override
    protected void shutdown()
    {
        player.stop();
        controller.setWillQuit();
        Global.getStreamAnalyser().shutdown();
        super.shutdown();
    }

    public void playProgram(String url, int programNumber)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playProgram(url, programNumber);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playVideo(String url, int videoPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playVideo(url, videoPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playAudio(String url, int audioPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playAudio(url, audioPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    private void registerCustomFont(String file)
    {
        try (InputStream in = Files.newInputStream(Paths.get(System.getProperty("user.dir"), file)))
        {
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Throwable any)
        {
            log.warn("无法加载字体：{}", file);
        }
    }
}
