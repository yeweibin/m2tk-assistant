package m2tk.assistant.ui.dialog;

import hu.kazocsaba.imageviewer.ImageViewer;
import hu.kazocsaba.imageviewer.ResizeStrategy;
import m2tk.assistant.ui.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NetworkGraphDialog extends JFrame
{
    private transient ImageViewer viewer;

    public NetworkGraphDialog()
    {
        initUI();
    }

    private void initUI()
    {
        viewer = new ImageViewer();
        viewer.setResizeStrategy(ResizeStrategy.NO_RESIZE);
        getContentPane().add(viewer.getComponent(), BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("网络结构");
    }

    public void showImage(BufferedImage image)
    {
        viewer.setImage(image);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.75, 0.75);
        setVisible(true);
    }
}
