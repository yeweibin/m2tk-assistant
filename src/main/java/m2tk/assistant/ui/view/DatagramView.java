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
import m2tk.assistant.ui.component.SectionDatagramPanel;
import m2tk.assistant.ui.event.SourceChangedEvent;
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

public class DatagramView extends JPanel implements InfoView
{
    private final transient FrameView frameView;
    private SectionDatagramPanel sectionDatagramPanel;
    private Timer timer;
    private volatile long transactionId;

    public DatagramView(FrameView view)
    {
        frameView = view;
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(10000, e -> {
            if (!isVisible())
                return; // 不在后台刷新

            if (!Global.getStreamAnalyser().isRunning())
                timer.stop();

            queryDatagrams();
        });

        sectionDatagramPanel = new SectionDatagramPanel();
        ComponentUtil.setTitledBorder(sectionDatagramPanel, "PSI/SI", TitledBorder.LEFT);

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

    @Override
    public void refresh()
    {
        queryDatagrams();
    }

    @Subscribe
    public void onSourceChanged(SourceChangedEvent event)
    {
        transactionId = event.getTransactionId();
    }

    public void reset()
    {
        sectionDatagramPanel.reset();
        if (Global.getStreamAnalyser().isRunning())
            timer.restart();
    }

    public void startRefreshing()
    {
        if (Global.getStreamAnalyser().isRunning())
            timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    private void queryDatagrams()
    {
        long currentTransactionId = (transactionId == -1) ? Global.getCurrentTransactionId() : transactionId;

        Supplier<Map<String, List<SectionEntity>>> query = () ->
                Global.getDatabaseService().getSectionGroups(currentTransactionId);

        Consumer<Map<String, List<SectionEntity>>> consumer = sectionDatagramPanel::update;

        AsyncQueryTask<Map<String, List<SectionEntity>>> task =
                new AsyncQueryTask<>(frameView.getApplication(), query, consumer);
        task.execute();
    }
}
