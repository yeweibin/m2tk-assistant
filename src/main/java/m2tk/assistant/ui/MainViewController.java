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

package m2tk.assistant.ui;

import cn.hutool.core.io.FileUtil;
import com.google.common.eventbus.Subscribe;
import m2tk.assistant.AssistantApp;
import m2tk.assistant.Global;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.ui.dialog.SystemInfoDialog;
import m2tk.assistant.ui.event.SourceAttachedEvent;
import m2tk.assistant.ui.event.SourceChangedEvent;
import m2tk.assistant.ui.task.DrawNetworkGraphTask;
import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.view.*;
import m2tk.multiplex.DemuxStatus;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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
    private SourceListView sourceListView;
    private StreamInfoView streamInfoView;
    private NetworkInfoView networkInfoView;
    private TR290InfoView tr290InfoView;
    private PCRInfoView pcrStatsView;
    private EPGInfoView epgInfoView;
    private NVODInfoView nvodInfoView;
    private DatagramView datagramView;
    private EBInfoView ebInfoView;
    private LogsView logsView;
    private JTabbedPane tabbedPane;
    private JFileChooser fileChooser;
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
        JMenu menuSys = new JMenu("??????(S)");
        menuSys.setMnemonic(KeyEvent.VK_S);
        menuSys.add(createMenuItem("showSystemInfo", "????????????", "??????????????????????????????????????????"));
        menuSys.add(createMenuItem("exitApp", "??????(X)", "??????" + AssistantApp.APP_NAME, KeyEvent.VK_X));

        JMenu menuOps = new JMenu("??????(O)");
        menuOps.setMnemonic(KeyEvent.VK_O);
        JMenu sourceMenu = new JMenu("???????????????");
        sourceMenu.add(createMenuItem("openFile", "??????", "????????????????????????"));
        sourceMenu.add(createMenuItem("openMulticast", "?????????", "???????????????"));
        sourceMenu.add(createMenuItem("openThirdPartyInputSource", "????????????", "????????????????????????"));
        menuOps.add(sourceMenu);
        menuOps.add(createMenuItem("reopenLastInput", "????????????", "????????????????????????"));
        menuOps.add(createMenuItem("stopAnalyzer", "????????????", "???????????????"));
        menuOps.add(createMenuItem("pauseRefreshing", "????????????", "????????????"));
        menuOps.add(createMenuItem("startRefreshing", "????????????", "????????????"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("openTerminal", "???????????????", "?????????????????????"));
        menuOps.add(createMenuItem("openCalc", "???????????????", "?????????????????????"));
        menuOps.add(createMenuItem("openNotepad", "???????????????", "?????????????????????"));
        menuOps.addSeparator();
        menuOps.add(createMenuItem("drawNetworkGraph", "?????????????????????", "?????????????????????"));

        JMenu menuViews = new JMenu("??????(V)");
        menuViews.setMnemonic(KeyEvent.VK_V);
        menuViews.add(createMenuItem("showStreamInfo", "????????????", "??????????????????"));
        menuViews.add(createMenuItem("showNetworkInfo", "????????????", "??????????????????"));
        menuViews.add(createMenuItem("showTR290Info", "????????????", "??????????????????"));
        menuViews.add(createMenuItem("showPCRInfo", "PCR??????", "??????PCR??????"));
        menuViews.add(createMenuItem("showEPGInfo", "EPG??????", "??????EPG??????"));
        menuViews.add(createMenuItem("showNVODInfo", "NVOD??????", "??????NVOD??????"));
        menuViews.add(createMenuItem("showPSISIInfo", "PSI/SI??????", "??????PSI/SI??????"));
        menuViews.add(createMenuItem("showEBInfo", "??????????????????", "????????????????????????"));

        JMenu menuLogs = new JMenu("??????(L)");
        menuLogs.setMnemonic(KeyEvent.VK_L);
        menuLogs.add(createMenuItem("clearLogs", "????????????", "????????????"));
        menuLogs.add(createMenuItem("checkLogs", "??????????????????", "??????????????????"));

        JMenu menuHelp = new JMenu("??????(H)");
        menuHelp.setMnemonic(KeyEvent.VK_H);
        menuHelp.add(createMenuItem("showHelp", "??????", "??????"));
        menuHelp.add(createMenuItem("showAbout", "??????(A)", "?????? " + AssistantApp.APP_NAME, KeyEvent.VK_A));

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

        JButton btnOpenFile = createButton("openFile", "??????????????????");
        btnOpenFile.setIcon(resourceMap.getIcon("toolbar.openFile.icon"));
        btnOpenFile.setText(null);
        JButton btnOpenMulticast = createButton("openMulticast", "???????????????");
        btnOpenMulticast.setIcon(resourceMap.getIcon("toolbar.openMulticast.icon"));
        btnOpenMulticast.setText(null);
        JButton btnReopenLastInput = createButton("reopenLastInput", "????????????");
        btnReopenLastInput.setIcon(resourceMap.getIcon("toolbar.reopenLastInput.icon"));
        btnReopenLastInput.setText(null);
        JButton btnStopAnalysing = createButton("stopAnalyzer", "????????????");
        btnStopAnalysing.setIcon(resourceMap.getIcon("toolbar.stopAnalyzer.icon"));
        btnStopAnalysing.setText(null);
        JButton btnPauseRefreshing = createButton("pauseRefreshing", "????????????");
        btnPauseRefreshing.setIcon(resourceMap.getIcon("toolbar.pauseRefreshing.icon"));
        btnPauseRefreshing.setText(null);
        JButton btnStartRefreshing = createButton("startRefreshing", "????????????");
        btnStartRefreshing.setIcon(resourceMap.getIcon("toolbar.startRefreshing.icon"));
        btnStartRefreshing.setText(null);
        JButton btnOpenTerminal = createButton("openTerminal", "???????????????");
        btnOpenTerminal.setIcon(resourceMap.getIcon("toolbar.openTerminal.icon"));
        btnOpenTerminal.setText(null);
        JButton btnOpenCalc = createButton("openCalc", "???????????????");
        btnOpenCalc.setIcon(resourceMap.getIcon("toolbar.openCalc.icon"));
        btnOpenCalc.setText(null);
        JButton btnOpenNotepad = createButton("openNotepad", "???????????????");
        btnOpenNotepad.setIcon(resourceMap.getIcon("toolbar.openNotepad.icon"));
        btnOpenNotepad.setText(null);
        JButton btnDrawNetworkGraph = createButton("drawNetworkGraph", "?????????????????????");
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
        sourceListView = new SourceListView();
        streamInfoView = new StreamInfoView(frameView);
        networkInfoView = new NetworkInfoView(frameView);
        tr290InfoView = new TR290InfoView(frameView);
        pcrStatsView = new PCRInfoView(frameView);
        epgInfoView = new EPGInfoView(frameView);
        nvodInfoView = new NVODInfoView(frameView);
        datagramView = new DatagramView(frameView);
        ebInfoView = new EBInfoView(frameView);
        logsView = new LogsView();

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.add("????????????", streamInfoView);
        tabbedPane.add("????????????", networkInfoView);
        tabbedPane.add("TR 101 290", tr290InfoView);
        tabbedPane.add("PCR", pcrStatsView);
        tabbedPane.add("EPG", epgInfoView);
        tabbedPane.add("NVOD", nvodInfoView);
        tabbedPane.add("PSI/SI", datagramView);
        tabbedPane.add("????????????", ebInfoView);
        tabbedPane.add("??????", logsView);

        frameView.getRootPane().getContentPane().add(sourceListView, BorderLayout.WEST);
        frameView.getRootPane().getContentPane().add(tabbedPane, BorderLayout.CENTER);
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

        Global.registerSubscriber(this);
    }

    @Subscribe
    public void onSourceChanged(SourceChangedEvent event)
    {
        Component c = tabbedPane.getSelectedComponent();
        if (c instanceof InfoView)
        {
            ((InfoView)c).refresh();
        }
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
    public void showNVODInfo()
    {
        tabbedPane.setSelectedComponent(nvodInfoView);
    }

    @Action
    public void showPSISIInfo()
    {
        tabbedPane.setSelectedComponent(datagramView);
    }

    @Action
    public void showEBInfo()
    {
        tabbedPane.setSelectedComponent(ebInfoView);
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
            logger.warn("???????????????????????????{}", ex.getMessage());
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
                                      String.format("%s %s%n??2022 %s", AssistantApp.APP_NAME, AssistantApp.APP_VERSION, AssistantApp.APP_VENDOR),
                                      String.format("?????? %s", AssistantApp.APP_NAME),
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
            String input = file.getAbsolutePath();

            Global.setInputResource(input);
            boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
            if (!started)
            {
                Global.setInputResource(null);
                actionMap.get("reopenLastInput").setEnabled(false);
                JOptionPane.showMessageDialog(frameView.getFrame(), "?????????????????????", "?????????", JOptionPane.WARNING_MESSAGE);
            } else
            {
                saveRecentFile(file);
                streamInfoView.reset();
                networkInfoView.reset();
                tr290InfoView.reset();
                pcrStatsView.reset();
                epgInfoView.reset();
                nvodInfoView.reset();
                datagramView.reset();
                actionMap.get("openFile").setEnabled(false);
                actionMap.get("openMulticast").setEnabled(false);
                actionMap.get("openThirdPartyInputSource").setEnabled(false);
                actionMap.get("reopenLastInput").setEnabled(false);
                actionMap.get("stopAnalyzer").setEnabled(true);
                actionMap.get("pauseRefreshing").setEnabled(true);
                actionMap.get("startRefreshing").setEnabled(false);

                long transactionId = Global.getCurrentTransactionId();
                SourceEntity source = Global.getDatabaseService().getSource(transactionId);
                SourceAttachedEvent event = new SourceAttachedEvent();
                event.setSource(source);
                Global.postEvent(event);
            }
        }
    }

    @Action
    public void openMulticast()
    {
        String input = JOptionPane.showInputDialog(frameView.getFrame(),
                                                   "????????????",
                                                   "udp://224.1.1.1:7890");
        if (input == null)
            return;

        if (!isCorrectMulticastAddress(input))
        {
            JOptionPane.showMessageDialog(frameView.getFrame(),
                                          "?????????????????????" + System.lineSeparator() + input,
                                          "??????",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        Global.setInputResource(input);
        boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
        if (!started)
        {
            Global.setInputResource(null);
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "?????????????????????", "?????????", JOptionPane.WARNING_MESSAGE);
        } else
        {
            Global.setInputResource(input);
            streamInfoView.reset();
            networkInfoView.reset();
            tr290InfoView.reset();
            pcrStatsView.reset();
            epgInfoView.reset();
            nvodInfoView.reset();
            datagramView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenLastInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);

            long transactionId = Global.getCurrentTransactionId();
            SourceEntity source = Global.getDatabaseService().getSource(transactionId);
            SourceAttachedEvent event = new SourceAttachedEvent();
            event.setSource(source);
            Global.postEvent(event);
        }
    }

    @Action
    public void openThirdPartyInputSource()
    {
        String input = JOptionPane.showInputDialog(frameView.getFrame(),
                                                   "???????????????",
                                                   null);
        if (input == null)
            return;

        Global.setInputResource(input);
        boolean started = Global.getStreamAnalyser().start(input, this::onAnalyzerStopped);
        if (!started)
        {
            Global.setInputResource(null);
            actionMap.get("reopenLastInput").setEnabled(false);
            JOptionPane.showMessageDialog(frameView.getFrame(), "?????????????????????", "?????????", JOptionPane.WARNING_MESSAGE);
        } else
        {
            Global.setInputResource(input);
            streamInfoView.reset();
            networkInfoView.reset();
            tr290InfoView.reset();
            pcrStatsView.reset();
            epgInfoView.reset();
            nvodInfoView.reset();
            datagramView.reset();
            actionMap.get("openFile").setEnabled(false);
            actionMap.get("openMulticast").setEnabled(false);
            actionMap.get("openThirdPartyInputSource").setEnabled(false);
            actionMap.get("reopenLastInput").setEnabled(false);
            actionMap.get("stopAnalyzer").setEnabled(true);
            actionMap.get("pauseRefreshing").setEnabled(true);
            actionMap.get("startRefreshing").setEnabled(false);

            long transactionId = Global.getCurrentTransactionId();
            SourceEntity source = Global.getDatabaseService().getSource(transactionId);
            SourceAttachedEvent event = new SourceAttachedEvent();
            event.setSource(source);
            Global.postEvent(event);
        }
    }

    @Action
    public void reopenLastInput()
    {
        String lastInput = Global.getInputResource();
        if (lastInput == null)
        {
            actionMap.get("reopenLastInput").setEnabled(false);
            return;
        }

        boolean started = Global.getStreamAnalyser().start(lastInput, this::onAnalyzerStopped);
        if (!started)
        {
            Global.setInputResource(null);
            JOptionPane.showMessageDialog(frameView.getFrame(), "?????????????????????", "?????????", JOptionPane.WARNING_MESSAGE);
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

            long transactionId = Global.getCurrentTransactionId();
            SourceEntity source = Global.getDatabaseService().getSource(transactionId);
            SourceAttachedEvent event = new SourceAttachedEvent();
            event.setSource(source);
            Global.postEvent(event);
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
        tr290InfoView.stopRefreshing();
        pcrStatsView.stopRefreshing();
        epgInfoView.stopRefreshing();
        nvodInfoView.stopRefreshing();
        datagramView.stopRefreshing();
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(true);
    }

    @Action
    public void startRefreshing()
    {
        streamInfoView.startRefreshing();
        networkInfoView.startRefreshing();
        tr290InfoView.startRefreshing();
        pcrStatsView.startRefreshing();
        epgInfoView.startRefreshing();
        nvodInfoView.startRefreshing();
        datagramView.startRefreshing();
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
            logger.warn("??????????????????????????????{}", ex.getMessage());
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
            logger.warn("??????????????????????????????{}", ex.getMessage());
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
            logger.warn("??????????????????????????????{}", ex.getMessage());
        }
    }

    @Action
    public void drawNetworkGraph()
    {
        DrawNetworkGraphTask task = new DrawNetworkGraphTask(frameView.getApplication());
        ComponentUtil.setWaitingMouseCursor(frameView.getRootPane(), true);
        task.execute();
    }

    private void onAnalyzerStopped(DemuxStatus status)
    {
        if (status.isRunning())
            return;

        if (!willQuit)
            JOptionPane.showMessageDialog(frameView.getFrame(), "??????????????????");

        actionMap.get("openFile").setEnabled(true);
        actionMap.get("openMulticast").setEnabled(true);
        actionMap.get("openThirdPartyInputSource").setEnabled(true);
        actionMap.get("reopenLastInput").setEnabled(true);
        actionMap.get("stopAnalyzer").setEnabled(false);
        actionMap.get("pauseRefreshing").setEnabled(false);
        actionMap.get("startRefreshing").setEnabled(false);

        streamInfoView.stopRefreshing();
        networkInfoView.stopRefreshing();
        epgInfoView.stopRefreshing();
        tr290InfoView.stopRefreshing();
        pcrStatsView.stopRefreshing();
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
            logger.debug("??????????????????????????????????????????{}", ex.getMessage());
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
            logger.debug("????????????????????????????????????{}", ex.getMessage());
        }
    }
}
