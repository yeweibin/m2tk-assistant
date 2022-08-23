package m2tk.assistant.ui.component;

import javax.swing.*;
import java.awt.*;

public class ImageCanvas extends JPanel
{
    private Image image;

    public void setImage(Image image)
    {
        if (image != null)
        {
            this.image = image;

            int w = image.getWidth(this);
            int h = image.getHeight(this);
            setPreferredSize(new Dimension(w, h));
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (image != null)
            g.drawImage(image, 0, 0, image.getWidth(this), image.getHeight(this), this);
    }
}
