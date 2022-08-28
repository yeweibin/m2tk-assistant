package m2tk.assistant;

import cn.hutool.core.thread.ThreadUtil;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import m2tk.assistant.ui.MainViewController;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public final class AssistantApp extends SingleFrameApplication
{
    static void enableAntiAliasing()
    {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static AssistantApp getInstance()
    {
        return SingleFrameApplication.getInstance(AssistantApp.class);
    }

    public static void main(String[] args)
    {
//        enableAntiAliasing();
        Global.init();
        SingleFrameApplication.launch(AssistantApp.class, args);
    }

    public static final String APP_NAME = "M2TK码流分析助手";
    public static final String APP_VERSION = "1.5.8.1900";
    public static final String APP_VENDOR = "M2TK项目组";
    private MainViewController controller;
    private MPEGTSPlayer player;
    private static final Logger logger = LoggerFactory.getLogger(AssistantApp.class);

    @Override
    protected void initialize(String[] args)
    {
        Font treeFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Tree.font").getSize());
        Font tableFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Table.font").getSize());
        FlatIntelliJLaf.setup();
//        FlatDarculaLaf.setup();
        UIManager.put("TitlePane.showIconBesideTitle", true);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Table.paintOutsideAlternateRows", true);
        UIManager.put("Tree.font", treeFont);
        UIManager.put("Table.font", tableFont);
        UIManager.put("Table.cellMargins", new Insets(4, 6, 4, 6));
        UIManager.put("Table.alternateRowColor", FlatLaf.isLafDark() ? new Color(0x626262) : new Color(0xEEEEEE));
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
                player.playVideoAndAudio(in, videoPid, audioPid);
            } catch (Exception ex)
            {
                logger.warn("{}", ex.getMessage());
                EventQueue.invokeLater(onError);
            }
        });
    }
}
