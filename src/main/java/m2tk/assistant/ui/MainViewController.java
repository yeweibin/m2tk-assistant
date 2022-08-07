package m2tk.assistant.ui;

import m2tk.assistant.AssistantApp;
import m2tk.assistant.Global;
import m2tk.assistant.ui.dialog.SystemInfoDialog;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ListModelOutputStream;
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
import java.net.InetAddress;
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
        menuOps.add(sourceMenu);
        menuOps.add(createMenuItem("stopAnalyzer", "停止分析", "停止分析器"));

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
        JButton btnStopAnalysing = createButton("stopAnalyzer", "停止分析");
        btnStopAnalysing.setIcon(resourceMap.getIcon("toolbar.stopAnalyzer.icon"));
        btnStopAnalysing.setText(null);

        toolBar.add(btnOpenFile);
        toolBar.add(btnOpenMulticast);
        toolBar.addSeparator();
        toolBar.add(btnStopAnalysing);

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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.add("基本信息", streamGeneralInfoView);
        tabbedPane.add("日志", new JScrollPane(logsView));
        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Bind ListModel to LogAppender
        TextListLogAppender.setStaticOutputStream(new ListModelOutputStream(logsModel));
    }

    private void setupInitialStates()
    {
        frameView.getFrame().setTitle(AssistantApp.APP_NAME);

        actionMap.get("stopAnalyzer").setEnabled(false);
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
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(Paths.get(System.getProperty("user.home"), "Downloads", "TS").toFile());
        int retCode = chooser.showOpenDialog(frameView.getFrame());

        if (retCode == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();
            String uri = file.toURI().toASCIIString();
            if (!Global.getStreamAnalyser().start(uri, this::onAnalyzerStopped))
            {
                JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
            } else
            {
                actionMap.get("openFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("stopAnalyzer").setEnabled(true);
            }
        }
    }

    @Action
    public void openMulticast()
    {
        String input = JOptionPane.showInputDialog(frameView.getFrame(),
                                                 "组播地址",
                                                 "udp://224.0.0.1:7890");

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
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            streamGeneralInfoView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
        }
    }

    @Action
    public void stopAnalyzer()
    {
        Global.getStreamAnalyser().stop();
    }

    private void onAnalyzerStopped(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        JOptionPane.showMessageDialog(frameView.getFrame(), "分析终止。");
        actionMap.get("stopAnalyzer").setEnabled(false);
        actionMap.get("openFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
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
}
