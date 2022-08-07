package m2tk.assistant.ui.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class ComponentUtil
{
    private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private static final Cursor NORMAL_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor WAITING_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private static final MouseAdapter EMPTY_MOUSE_ADAPTER = new MouseAdapter()
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            e.consume();
        }
    };

    private ComponentUtil()
    {
    }

    /**
     * 设置组件preferSize并定位于屏幕中央
     */
    public static void setPreferSizeAndLocateToCenter(Component component,
                                                      int preferWidth,
                                                      int preferHeight)
    {
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(component.getGraphicsConfiguration());
        int w = SCREEN_SIZE.width - screenInsets.left - screenInsets.right;
        int h = SCREEN_SIZE.height - screenInsets.top - screenInsets.bottom;
        component.setPreferredSize(new Dimension(preferWidth, preferHeight));
        component.setBounds((w - preferWidth) / 2, (h - preferHeight) / 2, preferWidth, preferHeight);
    }

    /**
     * 设置组件preferSize并定位于屏幕中央(基于屏幕宽高的百分百)
     */
    public static void setPreferSizeAndLocateToCenter(Component component,
                                                      double preferWidthPercent,
                                                      double preferHeightPercent)
    {
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(component.getGraphicsConfiguration());
        int w = SCREEN_SIZE.width - screenInsets.left - screenInsets.right;
        int h = SCREEN_SIZE.height - screenInsets.top - screenInsets.bottom;
        int preferWidth = (int) (w * preferWidthPercent);
        int preferHeight = (int) (h * preferHeightPercent);
        component.setPreferredSize(new Dimension(preferWidth, preferHeight));
        component.setBounds((w - preferWidth) / 2, (h - preferHeight) / 2, preferWidth, preferHeight);
    }

    public static void setWaitingMouseCursor(JComponent component, boolean enabled)
    {
        Component glassPane = component.getRootPane().getGlassPane();
        if (enabled)
        {
            glassPane.setCursor(WAITING_CURSOR);
            glassPane.addMouseListener(EMPTY_MOUSE_ADAPTER);
            glassPane.setVisible(true);
        } else
        {
            glassPane.setCursor(NORMAL_CURSOR);
            glassPane.removeMouseListener(EMPTY_MOUSE_ADAPTER);
            glassPane.setVisible(false);
        }
    }

    public static JLabel createLabel(String text, int alignment)
    {
        JLabel label = new JLabel();
        label.setText(text);
        label.setHorizontalAlignment(alignment);
        return label;
    }

    public static JButton createButton(String text, ActionListener actionListener, boolean enabled)
    {
        JButton button = new JButton();
        button.setText(text);
        button.addActionListener(actionListener);
        button.setEnabled(enabled);
        return button;
    }

    public static JButton createButton(String text, String tooltip, ActionListener actionListener, boolean enabled)
    {
        JButton button = new JButton();
        button.setText(text);
        button.setToolTipText(tooltip);
        button.addActionListener(actionListener);
        button.setEnabled(enabled);
        return button;
    }

    public static void configTableColumn(TableColumnModel model, int columnIndex, int width, boolean resizable)
    {
        configTableColumn(model, columnIndex, null, width, resizable);
    }

    public static void configTableColumn(TableColumnModel model, int columnIndex, TableCellRenderer cellRenderer, int width)
    {
        configTableColumn(model, columnIndex, cellRenderer, width, false);
    }

    public static void configTableColumn(TableColumnModel model, int columnIndex, TableCellRenderer cellRenderer, int width, boolean resizable)
    {
        TableColumn column = model.getColumn(columnIndex);
        column.setPreferredWidth(width);
        column.setResizable(resizable);

        if (cellRenderer != null)
            column.setCellRenderer(cellRenderer);
    }
}
