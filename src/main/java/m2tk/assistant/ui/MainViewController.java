package m2tk.assistant.ui;

import cn.hutool.core.io.FileUtil;
import m2tk.assistant.AssistantApp;
import m2tk.assistant.Global;
import m2tk.assistant.ui.dialog.SystemInfoDialog;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ListModelOutputStream;
import m2tk.assistant.ui.view.EPGInfoView;
import m2tk.assistant.ui.view.NetworkInfoView;
import m2tk.assistant.ui.view.StreamGeneralInfoView;
import m2tk.assistant.util.TextListLogAppender;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainViewController
{
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    private final FrameView frameView;
    private final ActionMap actionMap;
    private DefaultListModel<String> logsModel;
    private StreamGeneralInfoView streamGeneralInfoView;
    private NetworkInfoView networkInfoView;
    private EPGInfoView epgInfoView;
    private JFileChooser fileChooser;
    private volatile boolean willQuit;
    private String lastInput = null;

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
        createAndSetupStatusBar();
        createAndSetupWorkspace();
        setupInitialStates();
    }

    private void createAndSetupMenu()
    {
        JMenu menuSys = new JMenu("系统");
        menuSys.add(createMenuItem("showSystemInfo", "系统信息", "查看运行时信息及系统环境变量"));
        menuSys.add(createMenuItem("exitApp", "退出", "退出" + AssistantApp.APP_NAME));

        JMenu menuOps = new JMenu("操作");
        JMenu sourceMenu = new JMenu("选择输入源");
        sourceMenu.add(createMenuItem("openFile", "文件", "读取本地码流文件"));
        sourceMenu.add(createMenuItem("openMulticast", "组播流", "读取组播流"));
        sourceMenu.add(createMenuItem("openThirdPartyInputSource", "扩展外设", "读取扩展输入设备"));
        menuOps.add(sourceMenu);
        menuOps.add(createMenuItem("reopenLastInput", "重新分析", "重新分析当前输入"));
        menuOps.add(createMenuItem("stopAnalyzer", "停止分析", "停止分析器"));
        menuOps.add(createMenuItem("pauseRefreshing", "暂停刷新", "暂停刷新"));
        menuOps.add(createMenuItem("startRefreshing", "继续刷新", "继续刷新"));

        JMenu menuLogs = new JMenu("日志");
        menuLogs.add(createMenuItem("clearLogs", "清空日志", "清空日志"));
        menuLogs.add(createMenuItem("checkLogs", "查看历史日志", "查看历史日志"));

        JMenu menuHelp = new JMenu("帮助");
        menuHelp.add(createMenuItem("showHelp", "帮助", "帮助"));
        menuHelp.add(createMenuItem("showAbout", "关于", "关于 " + AssistantApp.APP_NAME));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuSys);
        menuBar.add(menuOps);
        menuBar.add(menuLogs);
        menuBar.add(menuHelp);
        frameView.setMenuBar(menuBar);
    }

    private void createAndSetupToolBar()
    {
        ResourceMap resourceMap = frameView.getResourceMap();

        JToolBar toolBar = new JToolBar();
        JButton btnOpenFile = createButton("openFile", "分析码流文件");
        btnOpenFile.setIcon(resourceMap.getIcon("toolbar.openFile.icon"));
        btnOpenFile.setText(null);
        JButton btnOpenMulticast = createButton("openMulticast", "分析组播流");
        btnOpenMulticast.setIcon(resourceMap.getIcon("toolbar.openMulticast.icon"));
        btnOpenMulticast.setText(null);
        JButton btnReopenLastInput = createButton("reopenLastInput", "重新分析");
        btnReopenLastInput.setIcon(resourceMap.getIcon("toolbar.reopenLastInput.icon"));
        btnReopenLastInput.setText(null);
        JButton btnStopAnalysing = createButton("stopAnalyzer", "停止分析");
        btnStopAnalysing.setIcon(resourceMap.getIcon("toolbar.stopAnalyzer.icon"));
        btnStopAnalysing.setText(null);
        JButton btnPauseRefreshing = createButton("pauseRefreshing", "暂停刷新");
        btnPauseRefreshing.setIcon(resourceMap.getIcon("toolbar.pauseRefreshing.icon"));
        btnPauseRefreshing.setText(null);
        JButton btnStartRefreshing = createButton("startRefreshing", "继续刷新");
        btnStartRefreshing.setIcon(resourceMap.getIcon("toolbar.startRefreshing.icon"));
        btnStartRefreshing.setText(null);

        toolBar.add(btnOpenFile);
        toolBar.add(btnOpenMulticast);
        toolBar.addSeparator();
        toolBar.add(btnReopenLastInput);
        toolBar.add(btnStopAnalysing);
        toolBar.add(btnPauseRefreshing);
        toolBar.add(btnStartRefreshing);

        frameView.setToolBar(toolBar);
    }

    private void createAndSetupStatusBar()
    {
    }

    private void createAndSetupWorkspace()
    {
        logsModel = new DefaultListModel<>();
        JList<String> logsView = new JList<>();
        logsView.setModel(logsModel);
        logsView.setDragEnabled(false);
        logsView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        streamGeneralInfoView = new StreamGeneralInfoView(frameView);
        networkInfoView = new NetworkInfoView(frameView);
        epgInfoView = new EPGInfoView(frameView);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.add("基本信息", streamGeneralInfoView);
        tabbedPane.add("网络信息", networkInfoView);
        tabbedPane.add("EPG全览", epgInfoView);
        tabbedPane.add("日志", new JScrollPane(logsView));
        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Bind ListModel to LogAppender
        TextListLogAppender.setStaticOutputStream(new ListModelOutputStream(logsModel));
    }

    private void setupInitialStates()
    {
        frameView.getFrame().setTitle(AssistantApp.APP_NAME);

        fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        initFileChooserCurrentDirectory(fileChooser);

        actionMap.get("reopenLastInput").setEnabled(false);
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

    private JButton createButton(String action, String tooltip)
    {
        JButton button = new JButton();
        button.setAction(actionMap.get(action));
        button.setToolTipText(tooltip);
        return button;
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
        logsModel.clear();
    }

    @Action
    public void showAbout()
    {
        JOptionPane.showMessageDialog(frameView.getFrame(),
                                      String.format("%s %s%n©2022 %s", AssistantApp.APP_NAME, AssistantApp.APP_VERSION, AssistantApp.APP_VENDOR),
                                      String.format("关于 %s", AssistantApp.APP_NAME),
                                      JOptionPane.INFORMATION_MESSAGE,
                                      frameView.getResourceMap().getIcon("Application.icon"));
    }

    @Action
    public void openFile()
    {
        int retCode = fileChooser.showOpenDialog(frameView.getFrame());

        if (retCode == JFileChooser.APPROVE_OPTION)
        {
            File file = fileChooser.getSelectedFile();
            fileChooser.setCurrentDirectory(file.getParentFile());
            String input = file.toURI().toASCIIString();
            if (!Global.getStreamAnalyser().start(input, this::onAnalyzerStopped))
            {
                lastInput = null;
                actionMap.get("reopenLastInput").setEnabled(false);
                JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
            } else
            {
                saveRecentFile(file);
                lastInput = input;
                streamGeneralInfoView.reset();
                networkInfoView.reset();
                epgInfoView.reset();
                actionMap.get("openFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("openThirdPartyInputSource").setEnabled(false);
                actionMap.get("reopenLastInput").setEnabled(false);
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
                                                   "udp://224.0.0.1:7890");
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

        if (!Global.getStreamAnalyser().start(input, this::onAnalyzerStopped))
        {
            lastInput = null;
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            lastInput = input;
            streamGeneralInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenLastInput").setEnabled(false);
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

        if (!Global.getStreamAnalyser().start(input, this::onAnalyzerStopped))
        {
            lastInput = null;
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            lastInput = input;
            streamGeneralInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenLastInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);
        }
    }

    @Action
    public void reopenLastInput()
    {
        if (lastInput == null)
        {
            actionMap.get("reopenLastInput").setEnabled(false);
            return;
        }

        if (!Global.getStreamAnalyser().start(lastInput, this::onAnalyzerStopped))
        {
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            streamGeneralInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenLastInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);
        }
    }

    @Action
    public void stopAnalyzer()
    {
        Global.getStreamAnalyser().stop();
    }

    @Action
    public void pauseRefreshing()
    {
        streamGeneralInfoView.stopRefreshing();
        networkInfoView.stopRefreshing();
        epgInfoView.stopRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(true);
    }

    @Action
    public void startRefreshing()
    {
        streamGeneralInfoView.startRefreshing();
        networkInfoView.startRefreshing();
        epgInfoView.startRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(true);
        actionMap.get("startRefreshing").setEnabled(false);
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
        actionMap.get("reopenLastInput").setEnabled(true);
        actionMap.get("stopAnalyzer").setEnabled(false);
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(false);
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

    public void setWillQuit()
    {
        willQuit = true;
    }

    private void initFileChooserCurrentDirectory(JFileChooser fileChooser)
    {
        try
        {
            Path pwd = Paths.get(System.getProperty("user.dir"));
            Path recentCfg = pwd.resolve("recent.cfg");
            String recentFile = FileUtil.readUtf8String(recentCfg.toFile());
            fileChooser.setCurrentDirectory(Paths.get(recentFile).toFile());
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
