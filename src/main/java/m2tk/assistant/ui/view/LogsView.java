package m2tk.assistant.ui.view;

import m2tk.assistant.ui.util.ComponentUtil;
import m2tk.assistant.ui.util.ListModelOutputStream;
import m2tk.assistant.util.TextListLogAppender;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LogsView extends JPanel
{
    private DefaultListModel<String> model;

    public LogsView()
    {
        initUI();
    }

    private void initUI()
    {
        model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setDragEnabled(false);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        ComponentUtil.setTitledBorder(panel, "日志", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(panel, "center, grow");

        TextListLogAppender.setStaticOutputStream(new ListModelOutputStream(model));
    }

    public void clear()
    {
        model.clear();
    }
}
