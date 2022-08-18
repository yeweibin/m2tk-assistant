/*
 * Copyright (c) Ye Weibin. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.ui.view;

import m2tk.assistant.Global;
import m2tk.assistant.SmallIcons;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.assistant.ui.component.SectionDatagramPanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DatagramView extends JPanel
{
    private final transient FrameView frameView;
    private final ActionMap actionMap;
    private SectionDatagramPanel sectionDatagramPanel;

    public DatagramView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        sectionDatagramPanel = new SectionDatagramPanel();
        JToolBar toolBar = new JToolBar();
        JButton btn = toolBar.add(actionMap.get("refresh"));
        btn.setIcon(SmallIcons.TABLE_REFRESH);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setText("刷新");
        btn.setToolTipText("刷新数据");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(sectionDatagramPanel, BorderLayout.CENTER);
        ComponentUtil.setTitledBorder(panel, "PSI/SI", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(panel, "center, grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                refresh();
            }
        });
    }

    @Action
    public void refresh()
    {
        Supplier<Map<String, List<SectionEntity>>> query = () ->
                Global.getDatabaseService().getSectionGroups();

        Consumer<Map<String, List<SectionEntity>>> consumer = sectionDatagramPanel::update;

        AsyncQueryTask<Map<String, List<SectionEntity>>> task =
                new AsyncQueryTask<>(frameView.getApplication(), query, consumer);
        task.execute();
    }
}
