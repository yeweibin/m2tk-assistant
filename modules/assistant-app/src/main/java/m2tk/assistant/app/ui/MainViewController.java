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
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.*;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.template.DescriptorDecoder;
import m2tk.assistant.api.template.SectionDecoder;
import m2tk.assistant.api.template.SelectorDecoder;
import m2tk.assistant.api.template.TemplateReader;
import m2tk.assistant.api.template.definition.M2TKTemplate;
import m2tk.assistant.app.kernel.service.StreamAnalyzer;
import m2tk.assistant.app.ui.dialog.AboutDialog;
import m2tk.assistant.app.ui.dialog.SourceHistoryDialog;
import m2tk.assistant.app.ui.dialog.SystemInfoDialog;
import m2tk.assistant.app.ui.event.ClearLogsEvent;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.task.DrawNetworkDiagramTask;
import m2tk.assistant.app.ui.util.ButtonBuilder;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.assistant.app.ui.util.MenuItemBuilder;
import m2tk.assistant.app.ui.view.LogsView;
import m2tk.assistant.app.ui.view.StreamInfoView;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class MainViewController
{
    private final FrameView frameView;
    private final ActionMap actionMap;

    private List<InfoView> infoViews;
    private List<InfoView> coreInfoViews;
    private List<InfoView> pluggedInfoViews;
    private List<Tracer> tracers;
    private LogsView logsView;
    private JTabbedPane tabbedPane;
    private Path lastOpenDirectory;
    private volatile boolean willQuit;

    private EventBus bus;
    private M2TKDatabase database;
    private StreamAnalyzer analyzer;
    private Timer timer;

    private Icon consoleMenuIcon, consoleToolbarIcon;
    private Icon appIcon;

    private static final Color DISABLED = UIManager.getColor("Label.disabledForeground");
    private static final Color MS_ORANGE = Color.decode("#F25022");
    private static final Color MS_GREEN = Color.decode("#7FBA00");
    private static final Color MS_BLUE = Color.decode("#00A4EF");
    private static final Color HP_BLUE = Color.decode("#0096D6");
    private static final Color INSTA_RED = Color.decode("#FD1D1D");
    private static final Color SLACK_LIGHT_BLUE = Color.decode("#89D3DF");
    private static final int TIMER_INTERVAL_MILLIS = 200;

    public MainViewController(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        initCustomIcons();
        loadInternalTemplates();
        loadPluginsAndExtensions();
        createAndSetupMenu();
        createAndSetupToolBar();
        createAndSetupWorkspace();
        setupInitialStates();
    }

    private void initCustomIcons()
    {
        // 下面的图标在FluentUI图标集里找不到合适的对应项，因此使用第三方图标。
        FlatSVGIcon.ColorFilter colorFilter = new FlatSVGIcon.ColorFilter();
        colorFilter.add(Color.black, HP_BLUE);

        FlatSVGIcon consoleIcon = new FlatSVGIcon("images/console.svg");
        consoleIcon.setColorFilter(colorFilter);
        consoleMenuIcon = consoleIcon.derive(16, 16);
        consoleToolbarIcon = consoleIcon.derive(26, 26);

        appIcon = new FlatSVGIcon("images/logo-a.svg", 20, 20);
    }

    private void loadInternalTemplates()
    {
        TemplateReader reader = new TemplateReader();
        M2TKTemplate psiTemplate = reader.parse(getClass().getResource("/template/PSITemplate.xml"));
        if (psiTemplate != null)
        {
            psiTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
            psiTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
            psiTemplate.getSelectorTemplates().forEach(SelectorDecoder::registerTemplate);
        }
        M2TKTemplate siTemplate = reader.parse(getClass().getResource("/template/SITemplate.xml"));
        if (siTemplate != null)
        {
            siTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
            siTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
            siTemplate.getSelectorTemplates().forEach(SelectorDecoder::registerTemplate);
        }
    }

    private void loadPluginsAndExtensions()
    {
        // 采用单例模式加载扩展，保证二次加载的插件扩展与首次加载内容一致。
        PluginManager pluginManager = new DefaultPluginManager()
        {
            @Override
            protected ExtensionFactory createExtensionFactory()
            {
                return new SingletonExtensionFactory(this,
                                                     Tracer.class.getName(),
                                                     InfoView.class.getName(),
                                                     StreamObserver.class.getName());
            }
        };

        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        tracers = new ArrayList<>();
        infoViews = new ArrayList<>();
        coreInfoViews = new ArrayList<>();
        pluggedInfoViews = new ArrayList<>();

        List<StreamObserver> streamObservers = new ArrayList<>();
        List<ProgramObserver> programObservers = new ArrayList<>();

        // 加载classpath里的扩展
        List<?> internalExtensions = pluginManager.getExtensions((String) null);
        for (Object extension : internalExtensions)
        {
            // 有可能一个extension实现了不同的扩展接口，所以用if逐个检查。

            if (extension instanceof StreamObserver observer)
            {
                streamObservers.add(observer);
            }
            if (extension instanceof ProgramObserver observer)
            {
                programObservers.add(observer);
            }
            if (extension instanceof Tracer tracer)
            {
                tracers.add(tracer);
            }
            if (extension instanceof InfoView view)
            {
                if (view instanceof LogsView lv)
                    logsView = lv;
                else
                    coreInfoViews.add(view);
                infoViews.add(view);
                view.setupApplication(frameView.getApplication());
            }
        }

        // 加载插件中的扩展
        List<PluginWrapper> plugins = pluginManager.getStartedPlugins();
        for (PluginWrapper plugin : plugins)
        {
            String pluginId = plugin.getPluginId();
            List<StreamObserver> pluginStreamObservers = pluginManager.getExtensions(StreamObserver.class, pluginId);
            streamObservers.addAll(pluginStreamObservers);

            List<ProgramObserver> pluginProgramObservers = pluginManager.getExtensions(ProgramObserver.class, pluginId);
            programObservers.addAll(pluginProgramObservers);

            List<Tracer> pluginTracers = pluginManager.getExtensions(Tracer.class, pluginId);
            tracers.addAll(pluginTracers);

            List<InfoView> pluginViews = pluginManager.getExtensions(InfoView.class, pluginId);
            for (InfoView view : pluginViews)
            {
                infoViews.add(view);
                pluggedInfoViews.add(view);
                view.setupApplication(frameView.getApplication());
            }
        }

        // 针对StreamInfoView做特别的设置
        StreamInfoView view = (StreamInfoView) coreInfoViews.stream()
                                                            .filter(v -> v instanceof StreamInfoView)
                                                            .findFirst()
                                                            .orElseThrow(() -> new IllegalStateException("模块加载错误"));
        view.setStreamObservers(streamObservers);
        view.setProgramObservers(programObservers);
    }

    private void createAndSetupMenu()
    {
        MenuItemBuilder builder = new MenuItemBuilder();

        JMenu menuOps = new JMenu("操作(O)");
        menuOps.setMnemonic(KeyEvent.VK_O);
        JMenu sourceMenu = new JMenu("选择输入源");
        sourceMenu.add(builder.create(actionMap.get("openLocalFile"))
                              .icon(getFontIcon(FluentUiRegularMZ.VIDEO_CLIP_20, 20, MS_ORANGE))
                              .disabledIcon(getFontIcon(FluentUiRegularMZ.VIDEO_CLIP_20, 20, DISABLED))
                              .text("文件")
                              .get());
        sourceMenu.add(builder.create(actionMap.get("openMulticast"))
                              .icon(getFontIcon(FluentUiRegularAL.LIVE_20, 20, MS_GREEN))
                              .disabledIcon(getFontIcon(FluentUiRegularAL.LIVE_20, 20, DISABLED))
                              .text("组播流")
                              .get());
        sourceMenu.add(builder.create(actionMap.get("openThirdPartyInputSource"))
                              .icon(getFontIcon(FluentUiRegularMZ.MAP_DRIVE_20, 20, MS_BLUE))
                              .disabledIcon(getFontIcon(FluentUiRegularMZ.MAP_DRIVE_20, 20, DISABLED))
                              .text("第三方设备")
                              .get());
        menuOps.add(sourceMenu);
        menuOps.add(builder.create(actionMap.get("reopenInput"))
                           .icon(getFontIcon(FluentUiRegularAL.HISTORY_20, 20, SLACK_LIGHT_BLUE))
                           .disabledIcon(getFontIcon(FluentUiRegularAL.HISTORY_20, 20, DISABLED))
                           .text("重新分析")
                           .get());
        menuOps.add(builder.create(actionMap.get("stopAnalyzer"))
                           .icon(getFontIcon(FluentUiRegularAL.DISMISS_CIRCLE_20, 20, SLACK_LIGHT_BLUE))
                           .disabledIcon(getFontIcon(FluentUiRegularAL.DISMISS_CIRCLE_20, 20, DISABLED))
                           .text("停止分析")
                           .get());
        menuOps.add(builder.create(actionMap.get("manualRefreshing"))
                           .icon(getFontIcon(FluentUiRegularAL.ARROW_SYNC_20, 20, SLACK_LIGHT_BLUE))
                           .text("手动刷新")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("openConsole"))
                           .icon(consoleMenuIcon)
                           .text("打开命令行")
                           .get());
        menuOps.add(builder.create(actionMap.get("openCalc"))
                           .icon(getFontIcon(FluentUiRegularAL.CALCULATOR_20, 20, HP_BLUE))
                           .text("打开计算器")
                           .get());
        menuOps.add(builder.create(actionMap.get("openNotepad"))
                           .icon(getFontIcon(FluentUiRegularMZ.NOTEPAD_20, 20, HP_BLUE))
                           .text("打开记事本")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("drawNetworkGraph"))
                           .icon(getFontIcon(FluentUiRegularAL.GRID_20, 20, HP_BLUE))
                           .text("绘制网络结构图")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("exportInternalTemplates"))
                           .icon(getFontIcon(FluentUiRegularMZ.SHARE_20, 20, SLACK_LIGHT_BLUE))
                           .text("导出默认解析模板")
                           .get());
        menuOps.add(builder.create(actionMap.get("loadCustomTemplates"))
                           .icon(getFontIcon(FluentUiRegularAL.LAYER_20, 20, SLACK_LIGHT_BLUE))
                           .text("加载自定义解析模板")
                           .get());
        menuOps.addSeparator();
        menuOps.add(builder.create(actionMap.get("exitApp"))
                           .icon(getFontIcon(FluentUiRegularMZ.POWER_20, 20, INSTA_RED))
                           .text("退出(X)")
                           .mnemonic(KeyEvent.VK_X).get());

        JMenu menuViews = new JMenu("查看(V)");
        menuViews.setMnemonic(KeyEvent.VK_V);
        for (InfoView view : coreInfoViews)
        {
            view.setupMenu(menuViews);
        }
        if (!pluggedInfoViews.isEmpty())
        {
            JMenu extViews = new JMenu("扩展");
            extViews.setIcon(getFontIcon(FluentUiRegularAL.EXTENSION_20, 20, SLACK_LIGHT_BLUE));
            menuViews.add(extViews);
            for (InfoView view : pluggedInfoViews)
                view.setupMenu(extViews);
        }

        JMenu menuLogs = new JMenu("日志(L)");
        menuLogs.setMnemonic(KeyEvent.VK_L);
        // 日志视图做成可关闭的，所以以类似扩展InfoView的方式进行创建
        logsView.setupMenu(menuLogs);
        menuLogs.add(builder.create(actionMap.get("clearLogs"))
                            .icon(getFontIcon(FluentUiRegularAL.DELETE_20, 20, SLACK_LIGHT_BLUE))
                            .text("清空日志")
                            .get());
        menuLogs.add(builder.create(actionMap.get("checkLogs"))
                            .icon(getFontIcon(FluentUiRegularAL.DOCUMENT_COPY_20, 20, SLACK_LIGHT_BLUE))
                            .text("查看历史日志")
                            .get());

        JMenu menuHelp = new JMenu("帮助(H)");
        menuHelp.setMnemonic(KeyEvent.VK_H);
        menuHelp.add(builder.create(actionMap.get("showSystemInfo"))
                            .icon(getFontIcon(FluentUiRegularAL.BOOK_INFORMATION_24, 20, SLACK_LIGHT_BLUE))
                            .text("查看系统信息")
                            .get());
        menuHelp.add(builder.create(actionMap.get("showAbout"))
                            .icon(appIcon)
                            .text("关于 M2TK Assistant")
                            .mnemonic(KeyEvent.VK_A)
                            .get());

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuOps);
        menuBar.add(menuViews);
        menuBar.add(menuLogs);
        menuBar.add(menuHelp);
        frameView.setMenuBar(menuBar);
    }

    private void createAndSetupToolBar()
    {
        ButtonBuilder builder = new ButtonBuilder();

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(builder.create(actionMap.get("openLocalFile"))
                           .icon(getFontIcon(FluentUiRegularMZ.VIDEO_CLIP_20, 32, MS_ORANGE))
                           .disabledIcon(getFontIcon(FluentUiRegularMZ.VIDEO_CLIP_20, 32, DISABLED))
                           .text(null)
                           .tooltip("分析码流文件")
                           .get());
        toolBar.add(builder.create(actionMap.get("openMulticast"))
                           .icon(getFontIcon(FluentUiRegularAL.LIVE_20, 32, MS_GREEN))
                           .disabledIcon(getFontIcon(FluentUiRegularAL.LIVE_20, 32, DISABLED))
                           .text(null)
                           .tooltip("分析组播流")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("reopenInput"))
                           .icon(getFontIcon(FluentUiRegularAL.HISTORY_20, 28, SLACK_LIGHT_BLUE))
                           .disabledIcon(getFontIcon(FluentUiRegularAL.HISTORY_20, 28, DISABLED))
                           .text(null)
                           .tooltip("重新分析")
                           .get());
        toolBar.add(builder.create(actionMap.get("stopAnalyzer"))
                           .icon(getFontIcon(FluentUiRegularAL.DISMISS_CIRCLE_24, 28, SLACK_LIGHT_BLUE))
                           .disabledIcon(getFontIcon(FluentUiRegularAL.DISMISS_CIRCLE_24, 28, DISABLED))
                           .text(null)
                           .tooltip("停止分析")
                           .get());
        toolBar.add(builder.createToggle(new AbstractAction()
                           {
                               @Override
                               public void actionPerformed(ActionEvent e)
                               {
                                   JToggleButton toggle = (JToggleButton) e.getSource();
                                   if (toggle.isSelected())
                                       timer.stop();
                                   else if (analyzer.isRunning())
                                       timer.start();
                               }
                           })
                           .icon(getFontIcon(FluentUiRegularMZ.PAUSE_20, 28, SLACK_LIGHT_BLUE))
                           .text(null)
                           .tooltip("暂停刷新")
                           .get());
        toolBar.add(builder.create(actionMap.get("manualRefreshing"))
                           .icon(getFontIcon(FluentUiRegularAL.ARROW_SYNC_20, 28, SLACK_LIGHT_BLUE))
                           .text(null)
                           .tooltip("手动刷新")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("openConsole"))
                           .icon(consoleToolbarIcon)
                           .text(null)
                           .tooltip("打开命令行")
                           .get());
        toolBar.add(builder.create(actionMap.get("openCalc"))
                           .icon(getFontIcon(FluentUiRegularAL.CALCULATOR_20, 28, HP_BLUE))
                           .text(null)
                           .tooltip("打开计算器")
                           .get());
        toolBar.add(builder.create(actionMap.get("openNotepad"))
                           .icon(getFontIcon(FluentUiRegularMZ.NOTEPAD_28, 28, HP_BLUE))
                           .text(null)
                           .tooltip("打开记事本")
                           .get());
        toolBar.addSeparator();
        toolBar.add(builder.create(actionMap.get("drawNetworkGraph"))
                           .icon(getFontIcon(FluentUiRegularAL.GRID_28, 28, HP_BLUE))
                           .text(null)
                           .tooltip("绘制网络结构图")
                           .get());

        frameView.setToolBar(toolBar);
    }

    private void createAndSetupWorkspace()
    {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

        for (InfoView view : coreInfoViews)
        {
            tabbedPane.addTab(view.getViewTitle(), view.getViewIcon(), view.getViewComponent());
        }

        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupInitialStates()
    {
        initFileChooserCurrentDirectory();

        timer = new Timer(TIMER_INTERVAL_MILLIS, e -> refreshInfoViews());

        actionMap.get("openLocalFile").setEnabled(false);
        actionMap.get("openMulticast").setEnabled(false);
        actionMap.get("reopenInput").setEnabled(false);
        actionMap.get("stopAnalyzer").setEnabled(false);

        ComponentUtil.setPreferSizeAndLocateToCenter(frameView.getFrame(), 0.5, 0.5);
    }

    public void ready()
    {
        AssistantApp application = AssistantApp.getInstance();
        EventBus bus = application.getEventBus();
        M2TKDatabase database = application.getM2TKDatabase();

        for (InfoView view : infoViews)
            view.setupDataSource(bus, database);
        bus.register(this);

        actionMap.get("openLocalFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
        actionMap.get("openThirdPartyInputSource").setEnabled(true);

        this.bus = bus;
        this.database = database;
        this.analyzer = application.getStreamAnalyzer();
    }

    @Subscribe
    public void onShowInfoViewEvent(ShowInfoViewEvent event)
    {
        InfoView view = event.view();
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

        // 目标组件不在TabbedPane里，则添加该组件。
        JComponent viewComponent = view.getViewComponent();
        viewComponent.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        viewComponent.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                                        (IntConsumer) tabIndex -> tabbedPane.remove(tabIndex));
        tabbedPane.addTab(view.getViewTitle(), view.getViewIcon(), viewComponent);
        tabbedPane.setSelectedIndex(tabs);
    }

    @Action
    public void exitApp()
    {
        timer.stop();
        willQuit = true;
        frameView.getApplication().exit();
    }

    @Action
    public void clearLogs()
    {
        bus.post(new ClearLogsEvent());
    }

    @Action
    public void checkLogs()
    {
        try
        {
            File logsDir = FileUtil.file(FileUtil.getUserHomeDir(), "m2tk", "logs");
            Desktop.getDesktop().open(logsDir);
        } catch (IOException ex)
        {
            log.warn("打开日志目录异常：{}", ex.getMessage());
        }
    }

    @Action
    public void showSystemInfo()
    {
        SystemInfoDialog dialog = new SystemInfoDialog(frameView.getFrame());
        dialog.setVisible(true);
    }

    @Action
    public void showAbout()
    {
        AboutDialog dialog = new AboutDialog(frameView.getFrame());
        dialog.setVisible(true);
    }

    @Action
    public void openLocalFile()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(lastOpenDirectory.toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("码流文件（ts/m2ts/mpeg）", "ts", "m2ts", "mpeg"));

        if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(frameView.getFrame()))
        {
            File file = fileChooser.getSelectedFile();
            String input = file.toURI().toString();

            log.info("开始分析 {}", file);
            boolean started = false;
            try
            {
                started = analyzer.start(input, tracers, this::onAnalyzerStopped);
            } catch (Exception ex)
            {
                log.error("启动本地文件分析时异常：{}", ex.getMessage());
            }

            if (!started)
            {
                JOptionPane.showMessageDialog(frameView.getFrame(),
                                              "无法启动分析器，详情请查看日志",
                                              "程序异常",
                                              JOptionPane.ERROR_MESSAGE);
            } else
            {
                saveRecentFile(file);
                actionMap.get("openLocalFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("openThirdPartyInputSource").setEnabled(false);
                actionMap.get("reopenInput").setEnabled(false);
                actionMap.get("stopAnalyzer").setEnabled(true);
                timer.start();
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
                                          "参数错误",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean started = false;
        try
        {
            started = analyzer.start(input, tracers, this::onAnalyzerStopped);
        } catch (Exception ex)
        {
            log.error("启动组播流分析时异常：{}", ex.getMessage());
        }

        if (!started)
        {
            JOptionPane.showMessageDialog(frameView.getFrame(),
                                          "无法启动分析器，详情请查看日志",
                                          "程序异常",
                                          JOptionPane.ERROR_MESSAGE);
        } else
        {
            actionMap.get("openLocalFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            timer.start();
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

        boolean started = false;
        try
        {
            started = analyzer.start(input, tracers, this::onAnalyzerStopped);
        } catch (Exception ex)
        {
            log.error("启动外设源分析时异常：{}", ex.getMessage());
        }

        if (!started)
        {
            JOptionPane.showMessageDialog(frameView.getFrame(),
                                          "无法启动分析器，详情请查看日志",
                                          "程序异常",
                                          JOptionPane.ERROR_MESSAGE);
        } else
        {
            actionMap.get("openLocalFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            timer.start();
        }
    }

    @Action
    public void reopenInput()
    {
        Supplier<List<String>> query = () -> database.listStreamSourceUris();
        Consumer<List<String>> consumer = sourceUris ->
        {
            SourceHistoryDialog dialog = new SourceHistoryDialog(frameView.getFrame());
            ComponentUtil.setPreferSizeAndLocateToCenter(dialog, 0.5, 0.4);

            String source = dialog.selectFromSourceHistory(sourceUris);
            if (source == null)
                return;

            boolean started = false;
            try
            {
                started = analyzer.start(source, tracers, this::onAnalyzerStopped);
            } catch (Exception ex)
            {
                log.error("重启分析时异常：{}", ex.getMessage());
            }

            if (!started)
            {
                JOptionPane.showMessageDialog(frameView.getFrame(),
                                              "无法启动分析器，详情请查看日志",
                                              "程序异常",
                                              JOptionPane.ERROR_MESSAGE);
            } else
            {
                actionMap.get("openLocalFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("openThirdPartyInputSource").setEnabled(false);
                actionMap.get("reopenInput").setEnabled(false);
                actionMap.get("stopAnalyzer").setEnabled(true);
                timer.start();
            }
        };
        Consumer<Throwable> failed = t -> log.error("exception: {}", t.getMessage(), t);

        AsyncQueryTask<List<String>> task = new AsyncQueryTask<>(frameView.getApplication(), query, consumer, failed);
        task.execute();
    }

    @Action
    public void stopAnalyzer()
    {
        analyzer.stop();
    }

    @Action
    public void manualRefreshing()
    {
        bus.post(new RefreshInfoViewEvent());
    }

    @Action
    public void openConsole()
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
        ComponentUtil.setWaitingMouseCursor(frameView.getRootPane(), true);
        DrawNetworkDiagramTask task = new DrawNetworkDiagramTask(frameView.getApplication(), database);
        task.execute();
    }

    @Action
    public void exportInternalTemplates()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(FileUtil.getUserHomeDir());
        fileChooser.setSelectedFile(new File("模板.zip"));

        if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(frameView.getFrame()))
        {
            File file = fileChooser.getSelectedFile();

            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file)))
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
                                              "导出模板到 " + file, "导出成功",
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(FileUtil.file("template"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("模板文件（xml）", "xml"));

        if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(frameView.getFrame()))
        {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length > 0)
            {
                int count = loadUserDefinedTemplates(files);
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
        if (status.isRunning() || willQuit)
            return;

        JOptionPane.showMessageDialog(frameView.getFrame(), "分析过程结束");

        actionMap.get("openLocalFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
        actionMap.get("openThirdPartyInputSource").setEnabled(true);
        actionMap.get("reopenInput").setEnabled(true);
        actionMap.get("stopAnalyzer").setEnabled(false);
        timer.stop();
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

    private void refreshInfoViews()
    {
        if (bus != null)
            bus.post(new RefreshInfoViewEvent());
    }

    private Icon getFontIcon(Ikon ikon, int size, Color color)
    {
        return FontIcon.of(ikon, size, color);
    }

    private int loadUserDefinedTemplates(File[] files)
    {
        TemplateReader reader = new TemplateReader();

        int success = 0;
        for (File file : files)
        {
            if (loadTemplate(reader, file))
                success += 1;
        }
        return success;
    }

    private boolean loadTemplate(TemplateReader reader, File file)
    {
        if (file == null || !file.getName().endsWith(".xml"))
            return false;

        M2TKTemplate userTemplate = reader.parse(file);
        if (userTemplate == null)
            return false;

        userTemplate.getTableTemplates().forEach(SectionDecoder::registerTemplate);
        userTemplate.getDescriptorTemplates().forEach(DescriptorDecoder::registerTemplate);
        userTemplate.getSelectorTemplates().forEach(SelectorDecoder::registerTemplate);
        log.info("加载自定义模板：{}", file);

        return true;
    }
}
