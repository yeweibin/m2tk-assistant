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
import m2tk.assistant.analyzer.domain.TR290Event;
import m2tk.assistant.analyzer.domain.TR290Stats;
import m2tk.assistant.analyzer.presets.TR290ErrorTypes;
import m2tk.assistant.dbi.entity.TR290StatEntity;
import m2tk.assistant.ui.component.TR290StatsPanel;
import m2tk.assistant.ui.task.AsyncQueryTask;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

public class TR290InfoView extends JPanel
{
    private final FrameView frameView;
    private final ActionMap actionMap;
    private TR290StatsPanel tr290StatsPanel;
    private Timer timer;

    public TR290InfoView(FrameView view)
    {
        frameView = view;
        actionMap = view.getContext().getActionMap(this);
        initUI();
    }

    private void initUI()
    {
        timer = new Timer(1000, actionMap.get("queryTR290Events"));

        tr290StatsPanel = new TR290StatsPanel();
        ComponentUtil.setTitledBorder(tr290StatsPanel, "TR 101 290", TitledBorder.LEFT);

        setLayout(new MigLayout("fill"));
        add(tr290StatsPanel, "center, grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                startRefreshing();
            }

            @Override
            public void componentHidden(ComponentEvent e)
            {
                stopRefreshing();
            }
        });
    }

    public void reset()
    {
        tr290StatsPanel.reset();
        timer.restart();
    }

    public void startRefreshing()
    {
        timer.start();
    }

    public void stopRefreshing()
    {
        timer.stop();
    }

    @Action
    public void queryTR290Events()
    {
        Supplier<TR290Stats> query = () -> {
            Map<String, TR290StatEntity> statsMap = Global.getDatabaseService()
                                                          .listTR290Stats()
                                                          .stream()
                                                          .collect(toMap(entity -> entity.getIndicator(),
                                                                         entity -> entity));

            TR290Stats stats = new TR290Stats();
            Consumer<TR290StatEntity> operator = stat -> {
                if (stat != null)
                {
                    stats.setStat(stat.getIndicator(),
                                  stat.getCount(),
                                  new TR290Event(stat.getLastEventTimestamp(),
                                                 stat.getIndicator(),
                                                 stat.getLastEventDescription()));
                }
            };

            operator.accept(statsMap.get(TR290ErrorTypes.TS_SYNC_LOSS));
            operator.accept(statsMap.get(TR290ErrorTypes.SYNC_BYTE_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PAT_ERROR_2));
            operator.accept(statsMap.get(TR290ErrorTypes.CONTINUITY_COUNT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PMT_ERROR_2));
            operator.accept(statsMap.get(TR290ErrorTypes.PID_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.TRANSPORT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.CRC_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_REPETITION_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_DISCONTINUITY_INDICATOR_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.PCR_ACCURACY_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.CAT_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.NIT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.NIT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.SI_REPETITION_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.UNREFERENCED_PID));
            operator.accept(statsMap.get(TR290ErrorTypes.SDT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.SDT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.EIT_ACTUAL_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.EIT_OTHER_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.RST_ERROR));
            operator.accept(statsMap.get(TR290ErrorTypes.TDT_ERROR));

            return stats;
        };

        Consumer<TR290Stats> consumer = tr290StatsPanel::update;

        AsyncQueryTask<TR290Stats> task = new AsyncQueryTask<>(frameView.getApplication(),
                                                               query,
                                                               consumer);
        task.execute();
    }
}
