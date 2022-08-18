package m2tk.assistant;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import m2tk.assistant.ui.MainViewController;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

import javax.swing.*;
import java.awt.*;

public final class AssistantApp extends SingleFrameApplication
{
    static void enableAntiAliasing()
    {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static void main(String[] args)
    {
//        enableAntiAliasing();
        Global.init();
        SingleFrameApplication.launch(AssistantApp.class, args);
    }

    public static final String APP_NAME = "M2TK码流分析助手";
    public static final String APP_VERSION = "1.5.1700";
    public static final String APP_VENDOR = "M2TK项目组";
    private MainViewController controller;

    @Override
    protected void initialize(String[] args)
    {
        Font treeFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Tree.font").getSize());
        Font tableFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Table.font").getSize());
//        FlatIntelliJLaf.setup();
        FlatDarculaLaf.setup();
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
        show(frameView);
    }

    @Override
    protected void shutdown()
    {
        controller.setWillQuit();
        Global.getStreamAnalyser().shutdown();
        super.shutdown();
    }
}
