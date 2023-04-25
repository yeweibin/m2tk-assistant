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

import com.google.common.eventbus.Subscribe;
import m2tk.assistant.Global;
import m2tk.assistant.dbi.entity.SectionEntity;
import m2tk.assistant.ui.component.EBSectionDatagramPanel;
import m2tk.assistant.ui.event.SourceAttachedEvent;
import m2tk.assistant.ui.event.SourceDetachedEvent;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EBInfoView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private EBSectionDatagramPanel sectionDatagramPanel;
    private Timer timer;
    private volatile long transactionId;

    public EBInfoView(FrameView view)
    {
        frameView = view;
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(10000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (transactionId == -1)
                timer.stop();

            queryDatagrams();
        });

        sectionDatagramPanel = new EBSectionDatagramPanel();
        ComponentUtil.setTitledBorder(sectionDatagramPanel, "应急广播", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(sectionDatagramPanel, "center, grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                refresh();
            }
        });

        Global.registerSubscriber(this);
        transactionId = -1;
    }

    @Subscribe
    public void onSourceAttachedEvent(SourceAttachedEvent event)
    {
        transactionId = event.getSource().getTransactionId();
    }

    @Subscribe
    public void onSourceDetachedEvent(SourceDetachedEvent event)
    {
        transactionId = -1;
    }

    @Override
    public void refresh()
    {
        queryDatagrams();
    }

    public void reset()
    {
        sectionDatagramPanel.reset();
        if (transactionId != -1)
            timer.restart();
    }

    public void startRefreshing()
    {
        if (transactionId != -1)
            timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    private void queryDatagrams()
    {
        long currentTransaction = transactionId;
        if (currentTransaction == -1)
            return;

        Supplier<Map<String, List<SectionEntity>>> query = () ->
                Global.getDatabaseService()
                      .getSectionGroups(currentTransaction, "eb-section.")
                      .stream()
                      .collect(Collectors.groupingBy(section -> section.getTag().replace("eb-section.", "")));

        Consumer<Map<String, List<SectionEntity>>> consumer = sectionDatagramPanel::update;

        AsyncQueryTask<Map<String, List<SectionEntity>>> task =
                new AsyncQueryTask<>(frameView.getApplication(), query, consumer);
        task.execute();
    }
}
