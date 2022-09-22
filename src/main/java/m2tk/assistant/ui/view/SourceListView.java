package m2tk.assistant.ui.view;

import com.google.common.eventbus.Subscribe;
import m2tk.assistant.Global;
import m2tk.assistant.ui.component.SourceListPanel;
import m2tk.assistant.ui.event.SourceAttachedEvent;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class SourceListView extends JPanel
{
    private SourceListPanel sourceList;

    public SourceListView()
    {
        initUI();
    }

    private void initUI()
    {
        sourceList = new SourceListPanel();
        ComponentUtil.setTitledBorder(sourceList, "输入源", TitledBorder.LEFT);

        setLayout(new MigLayout("fill", "[300!]"));
        add(sourceList, "center, grow");

        Global.registerSubscriber(this);
    }

    @Subscribe
    public void onSourceAttached(SourceAttachedEvent event)
    {
        sourceList.addSource(event.getSource());
    }
}
