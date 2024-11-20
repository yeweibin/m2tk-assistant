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

package m2tk.assistant.app.ui;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.EventBus;
import jnafilechooser.api.JnaFileChooser;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.app.Global;
import m2tk.assistant.app.ui.dialog.SourceHistoryDialog;
import m2tk.assistant.app.ui.dialog.SystemInfoDialog;
import m2tk.assistant.app.ui.task.DrawNetworkGraphTask;
import m2tk.assistant.app.ui.tracer.PSITracer;
import m2tk.assistant.app.ui.tracer.SITracer;
import m2tk.assistant.app.ui.tracer.StreamTracer;
import m2tk.assistant.app.ui.util.ComponentUtil;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainViewController
{
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    private final FrameView frameView;
    private final ActionMap actionMap;
//    private StreamInfoView streamInfoView;
//    private NetworkInfoView networkInfoView;
//    private TR290InfoView tr290InfoView;
//    private PCRInfoView pcrStatsView;
//    private EPGInfoView epgInfoView;
//    private NVODInfoView nvodInfoView;
//    private DatagramView datagramView;
//    private EBInfoView ebInfoView;

    private List<InfoView> infoViews;
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
        createAndSetupMenu();
        createAndSetupToolBar();
        createAndSetupWorkspace();
        setupInitialStates();
    }

    private void createAndSetupMenu()
    {
        JMenu menuSys = new JMenu("系统(S)");
        menuSys.setMnemonic(KeyEvent.VK_S);
        menuSys.add(createMenuItem("showSystemInfo", "系统信息", "查看运行时信息及系统环境变量"));
        menuSys.add(createMenuItem("exitApp", "退出(X)", "退出" + AssistantApp.APP_NAME, KeyEvent.VK_X));

        JMenu menuOps = new JMenu("操作(O)");
        menuOps.setMnemonic(KeyEvent.VK_O);
        JMenu sourceMenu = new JMenu("选择输入源");
        sourceMenu.add(createMenuItem("openFile", "文件", "读取本地码流文件"));
        sourceMenu.add(createMenuItem("openMulticast", "组播流", "读取组播流"));
        sourceMenu.add(createMenuItem("openThirdPartyInputSource", "扩展外设", "读取扩展输入设备"));
        menuOps.add(sourceMenu);
        menuOps.add(createMenuItem("reopenInput", "重新分析", "重新分析当前输入"));
        menuOps.add(createMenuItem("stopAnalyzer", "停止分析", "停止分析器"));
        menuOps.add(createMenuItem("pauseRefreshing", "暂停刷新", "暂停刷新"));
        menuOps.add(createMenuItem("startRefreshing", "继续刷新", "继续刷新"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("openTerminal", "打开命令行", "打开命令行程序"));
        menuOps.add(createMenuItem("openCalc", "打开计算器", "打开计算器程序"));
        menuOps.add(createMenuItem("openNotepad", "打开记事本", "打开记事本程序"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("drawNetworkGraph", "绘制网络结构图", "绘制网络结构图"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("exportInternalTemplates", "导出默认解析模板", "导出默认解析模板"));
        menuOps.add(createMenuItem("loadCustomTemplates", "加载自定义解析模板", "加载自定义解析模板"));

        JMenu menuViews = new JMenu("查看(V)");
        menuViews.setMnemonic(KeyEvent.VK_V);
        menuViews.add(createMenuItem("showStreamInfo", "基本信息", "查看基本信息"));
        menuViews.add(createMenuItem("showNetworkInfo", "网络信息", "查看网络信息"));
        menuViews.add(createMenuItem("showTR290Info", "报警信息", "查看报警信息"));
        menuViews.add(createMenuItem("showPCRInfo", "PCR信息", "查看PCR信息"));
        menuViews.add(createMenuItem("showEPGInfo", "EPG信息", "查看EPG信息"));
        menuViews.add(createMenuItem("showNVODInfo", "NVOD信息", "查看NVOD信息"));
        menuViews.add(createMenuItem("showPSISIInfo", "PSI/SI信息", "查看PSI/SI信息"));
        menuViews.add(createMenuItem("showEBInfo", "应急广播信息", "查看应急广播信息"));

        JMenu menuLogs = new JMenu("日志(L)");
        menuLogs.setMnemonic(KeyEvent.VK_L);
        menuLogs.add(createMenuItem("clearLogs", "清空日志", "清空日志"));
        menuLogs.add(createMenuItem("checkLogs", "查看历史日志", "查看历史日志"));

        JMenu menuHelp = new JMenu("帮助(H)");
        menuHelp.setMnemonic(KeyEvent.VK_H);
        menuHelp.add(createMenuItem("showHelp", "帮助", "帮助"));
        menuHelp.add(createMenuItem("showAbout", "关于(A)", "关于 " + AssistantApp.APP_NAME, KeyEvent.VK_A));

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
        ResourceMap resourceMap = frameView.getResourceMap();

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton btnOpenFile = createButton("openFile", "分析码流文件");
        btnOpenFile.setIcon(resourceMap.getIcon("toolbar.openFile.icon"));
        btnOpenFile.setText(null);
        JButton btnOpenMulticast = createButton("openMulticast", "分析组播流");
        btnOpenMulticast.setIcon(resourceMap.getIcon("toolbar.openMulticast.icon"));
        btnOpenMulticast.setText(null);
        JButton btnReopenInput = createButton("reopenInput", "重新分析");
        btnReopenInput.setIcon(resourceMap.getIcon("toolbar.reopenInput.icon"));
        btnReopenInput.setText(null);
        JButton btnStopAnalysing = createButton("stopAnalyzer", "停止分析");
        btnStopAnalysing.setIcon(resourceMap.getIcon("toolbar.stopAnalyzer.icon"));
        btnStopAnalysing.setText(null);
        JButton btnPauseRefreshing = createButton("pauseRefreshing", "暂停刷新");
        btnPauseRefreshing.setIcon(resourceMap.getIcon("toolbar.pauseRefreshing.icon"));
        btnPauseRefreshing.setText(null);
        JButton btnStartRefreshing = createButton("startRefreshing", "继续刷新");
        btnStartRefreshing.setIcon(resourceMap.getIcon("toolbar.startRefreshing.icon"));
        btnStartRefreshing.setText(null);
        JButton btnOpenTerminal = createButton("openTerminal", "打开命令行");
        btnOpenTerminal.setIcon(resourceMap.getIcon("toolbar.openTerminal.icon"));
        btnOpenTerminal.setText(null);
        JButton btnOpenCalc = createButton("openCalc", "打开计算器");
        btnOpenCalc.setIcon(resourceMap.getIcon("toolbar.openCalc.icon"));
        btnOpenCalc.setText(null);
        JButton btnOpenNotepad = createButton("openNotepad", "打开记事本");
        btnOpenNotepad.setIcon(resourceMap.getIcon("toolbar.openNotepad.icon"));
        btnOpenNotepad.setText(null);
        JButton btnDrawNetworkGraph = createButton("drawNetworkGraph", "绘制网络结构图");
        btnDrawNetworkGraph.setIcon(resourceMap.getIcon("toolbar.drawNetworkGraph.icon"));
        btnDrawNetworkGraph.setText(null);

        toolBar.add(btnOpenFile);
        toolBar.add(btnOpenMulticast);
        toolBar.addSeparator();
        toolBar.add(btnReopenInput);
        toolBar.add(btnStopAnalysing);
        toolBar.add(btnPauseRefreshing);
        toolBar.add(btnStartRefreshing);
        toolBar.addSeparator();
        toolBar.add(btnOpenTerminal);
        toolBar.add(btnOpenCalc);
        toolBar.add(btnOpenNotepad);
        toolBar.addSeparator();
        toolBar.add(btnDrawNetworkGraph);

        frameView.setToolBar(toolBar);
    }

    private void createAndSetupWorkspace()
    {
//        streamInfoView = new StreamInfoView(frameView);
//        networkInfoView = new NetworkInfoView(frameView);
//        tr290InfoView = new TR290InfoView(frameView);
//        pcrStatsView = new PCRInfoView(frameView);
//        epgInfoView = new EPGInfoView(frameView);
////        nvodInfoView = new NVODInfoView(frameView);
//        datagramView = new DatagramView(frameView);
//        ebInfoView = new EBInfoView(frameView);
        logsView = new LogsView();

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

//        PluginManager pluginManager = new DefaultPluginManager(Paths.get("D:\\Projects\\m2tk\\m2tk-assistant\\modules\\assistant-app\\plugins"));
//        pluginManager.loadPlugins();
//        pluginManager.startPlugins();

        PluginManager pluginManager = new DefaultPluginManager();
        infoViews = pluginManager.getExtensions(InfoView.class);
        for (InfoView view : infoViews)
        {
            logger.info("view: {}", view.getViewTitle());
            view.setupApplication(frameView.getApplication());
            tabbedPane.add(view.getViewTitle(), view.getViewComponent());
        }

//        tabbedPane.add("基本信息", streamInfoView);
//        tabbedPane.add("网络信息", networkInfoView);
//        tabbedPane.add("TR 101 290", tr290InfoView);
//        tabbedPane.add("PCR", pcrStatsView);
//        tabbedPane.add("EPG", epgInfoView);
////        tabbedPane.add("NVOD", nvodInfoView);
//        tabbedPane.add("PSI/SI", datagramView);
//        tabbedPane.add("应急广播", ebInfoView);
        tabbedPane.add("日志", logsView);

        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupInitialStates()
    {
        frameView.getFrame().setTitle(AssistantApp.APP_NAME);

        initFileChooserCurrentDirectory();

        actionMap.get("openFile").setEnabled(false);
        actionMap.get("openMulticast").setEnabled(false);
        actionMap.get("reopenInput").setEnabled(false);
        actionMap.get("stopAnalyzer").setEnabled(false);
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(false);
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

    private JButton createButton(String action, String tooltip)
    {
        JButton button = new JButton();
        button.setAction(actionMap.get(action));
        button.setToolTipText(tooltip);
        return button;
    }

    public void ready()
    {
        EventBus bus = AssistantApp.getInstance().getEventBus();
        M2TKDatabase database = AssistantApp.getInstance().getM2TKDatabase();

        for (InfoView view : infoViews)
        {
            view.setupDataSource(bus, database);
        }
//        streamInfoView.setupDataSource(bus, database);
//        networkInfoView.setupDataSource(bus, database);
//        tr290InfoView.setupDataSource(bus, database);
//        pcrStatsView.setupDataSource(bus, database);
//        epgInfoView.setupDataSource(bus, database);
//        datagramView.setupDataSource(bus, database);
//        ebInfoView.setupDataSource(bus, database);

        actionMap.get("openFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
    }

    @Action
    public void showSystemInfo()
    {
        SystemInfoDialog dialog = new SystemInfoDialog(frameView.getFrame());
        ComponentUtil.setPreferSizeAndLocateToCenter(dialog, 0.4, 0.64);
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
            logger.warn("打开日志目录异常：{}", ex.getMessage());
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
        JOptionPane.showMessageDialog(frameView.getFrame(),
                                      String.format("%s %s%n©%s %s",
                                                    AssistantApp.APP_NAME, AssistantApp.APP_VERSION,
                                                    AssistantApp.APP_COPYRIGHT_YEAR, AssistantApp.APP_VENDOR),
                                      String.format("关于 %s", AssistantApp.APP_NAME),
                                      JOptionPane.INFORMATION_MESSAGE,
                                      frameView.getResourceMap().getIcon("Application.icon"));
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
            logger.info("准备分析");
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
                logger.info("开始分析：{}", started);
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
            logger.warn("打开命令行程序异常：{}", ex.getMessage());
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
            logger.warn("打开计算器程序异常：{}", ex.getMessage());
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
            logger.warn("打开记事本程序异常：{}", ex.getMessage());
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
            logger.debug("无法设置文件选择器起始路径：{}", ex.getMessage());
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
            logger.debug("无法保存最近使用的文件：{}", ex.getMessage());
        }
    }
}
