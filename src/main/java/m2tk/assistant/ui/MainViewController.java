package m2tk.assistant.ui;

import cn.hutool.core.io.FileUtil;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import m2tk.assistant.AssistantApp;
import m2tk.assistant.Global;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SIMultiplexEntity;
import m2tk.assistant.dbi.entity.SINetworkEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.dialog.NetworkGraphDialog;
import m2tk.assistant.ui.dialog.SystemInfoDialog;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ListModelOutputStream;
import m2tk.assistant.ui.view.*;
import m2tk.assistant.util.TextListLogAppender;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.*;

public class MainViewController
{
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    private final FrameView frameView;
    private final ActionMap actionMap;
    private DefaultListModel<String> logsModel;
    private StreamInfoView streamInfoView;
    private NetworkInfoView networkInfoView;
    private TR290InfoView tr290InfoView;
    private PCRInfoView pcrStatsView;
    private EPGInfoView epgInfoView;
    private DatagramView datagramView;
    private JTabbedPane tabbedPane;
    private JFileChooser fileChooser;
    private volatile boolean willQuit;
    private String lastInput = null;
    private NetworkGraphDialog networkGraphDialog;


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
        menuOps.add(createMenuItem("reopenLastInput", "重新分析", "重新分析当前输入"));
        menuOps.add(createMenuItem("stopAnalyzer", "停止分析", "停止分析器"));
        menuOps.add(createMenuItem("pauseRefreshing", "暂停刷新", "暂停刷新"));
        menuOps.add(createMenuItem("startRefreshing", "继续刷新", "继续刷新"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("openTerminal", "打开命令行", "打开命令行程序"));
        menuOps.add(createMenuItem("openCalc", "打开计算器", "打开计算器程序"));
        menuOps.add(createMenuItem("openNotepad", "打开记事本", "打开记事本程序"));

        JMenu menuViews = new JMenu("查看(V)");
        menuViews.setMnemonic(KeyEvent.VK_V);
        menuViews.add(createMenuItem("showStreamInfo", "基本信息", "查看基本信息"));
        menuViews.add(createMenuItem("showNetworkInfo", "网络信息", "查看网络信息"));
        menuViews.add(createMenuItem("showTR290Info", "报警信息", "查看报警信息"));
        menuViews.add(createMenuItem("showPCRInfo", "PCR信息", "查看PCR信息"));
        menuViews.add(createMenuItem("showEPGInfo", "EPG信息", "查看EPG信息"));
        menuViews.add(createMenuItem("showPSISIInfo", "PSI/SI信息", "查看PSI/SI信息"));

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
        toolBar.add(btnReopenLastInput);
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
        logsModel = new DefaultListModel<>();
        JList<String> logsView = new JList<>();
        logsView.setModel(logsModel);
        logsView.setDragEnabled(false);
        logsView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        streamInfoView = new StreamInfoView(frameView);
        networkInfoView = new NetworkInfoView(frameView);
        tr290InfoView = new TR290InfoView(frameView);
        pcrStatsView = new PCRInfoView(frameView);
        epgInfoView = new EPGInfoView(frameView);
        datagramView = new DatagramView(frameView);

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.add("基本信息", streamInfoView);
        tabbedPane.add("网络信息", networkInfoView);
        tabbedPane.add("TR 101 290", tr290InfoView);
        tabbedPane.add("PCR", pcrStatsView);
        tabbedPane.add("EPG", epgInfoView);
        tabbedPane.add("PSI/SI", datagramView);
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

        networkGraphDialog = new NetworkGraphDialog();

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
        tabbedPane.setSelectedComponent(streamInfoView);
    }

    @Action
    public void showNetworkInfo()
    {
        tabbedPane.setSelectedComponent(networkInfoView);
    }

    @Action
    public void showTR290Info()
    {
        tabbedPane.setSelectedComponent(tr290InfoView);
    }

    @Action
    public void showPCRInfo()
    {
        tabbedPane.setSelectedComponent(pcrStatsView);
    }

    @Action
    public void showEPGInfo()
    {
        tabbedPane.setSelectedComponent(epgInfoView);
    }

    @Action
    public void showPSISIInfo()
    {
        tabbedPane.setSelectedComponent(datagramView);
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
            boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
            if (!started)
            {
                lastInput = null;
                actionMap.get("reopenLastInput").setEnabled(false);
                JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
            } else
            {
                saveRecentFile(file);
                lastInput = input;
                streamInfoView.reset();
                networkInfoView.reset();
                epgInfoView.reset();
                tr290InfoView.reset();
                pcrStatsView.reset();
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

        boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
        if (!started)
        {
            lastInput = null;
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            lastInput = input;
            streamInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            tr290InfoView.reset();
            pcrStatsView.reset();
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

        boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
        if (!started)
        {
            lastInput = null;
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            lastInput = input;
            streamInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            tr290InfoView.reset();
            pcrStatsView.reset();
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

        boolean started = Global.getStreamAnalyser().start(lastInput, this::onAnalyzerStopped);
        if (!started)
        {
            JOptionPane.showMessageDialog(frameView.getFrame(), "无法启动分析器", "请注意", JOptionPane.WARNING_MESSAGE);
        } else
        {
            streamInfoView.reset();
            networkInfoView.reset();
            epgInfoView.reset();
            tr290InfoView.reset();
            pcrStatsView.reset();
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
        streamInfoView.stopRefreshing();
        networkInfoView.stopRefreshing();
        epgInfoView.stopRefreshing();
        tr290InfoView.stopRefreshing();
        pcrStatsView.stopRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(true);
    }

    @Action
    public void startRefreshing()
    {
        streamInfoView.startRefreshing();
        networkInfoView.startRefreshing();
        epgInfoView.startRefreshing();
        tr290InfoView.startRefreshing();
        pcrStatsView.startRefreshing();
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
        Supplier<BufferedImage> query = () -> {
            Map<String, Node> nodes = new HashMap<>();
            DatabaseService databaseService = Global.getDatabaseService();
            List<SINetworkEntity> networks = databaseService.listNetworks();
            List<SIMultiplexEntity> multiplexes = databaseService.listMultiplexes();
            List<SIServiceEntity> services = databaseService.listServices();

            int actualTransportStreamId = -1;
            Map<String, Object> siObjects = new HashMap<>();
            Map<String, List<String>> muxSrvMap = new HashMap<>();
            Map<String, List<String>> netMuxMap = new HashMap<>();
            for (SIServiceEntity service : services)
            {
                String srvKey = String.format("service.%d.%d.%d",
                                              service.getServiceId(),
                                              service.getTransportStreamId(),
                                              service.getOriginalNetworkId());
                siObjects.put(srvKey, service);
                nodes.put(srvKey, createServiceNode(service));

                String muxKey = String.format("multiplex.%d.%d",
                                              service.getTransportStreamId(),
                                              service.getOriginalNetworkId());
                muxSrvMap.computeIfAbsent(muxKey, k -> new ArrayList<>()).add(srvKey);

                if (service.isActualTransportStream())
                    actualTransportStreamId = service.getTransportStreamId();
            }

            for (SIMultiplexEntity multiplex : multiplexes)
            {
                String muxKey = String.format("multiplex.%d.%d",
                                              multiplex.getTransportStreamId(),
                                              multiplex.getOriginalNetworkId());
                siObjects.put(muxKey, multiplex);
                nodes.put(muxKey, createMultiplexNode(multiplex, siObjects, muxSrvMap, actualTransportStreamId));

                String netKey = String.format("network.%d", multiplex.getNetworkId());
                netMuxMap.computeIfAbsent(netKey, k -> new ArrayList<>()).add(muxKey);
            }

            for (SINetworkEntity network : networks)
            {
                String netKey = String.format("network.%d", network.getNetworkId());
                siObjects.put(netKey, network);
                nodes.put(netKey, createNetworkNode(network, siObjects, netMuxMap));
            }

            networks.forEach(network -> siObjects.put(String.format("network.%d", network.getNetworkId()),
                                                      network));
            multiplexes.forEach(multiplex -> siObjects.put(String.format("ts.%d.%d",
                                                                         multiplex.getTransportStreamId(),
                                                                         multiplex.getOriginalNetworkId()),
                                                           multiplex));
            services.forEach(service -> siObjects.put(String.format("service.%d.%d.%d",
                                                                    service.getServiceId(),
                                                                    service.getTransportStreamId(),
                                                                    service.getOriginalNetworkId()),
                                                      service));

            List<LinkSource> linkSources = new ArrayList<>();
            for (String netKey : netMuxMap.keySet())
            {
                Node netNode = nodes.get(netKey);
                List<String> muxKeys = netMuxMap.get(netKey);
                for (String muxKey : muxKeys)
                {
                    Node muxNode = nodes.get(muxKey);
                    if (muxNode != null)
                        linkSources.add(netNode.link(between(port(muxKey), muxNode.port("id"))));
                }
            }
            for (String muxKey : muxSrvMap.keySet())
            {
                Node muxNode = nodes.get(muxKey);
                if (muxNode != null)
                {
                    List<String> srvKeys = muxSrvMap.get(muxKey);
                    for (String srvKey : srvKeys)
                    {
                        Node srvNode = nodes.get(srvKey);
                        if (srvNode != null)
                            linkSources.add(muxNode.link(between(port(srvKey), srvNode.port("id"))));
                    }
                }
            }

            Graph graph = graph("NetworkGraph")
                    .directed()
                    .graphAttr().with(Rank.dir(LEFT_TO_RIGHT))
                    .nodeAttr().with(attr("fontname", "SimSun"))
                    .linkAttr().with("class", "link-class")
                    .with(linkSources);
            return Graphviz.fromGraph(graph).engine(Engine.DOT).render(Format.PNG).toImage();
        };

        Consumer<BufferedImage> consumer = networkGraphDialog::showImage;

        AsyncQueryTask<BufferedImage> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                                  query,
                                                                  consumer);
        task.execute();
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

    private Node createServiceNode(SIServiceEntity service)
    {
        return Factory.node(String.format("service.%d", service.getServiceId()))
                      .with("shape", "record")
                      .with(Records.of(Records.turn(Records.rec("id", String.format("Service %d", service.getServiceId())),
                                                    Records.rec(String.format("transport_stream_id = %d%n" +
                                                                              "original_network_id = %d",
                                                                              service.getTransportStreamId(),
                                                                              service.getOriginalNetworkId())),
                                                    Records.rec(String.format("type = 0x%02X（%s）", service.getServiceType(), service.getServiceTypeName())),
                                                    Records.rec(String.format("name = %s%n" +
                                                                              "provider = %s",
                                                                              service.getServiceName(),
                                                                              service.getServiceProvider())),
                                                    Records.rec(String.format("running_status = %s%n" +
                                                                              "free_CA_mode = %d%n" +
                                                                              "EIT_P/f_flag = %d%n" +
                                                                              "EIT_Sch_flag = %d",
                                                                              service.getRunningStatus(),
                                                                              service.isFreeCAMode() ? 0 : 1,
                                                                              service.isPresentFollowingEITEnabled() ? 1 : 0,
                                                                              service.isScheduleEITEnabled() ? 1 : 0
                                                                             )))));
    }

    private Node createMultiplexNode(SIMultiplexEntity multiplex,
                                     Map<String, Object> siObjects,
                                     Map<String, List<String>> muxSrvMap,
                                     int actualTransportStreamId)
    {
        List<String> records = new ArrayList<>();
        records.add(Records.rec("id",
                                String.format("TransportStream %d%s",
                                              multiplex.getTransportStreamId(),
                                              (multiplex.getTransportStreamId() == actualTransportStreamId)
                                              ? "[当前传输流]" : "")));
        records.add(Records.rec(String.format("original_network_id = %d", multiplex.getOriginalNetworkId())));
        records.add(Records.rec(String.format("delivery_system_type = %s", multiplex.getDeliverySystemType())));

        String muxKey = String.format("multiplex.%d.%d",
                                      multiplex.getTransportStreamId(),
                                      multiplex.getOriginalNetworkId());
        List<String> srvKeys = muxSrvMap.getOrDefault(muxKey, Collections.emptyList());
        for (String srvKey : srvKeys)
        {
            SIServiceEntity service = (SIServiceEntity) siObjects.get(srvKey);
            records.add(Records.rec(srvKey, String.format("Service %d", service.getServiceId())));
        }

        return Factory.node(String.format("multiplex.%d", multiplex.getTransportStreamId()))
                      .with("shape", "record")
                      .with(Records.of(Records.turn(records.toArray(new String[0]))));
    }

    private Node createNetworkNode(SINetworkEntity network,
                                   Map<String, Object> siObjects,
                                   Map<String, List<String>> netMuxMap)
    {
        List<String> records = new ArrayList<>();
        records.add(Records.rec("id", String.format("Network %d", network.getNetworkId())));
        records.add(Records.rec(String.format("name = %s", network.getNetworkName())));

        String netKey = String.format("network.%d", network.getNetworkId());
        List<String> muxKeys = netMuxMap.getOrDefault(netKey, Collections.emptyList());
        for (String muxKey : muxKeys)
        {
            SIMultiplexEntity multiplex = (SIMultiplexEntity) siObjects.get(muxKey);
            records.add(Records.rec(muxKey, String.format("TransportStream %d", multiplex.getTransportStreamId())));
        }

        return Factory.node(String.format("network.%d", network.getNetworkId()))
                      .with("shape", "record")
                      .with(Records.of(Records.turn(records.toArray(new String[0]))));
    }
}
