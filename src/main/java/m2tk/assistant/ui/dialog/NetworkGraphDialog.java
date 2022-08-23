package m2tk.assistant.ui.dialog;

import hu.kazocsaba.imageviewer.ImageViewer;
import m2tk.assistant.ui.component.ImageCanvas;
import m2tk.assistant.ui.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NetworkGraphDialog extends JFrame
{
    private ImageViewer viewer;

    public NetworkGraphDialog()
    {
        initUI();
    }

    private void initUI()
    {
        viewer = new ImageViewer();
        getContentPane().add(viewer.getComponent(), BorderLayout.CENTER);
        setTitle("网络结构");
    }

    public void showImage(BufferedImage image)
    {
//        canvas.setImage(image);
        viewer.setImage(image);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.75, 0.75);
        setVisible(true);
    }
}
