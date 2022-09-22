package m2tk.assistant.ui.component;

import m2tk.assistant.Global;
import m2tk.assistant.dbi.entity.SourceEntity;
import m2tk.assistant.ui.event.SourceChangedEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class SourceListPanel extends JPanel
{
    private DefaultListModel<SourceEntity> model;
    private JList<SourceEntity> list;

    public SourceListPanel()
    {
        initUI();
    }

    private void initUI()
    {
        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SourceListCellRenderer());
        list.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int clicks = e.getClickCount();
                int selectedIndex = list.getSelectedIndex();
                if (clicks == 2 && selectedIndex >= 0)
                {
                    fireSourceSelectedEvent(model.get(selectedIndex));
                }
            }
        });

        setLayout(new BorderLayout());
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void addSource(SourceEntity source)
    {
        model.addElement(Objects.requireNonNull(source));
        list.setSelectedValue(source, true);
        fireSourceSelectedEvent(source);
    }

    private void fireSourceSelectedEvent(SourceEntity source)
    {
        SourceChangedEvent event = new SourceChangedEvent();
        event.setSourceName(source.getSourceName());
        event.setTransactionId(source.getTransactionId());
        Global.postEvent(event);
    }

    static class SourceListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            SourceEntity source = (SourceEntity) value;
            return super.getListCellRendererComponent(list,
                                                      (source == null) ? null : source.getSourceName(),
                                                      index,
                                                      isSelected,
                                                      cellHasFocus);
        }
    }
}
