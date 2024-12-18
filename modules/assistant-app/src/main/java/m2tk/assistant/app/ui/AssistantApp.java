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
package m2tk.assistant.app.ui;

import cn.hutool.core.thread.ThreadUtil;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto_mono.FlatRobotoMonoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.app.kernel.KernelEntry;
import m2tk.assistant.app.kernel.service.MPEGTSPlayer;
import m2tk.assistant.app.kernel.service.StreamAnalyzer;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;
import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.core.AppContext;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.Objects;

@Slf4j
public final class AssistantApp extends SingleFrameApplication
{
    public static AssistantApp getInstance()
    {
        return SingleFrameApplication.getInstance(AssistantApp.class);
    }

    public static final String APP_NAME = "M2TK传输流分析助手";
    public static final String APP_VERSION = "2.0.0-dev";
    public static final String APP_VENDOR = "M2TK项目组";
    public static final Color M2TK_DARK = Color.decode("#104071");
    public static final Color M2TK_LIGHT = Color.decode("#F6DD34");

    private MainViewController controller;

    private volatile M2TKDatabase database;
    private volatile StreamAnalyzer analyzer;
    private volatile MPEGTSPlayer player;
    private volatile EventBus eventBus;
    private String[] cmdArgs;

    @Override
    protected void initialize(String[] args)
    {
        cmdArgs = args;

        // 加载必要的自定义字体
        registerCustomFont("/fonts/Wallpoet.ttf");

        FlatLaf.registerCustomDefaultsSource("m2tk.assistant.app.ui.theme");
        FlatRobotoMonoFont.install();
        FlatLaf.setPreferredMonospacedFontFamily(FlatRobotoMonoFont.FAMILY);
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
        log.info("启动内核...");

        Task<SolonApp, Void> kernelStarter = new Task<>(getInstance())
        {
            @Override
            protected SolonApp doInBackground()
            {
                return Solon.start(KernelEntry.class, cmdArgs);
            }

            @Override
            protected void succeeded(SolonApp solon)
            {
                AppContext context = Solon.context();
                analyzer = context.getBean(StreamAnalyzer.class);
                player = context.getBean(MPEGTSPlayer.class);
                eventBus = context.getBean(EventBus.class);
                database = context.getBean(M2TKDatabase.class);
                controller.ready();
                log.info("内核已就绪");
            }

            @Override
            protected void failed(Throwable cause)
            {
                JOptionPane.showMessageDialog(getMainFrame(),
                                              "内核无法启动，即将退出程序。",
                                              "运行时异常",
                                              JOptionPane.ERROR_MESSAGE);
                exit();
            }
        };
        kernelStarter.execute();
    }

    @Override
    protected void shutdown()
    {
        controller.setWillQuit();
        analyzer.shutdown();
        player.stop();

        super.shutdown();
    }

    public StreamAnalyzer getStreamAnalyzer()
    {
        return analyzer;
    }

    public EventBus getEventBus()
    {
        return eventBus;
    }

    public M2TKDatabase getM2TKDatabase()
    {
        return database;
    }

    public void playProgram(String uri, int programNumber)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playProgram(uri, programNumber);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playVideo(String uri, int videoPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playVideo(uri, videoPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    public void playAudio(String uri, int audioPid)
    {
        Runnable onError = () -> JOptionPane.showMessageDialog(getMainView().getFrame(),
                                                               "无法播放指定内容",
                                                               "错误",
                                                               JOptionPane.WARNING_MESSAGE);

        ThreadUtil.execute(() -> {
            try
            {
                player.stop();
                player.playAudio(uri, audioPid);
            } catch (Exception ex)
            {
                log.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }

    private void registerCustomFont(String file)
    {
        try (InputStream in = getClass().getResourceAsStream(file))
        {
            Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(in));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Throwable any)
        {
            log.warn("无法加载字体：{}", file);
        }
    }
}
