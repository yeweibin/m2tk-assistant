package m2tk.assistant.ui.component;

import m2tk.assistant.dbi.entity.SourceEntity;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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

        setLayout(new BorderLayout());
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public JList<SourceEntity> getSourceList()
    {
        return list;
    }

    public void addSources(List<SourceEntity> sources)
    {
        model.addAll(sources);
    }

    public void addSource(SourceEntity source)
    {
        model.addElement(source);
    }

    public void removeSource(SourceEntity source)
    {
        model.removeElement(source);
    }

    public SourceEntity getFirstSelectableSource()
    {
        return (model.size() == 0) ? null : model.get(0);
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
