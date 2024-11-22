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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Functions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jnafilechooser.api.JnaFileChooser;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.app.Global;
import m2tk.assistant.app.ui.dialog.AboutDialog;
import m2tk.assistant.app.ui.dialog.SourceHistoryDialog;
import m2tk.assistant.app.ui.dialog.SystemInfoDialog;
import m2tk.assistant.app.ui.task.DrawNetworkGraphTask;
import m2tk.assistant.app.ui.tracer.PSITracer;
import m2tk.assistant.app.ui.tracer.SITracer;
import m2tk.assistant.app.ui.tracer.StreamTracer;
import m2tk.assistant.app.ui.util.ButtonBuilder;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.MenuItemBuilder;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class MainViewController
{
    private final FrameView frameView;
    private final ActionMap actionMap;

    private List<InfoView> coreInfoViews;
    private List<InfoView> pluggedInfoViews;
    private List<Tracer> tracers;
    private LogsView logsView;
    private JTabbedPane tabbedPane;
    private Path lastOpenDirectory;
    private volatile boolean willQuit;

    public MainViewController(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        loadPluginsAndExtensions();
        createAndSetupMenu();
        createAndSetupToolBar();
        createAndSetupWorkspace();
        setupInitialStates();
    }

    private void loadPluginsAndExtensions()
    {
        PluginManager pluginManager = new DefaultPluginManager(Paths.get("D:\\Projects\\m2tk\\m2tk-assistant\\modules\\assistant-app\\plugins"));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        coreInfoViews = pluginManager.getExtensions(InfoView.class);
        pluggedInfoViews = new ArrayList<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper plugin : plugins)
        {
            List<InfoView> extViews = pluginManager.getExtensions(InfoView.class, plugin.getPluginId());
            pluggedInfoViews.addAll(extViews);
        }
        Map<String, InfoView> pluggedViewMap = pluggedInfoViews.stream().collect(Collectors.toMap(view -> view.getClass().getName(), Functions.identity()));
        coreInfoViews.removeIf(view -> pluggedViewMap.containsKey(view.getClass().getName()));

        tracers = pluginManager.getExtensions(Tracer.class);
        for (InfoView view : coreInfoViews)
            view.setupApplication(frameView.getApplication());
        for (InfoView view : pluggedInfoViews)
            view.setupApplication(frameView.getApplication());
    }

    private void createAndSetupMenu()
    {
        int iconSize = 20;
        Color iconColor = UIManager.getColor("Label.foreground");
        Function<Ikon, Icon> iconSupplier = ikon -> FontIcon.of(ikon, iconSize, iconColor);

        MenuItemBuilder builder = new MenuItemBuilder();
        JMenu menuSys = new JMenu("系统(S)");
        menuSys.setMnemonic(KeyEvent.VK_S);
        menuSys.add(builder.create(actionMap.get("showSystemInfo"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.BOOK_INFORMATION_24))
                           .text("查看系统信息")
                           .get());
        menuSys.add(builder.create(actionMap.get("exitApp"))
                           .text("退出(X)")
                           .mnemonic(KeyEvent.VK_X).get());

        JMenu menuOps = new JMenu("操作(O)");
        menuOps.setMnemonic(KeyEvent.VK_O);
        JMenu sourceMenu = new JMenu("选择输入源");
        sourceMenu.add(builder.create(actionMap.get("openFile"))
                              .icon(iconSupplier.apply(FluentUiRegularMZ.VIDEO_CLIP_20))
                              .text("本地文件")
                              .get());
        sourceMenu.add(builder.create(actionMap.get("openMulticast"))
                              .icon(iconSupplier.apply(FluentUiRegularAL.LIVE_20))
                              .text("组播流")
                              .get());
        sourceMenu.add(builder.create(actionMap.get("openThirdPartyInputSource"))
                              .icon(iconSupplier.apply(FluentUiRegularMZ.MAP_DRIVE_20))
                              .text("扩展外设")
                              .get());
        menuOps.add(sourceMenu);
        menuOps.add(builder.create(actionMap.get("reopenInput"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.HISTORY_20))
                           .text("重新分析")
                           .get());
        menuOps.add(builder.create(actionMap.get("stopAnalyzer"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.DISMISS_CIRCLE_20))
                           .text("停止分析")
                           .get());
        menuOps.add(builder.create(actionMap.get("pauseRefreshing"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALENDAR_CLOCK_20))
                           .text("暂停刷新")
                           .get());
        menuOps.add(builder.create(actionMap.get("startRefreshing"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALENDAR_SYNC_20))
                           .text("继续刷新")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("openTerminal"))
                           .icon(iconSupplier.apply(Feather.TERMINAL))
                           .text("打开命令行")
                           .get());
        menuOps.add(builder.create(actionMap.get("openCalc"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALCULATOR_20))
                           .text("打开计算器")
                           .get());
        menuOps.add(builder.create(actionMap.get("openNotepad"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.NOTEPAD_20))
                           .text("打开记事本")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("drawNetworkGraph"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.ORGANIZATION_20))
                           .text("绘制网络结构图")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("exportInternalTemplates"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.SHARE_20))
                           .text("导出默认解析模板")
                           .get());
        menuOps.add(builder.create(actionMap.get("loadCustomTemplates"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.LAYER_20))
                           .text("加载自定义解析模板")
                           .get());

        JMenu menuViews = new JMenu("查看(V)");
        menuViews.setMnemonic(KeyEvent.VK_V);
        for (InfoView view : coreInfoViews)
        {
            view.setupMenu(menuViews);
        }
        if (!pluggedInfoViews.isEmpty())
        {
            JMenu extViews = new JMenu("扩展视图");
            extViews.setIcon(iconSupplier.apply(FluentUiRegularAL.EXTENSION_20));
            menuViews.add(extViews);
            for (InfoView view : pluggedInfoViews)
                view.setupMenu(extViews);
        }

        JMenu menuLogs = new JMenu("日志(L)");
        menuLogs.setMnemonic(KeyEvent.VK_L);
        menuLogs.add(builder.create(actionMap.get("clearLogs"))
                            .icon(iconSupplier.apply(FluentUiRegularAL.DELETE_20))
                            .text("清空日志")
                            .get());
        menuLogs.add(builder.create(actionMap.get("checkLogs"))
                            .icon(iconSupplier.apply(FluentUiRegularAL.DOCUMENT_COPY_20))
                            .text("查看历史日志")
                            .get());

        JMenu menuHelp = new JMenu("帮助(H)");
        menuHelp.setMnemonic(KeyEvent.VK_H);
        menuHelp.add(builder.create(actionMap.get("showHelp"))
                            .text("帮助")
                            .get());
        menuHelp.add(builder.create(actionMap.get("showAbout"))
                            .text("关于(A)")
                            .mnemonic(KeyEvent.VK_A)
                            .get());

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuSys);
        menuBar.add(menuOps);
        menuBar.add(menuViews);
        menuBar.add(menuLogs);
        menuBar.add(menuHelp);
        frameView.setMenuBar(menuBar);
    }

    private void createAndSetupToolBar()
    {
        ButtonBuilder builder = new ButtonBuilder();
        int iconSize = 24;
        Color iconColor = UIManager.getColor("Label.foreground");
        Function<Ikon, Icon> iconSupplier = ikon -> FontIcon.of(ikon, iconSize, iconColor);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(builder.create(actionMap.get("openFile"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.VIDEO_CLIP_24))
                           .text(null)
                           .tooltip("分析码流文件")
                           .get());
        toolBar.add(builder.create(actionMap.get("openMulticast"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.LIVE_24))
                           .text(null)
                           .tooltip("分析组播流")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("reopenInput"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.HISTORY_24))
                           .text(null)
                           .tooltip("重新分析")
                           .get());
        toolBar.add(builder.create(actionMap.get("stopAnalyzer"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.DISMISS_CIRCLE_24))
                           .text(null)
                           .tooltip("停止分析")
                           .get());
        toolBar.add(builder.create(actionMap.get("pauseRefreshing"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALENDAR_CLOCK_24))
                           .text(null)
                           .tooltip("暂停刷新")
                           .get());
        toolBar.add(builder.create(actionMap.get("startRefreshing"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALENDAR_SYNC_24))
                           .text(null)
                           .tooltip("继续刷新")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("openTerminal"))
                           .icon(iconSupplier.apply(Feather.TERMINAL))
                           .text(null)
                           .tooltip("打开命令行")
                           .get());
        toolBar.add(builder.create(actionMap.get("openCalc"))
                           .icon(iconSupplier.apply(FluentUiRegularAL.CALCULATOR_20))
                           .text(null)
                           .tooltip("打开计算器")
                           .get());
        toolBar.add(builder.create(actionMap.get("openNotepad"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.NOTEPAD_24))
                           .text(null)
                           .tooltip("打开记事本")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("drawNetworkGraph"))
                           .icon(iconSupplier.apply(FluentUiRegularMZ.ORGANIZATION_24))
                           .text(null)
                           .tooltip("绘制网络结构图")
                           .get());

        frameView.setToolBar(toolBar);
    }

    private void createAndSetupWorkspace()
    {
        logsView = new LogsView();

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

        for (InfoView view : coreInfoViews)
        {
            tabbedPane.add(view.getViewTitle(), view.getViewComponent());
            tabbedPane.addTab(view.getViewTitle(), view.getViewIcon(), view.getViewComponent());
        }

//        for (InfoView view : pluggedInfoViews)
//        {
//            view.setupApplication(frameView.getApplication());
//            tabbedPane.add(view.getViewTitle(), view.getViewComponent());
//            JComponent viewComponent = view.getViewComponent();
//            if (view.isClosable())
//            {
//                viewComponent.putClientProperty("JTabbedPane.tabClosable", true);
//                viewComponent.putClientProperty("JTabbedPane.tabCloseCallback",
//                                                (IntConsumer) tabIndex -> tabbedPane.remove(tabIndex));
//            }
//            tabbedPane.addTab(view.getViewTitle(), view.getViewIcon(), viewComponent);
//        }

        tabbedPane.addTab("日志",
                          FontIcon.of(FluentUiRegularMZ.TEXTBOX_20, 20, UIManager.getColor("Label.foreground")),
                          logsView);

        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupInitialStates()
    {
        frameView.getFrame().setTitle(AssistantApp.APP_NAME);

        initFileChooserCurrentDirectory();

        actionMap.get("openFile").setEnabled(false);
        actionMap.get("openMulticast").setEnabled(false);
//        actionMap.get("reopenInput").setEnabled(false);
//        actionMap.get("stopAnalyzer").setEnabled(false);
//        actionMap.get("pauseRefreshing").setEnabled(false);
//        actionMap.get("startRefreshing").setEnabled(false);
        ComponentUtil.setPreferSizeAndLocateToCenter(frameView.getFrame(), 0.5, 0.5);
    }

    private JMenuItem createMenuItem(String action, String text, String tooltip)
    {
        JMenuItem item = new JMenuItem();
        item.setAction(actionMap.get(action));
        item.setText(text);
        item.setToolTipText(tooltip);
        return item;
    }

    private JMenuItem createMenuItem(String action, String text, String tooltip, int mnemonic)
    {
        JMenuItem item = new JMenuItem();
        item.setAction(actionMap.get(action));
        item.setText(text);
        item.setToolTipText(tooltip);
        item.setMnemonic(mnemonic);
        return item;
    }

    private JMenuItem createMenuItem(String action, String text, String tooltip, int mnemonic, Icon icon)
    {
        JMenuItem item = new JMenuItem();
        item.setAction(actionMap.get(action));
        item.setText(text);
        item.setIcon(icon);
        item.setToolTipText(tooltip);
        item.setMnemonic(mnemonic);
        return item;
    }

    private JButton createButton(String action, String tooltip)
    {
        JButton button = new JButton();
        button.setAction(actionMap.get(action));
        button.setToolTipText(tooltip);
        return button;
    }

    public void ready()
    {
        AssistantApp application = AssistantApp.getInstance();
        EventBus bus = application.getEventBus();
        M2TKDatabase database = application.getM2TKDatabase();
        for (InfoView view : coreInfoViews)
        {
            view.setupBus(bus);
            view.setupDatabase(database);
        }
        for (InfoView view : pluggedInfoViews)
        {
            view.setupBus(bus);
            view.setupDatabase(database);
        }
//        streamInfoView.setupDataSource(bus, database);
//        networkInfoView.setupDataSource(bus, database);
//        tr290InfoView.setupDataSource(bus, database);
//        pcrStatsView.setupDataSource(bus, database);
//        epgInfoView.setupDataSource(bus, database);
//        datagramView.setupDataSource(bus, database);
//        ebInfoView.setupDataSource(bus, database);

        bus.register(this);
        actionMap.get("openFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
    }

    @Subscribe
    public void onShowInfoViewEvent(ShowInfoViewEvent event)
    {
        InfoView view = event.getView();
        int tabs = tabbedPane.getTabCount();
        for (int i = 0; i < tabs; i++)
        {
            Component c = tabbedPane.getComponentAt(i);
            if (c == view.getViewComponent())
            {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }

        // Not found
        JComponent viewComponent = view.getViewComponent();
        viewComponent.putClientProperty("JTabbedPane.tabClosable", true);
        viewComponent.putClientProperty("JTabbedPane.tabCloseCallback",
                                        (IntConsumer) tabIndex -> tabbedPane.remove(tabIndex));
        tabbedPane.addTab(view.getViewTitle(), view.getViewIcon(), viewComponent);
        tabbedPane.setSelectedIndex(tabs);
    }

    @Action
    public void showSystemInfo()
    {
        SystemInfoDialog dialog = new SystemInfoDialog(frameView.getFrame());
        dialog.setVisible(true);
    }

    @Action
    public void exitApp()
    {
        willQuit = true;
        frameView.getApplication().exit();
    }

    @Action
    public void showStreamInfo()
    {
//        tabbedPane.setSelectedComponent(streamInfoView);
    }

    @Action
    public void showNetworkInfo()
    {
//        tabbedPane.setSelectedComponent(networkInfoView);
    }

    @Action
    public void showTR290Info()
    {
//        tabbedPane.setSelectedComponent(tr290InfoView);
    }

    @Action
    public void showPCRInfo()
    {
//        tabbedPane.setSelectedComponent(pcrStatsView);
    }

    @Action
    public void showEPGInfo()
    {
//        tabbedPane.setSelectedComponent(epgInfoView);
    }

    @Action
    public void showNVODInfo()
    {
//        tabbedPane.setSelectedComponent(nvodInfoView);
    }

    @Action
    public void showPSISIInfo()
    {
//        tabbedPane.setSelectedComponent(datagramView);
    }

    @Action
    public void showEBInfo()
    {
//        tabbedPane.setSelectedComponent(ebInfoView);
    }

    @Action
    public void checkLogs()
    {
        Path logsDir = Paths.get(System.getProperty("user.home"), "m2tk", "logs");

        try
        {
            Desktop.getDesktop().open(logsDir.toFile());
        } catch (IOException ex)
        {
            log.warn("打开日志目录异常：{}", ex.getMessage());
        }
    }

    @Action
    public void clearLogs()
    {
        logsView.clear();
    }

    @Action
    public void showAbout()
    {
        AboutDialog dialog = new AboutDialog(frameView.getFrame());
        dialog.setVisible(true);
    }

    @Action
    public void openFile()
    {
        JnaFileChooser fileChooser = new JnaFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setMode(JnaFileChooser.Mode.Files);
        fileChooser.setCurrentDirectory(lastOpenDirectory.toString());
        fileChooser.addFilter("码流文件", "ts", "m2ts", "mpeg");

        if (fileChooser.showOpenDialog(frameView.getFrame()))
        {
            File file = fileChooser.getSelectedFile();
            String input = file.getAbsolutePath();

            System.out.println("准备分析 @ " + LocalDateTime.now());
            log.info("准备分析");
            Global.resetUserPrivateSectionStreams();
            boolean started = false;
            try
            {
                started = AssistantApp.getInstance()
                                      .getStreamAnalyzer()
                                      .start(input,
                                             List.of(new StreamTracer(),
                                                     new PSITracer(),
                                                     new SITracer()),
                                             this::onAnalyzerStopped);
                log.info("开始分析：{}", started);
            } catch (Exception ex)
            {
                System.err.println(ex.getMessage());
            }

            if (!started)
            {
                actionMap.get("reopenInput").setEnabled(false);
                JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
            } else
            {
                saveRecentFile(file);
//                streamInfoView.reset();
//                networkInfoView.reset();
//                tr290InfoView.reset();
//                pcrStatsView.reset();
//                epgInfoView.reset();
////                nvodInfoView.reset();
//                datagramView.reset();
//                ebInfoView.reset();
                actionMap.get("openFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("openThirdPartyInputSource").setEnabled(false);
                actionMap.get("reopenInput").setEnabled(false);
                actionMap.get("stopAnalyzer").setEnabled(true);
                actionMap.get("pauseRefreshing").setEnabled(true);
                actionMap.get("startRefreshing").setEnabled(false);
            }
        }
    }

    @Action
    public void openMulticast()
    {
        String input = JOptionPane.showInputDialog(frameView.getFrame(),
                                                   "组播地址",
                                                   "udp://224.1.1.1:7890");
        if (input == null)
            return;

        if (!isCorrectMulticastAddress(input))
        {
            JOptionPane.showMessageDialog(frameView.getFrame(),
                                          "无效的组播地址" + System.lineSeparator() + input,
                                          "错误",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        Global.resetUserPrivateSectionStreams();
        boolean started = AssistantApp.getInstance()
                                      .getStreamAnalyzer()
                                      .start(input, List.of(), this::onAnalyzerStopped);
        if (!started)
        {
            actionMap.get("reopenInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
//            streamInfoView.reset();
//            networkInfoView.reset();
//            tr290InfoView.reset();
//            pcrStatsView.reset();
//            epgInfoView.reset();
//            nvodInfoView.reset();
//            datagramView.reset();
//            ebInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);
        }
    }

    @Action
    public void openThirdPartyInputSource()
    {
        String input = JOptionPane.showInputDialog(frameView.getFrame(),
                                                   "输入源地址",
                                                   null);
        if (input == null)
            return;

        boolean started = AssistantApp.getInstance()
                                      .getStreamAnalyzer()
                                      .start(input, List.of(), this::onAnalyzerStopped);
        if (!started)
        {
            actionMap.get("reopenInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
//            streamInfoView.reset();
//            networkInfoView.reset();
//            tr290InfoView.reset();
//            pcrStatsView.reset();
//            epgInfoView.reset();
//            nvodInfoView.reset();
//            datagramView.reset();
//            ebInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);
        }
    }

    @Action
    public void reopenInput()
    {
        SourceHistoryDialog dialog = new SourceHistoryDialog(frameView.getFrame());
        ComponentUtil.setPreferSizeAndLocateToCenter(dialog, 0.5, 0.4);

        String source = dialog.selectFromSourceHistory();
        if (source == null)
            return;

        boolean started = AssistantApp.getInstance()
                                      .getStreamAnalyzer()
                                      .start(source, List.of(), this::onAnalyzerStopped);
        if (!started)
        {
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
//            streamInfoView.reset();
//            networkInfoView.reset();
//            tr290InfoView.reset();
//            pcrStatsView.reset();
//            epgInfoView.reset();
//            nvodInfoView.reset();
//            datagramView.reset();
//            ebInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);
        }
    }

    @Action
    public void stopAnalyzer()
    {
        AssistantApp.getInstance().getStreamAnalyzer().stop();
    }

    @Action
    public void pauseRefreshing()
    {
//        streamInfoView.stopRefreshing();
//        networkInfoView.stopRefreshing();
//        tr290InfoView.stopRefreshing();
//        pcrStatsView.stopRefreshing();
//        epgInfoView.stopRefreshing();
////        nvodInfoView.stopRefreshing();
//        datagramView.stopRefreshing();
//        ebInfoView.stopRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(true);
    }

    @Action
    public void startRefreshing()
    {
//        streamInfoView.startRefreshing();
//        networkInfoView.startRefreshing();
//        tr290InfoView.startRefreshing();
//        pcrStatsView.startRefreshing();
//        epgInfoView.startRefreshing();
////        nvodInfoView.startRefreshing();
//        datagramView.startRefreshing();
//        ebInfoView.startRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(true);
        actionMap.get("startRefreshing").setEnabled(false);
    }

    @Action
    public void openTerminal()
    {
        try
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("cmd", "/c", "start", "cmd.exe")
                   .start();
        } catch (IOException ex)
        {
            log.warn("打开命令行程序异常：{}", ex.getMessage());
        }
    }

    @Action
    public void openCalc()
    {
        try
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("calc.exe").start();
        } catch (IOException ex)
        {
            log.warn("打开计算器程序异常：{}", ex.getMessage());
        }
    }

    @Action
    public void openNotepad()
    {
        try
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("notepad.exe").start();
        } catch (IOException ex)
        {
            log.warn("打开记事本程序异常：{}", ex.getMessage());
        }
    }

    @Action
    public void drawNetworkGraph()
    {
        DrawNetworkGraphTask task = new DrawNetworkGraphTask(frameView.getApplication());
        ComponentUtil.setWaitingMouseCursor(frameView.getRootPane(), true);
        task.execute();
    }

    @Action
    public void exportInternalTemplates()
    {
        JnaFileChooser fileChooser = new JnaFileChooser();
        fileChooser.setMode(JnaFileChooser.Mode.Directories);
        if (fileChooser.showSaveDialog(frameView.getFrame()))
        {
            File dir = fileChooser.getSelectedFile();
            File zip = new File(dir, "模板.zip");

            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)))
            {
                out.putNextEntry(new ZipEntry("PSITemplates.xml"));
                try (InputStream in = getClass().getResourceAsStream("/template/PSITemplate.xml"))
                {
                    IoUtil.copy(in, out);
                }
                out.closeEntry();

                out.putNextEntry(new ZipEntry("SITemplates.xml"));
                try (InputStream in = getClass().getResourceAsStream("/template/SITemplate.xml"))
                {
                    IoUtil.copy(in, out);
                }
                out.closeEntry();
                out.finish();

                JOptionPane.showMessageDialog(frameView.getFrame(),
                                              "导出模板到 " + zip, "导出成功",
                                              JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex)
            {
                JOptionPane.showMessageDialog(frameView.getFrame(),
                                              "无法导出模板", "请注意",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Action
    public void loadCustomTemplates()
    {
        JnaFileChooser fileChooser = new JnaFileChooser();
        fileChooser.setCurrentDirectory(Paths.get(System.getProperty("user.dir"), "template").toString());
        fileChooser.setMode(JnaFileChooser.Mode.Files);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addFilter("模板文件", "xml");
        if (fileChooser.showOpenDialog(frameView.getFrame()))
        {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length > 0)
            {
                int count = Global.loadUserDefinedTemplates(files);
                if (count == files.length)
                {
                    JOptionPane.showMessageDialog(frameView.getFrame(), "加载成功");
                } else if (count > 0)
                {
                    JOptionPane.showMessageDialog(frameView.getFrame(),
                                                  String.format("有 %d 个模板文件加载失败", files.length - count),
                                                  "请注意",
                                                  JOptionPane.WARNING_MESSAGE);
                } else
                {
                    JOptionPane.showMessageDialog(frameView.getFrame(),
                                                  "全部模板加载失败", "请注意",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void setWillQuit()
    {
        willQuit = true;
    }

    private void onAnalyzerStopped(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        if (!willQuit)
            JOptionPane.showMessageDialog(frameView.getFrame(), "分析过程结束");

        actionMap.get("openFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
        actionMap.get("openThirdPartyInputSource").setEnabled(true);
        actionMap.get("reopenInput").setEnabled(true);
        actionMap.get("stopAnalyzer").setEnabled(false);
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(false);

//        streamInfoView.stopRefreshing();
//        networkInfoView.stopRefreshing();
//        tr290InfoView.stopRefreshing();
//        pcrStatsView.stopRefreshing();
//        epgInfoView.stopRefreshing();
////        nvodInfoView.startRefreshing();
//        datagramView.startRefreshing();
//        ebInfoView.startRefreshing();
    }

    private boolean isCorrectMulticastAddress(String input)
    {
        try
        {
            URI uri = URI.create(input);
            if (!uri.getScheme().equals("udp"))
                return false;

            return Inet4Address.getByName(uri.getHost()).isMulticastAddress();
        } catch (Exception ex)
        {
            return false;
        }
    }

    private void initFileChooserCurrentDirectory()
    {
        try
        {
            Path pwd = Paths.get(System.getProperty("user.dir"));
            Path recentCfg = pwd.resolve("recent.cfg");
            if (!Files.exists(recentCfg))
            {
                lastOpenDirectory = Paths.get(System.getProperty("user.dir"));
            } else
            {
                String recentFile = FileUtil.readUtf8String(recentCfg.toFile());
                lastOpenDirectory = StrUtil.isNotBlank(recentFile)
                                    ? Paths.get(recentFile)
                                    : Paths.get(System.getProperty("user.dir"));
            }
        } catch (Exception ex)
        {
            log.debug("无法设置文件选择器起始路径：{}", ex.getMessage());
        }
    }

    private void saveRecentFile(File file)
    {
        try
        {
            Path pwd = Paths.get(System.getProperty("user.dir"));
            Path recentCfg = pwd.resolve("recent.cfg");

            FileUtil.writeUtf8String(file.getAbsolutePath(), recentCfg.toFile());
        } catch (Exception ex)
        {
            log.debug("无法保存最近使用的文件：{}", ex.getMessage());
        }
    }
}
