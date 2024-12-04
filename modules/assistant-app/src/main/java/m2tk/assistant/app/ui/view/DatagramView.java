/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package m2tk.assistant.app.ui.view;

import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.InfoView;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.StreamObserver;
import m2tk.assistant.api.domain.ElementaryStream;
import m2tk.assistant.api.domain.FilteringHook;
import m2tk.assistant.api.domain.PrivateSection;
import m2tk.assistant.api.event.RefreshInfoViewEvent;
import m2tk.assistant.api.event.ShowInfoViewEvent;
import m2tk.assistant.api.presets.StreamTypes;
import m2tk.assistant.app.ui.component.SectionDatagramPanel;
import m2tk.assistant.app.ui.task.AsyncQueryTask;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Application;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;
import org.kordamp.ikonli.swing.FontIcon;
import org.pf4j.Extension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Extension(ordinal = 8)
public class DatagramView extends JPanel implements InfoView, StreamObserver
{
    private Application application;
    private SectionDatagramPanel sectionDatagramPanel;
    private EventBus bus;
    private M2TKDatabase database;

    private volatile long lastTimestamp;
    private final long MIN_QUERY_INTERVAL_MILLIS = 500;

    public DatagramView()
    {
        initUI();
    }

    private void initUI()
    {
        sectionDatagramPanel = new SectionDatagramPanel();
        ComponentUtil.setTitledBorder(sectionDatagramPanel, "数据段结构");

        setLayout(new MigLayout("fill"));
        add(sectionDatagramPanel, "center, grow");

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (database != null)
                    queryDatagrams();
            }
        });
    }

    @Override
    public void setupApplication(Application application)
    {
        this.application = application;
    }

    @Override
    public void setupDataSource(EventBus bus, M2TKDatabase database)
    {
        this.bus = bus;
        this.database = database;

        bus.register(this);
    }

    @Override
    public void setupMenu(JMenu menu)
    {
        JMenuItem item = new JMenuItem(getViewTitle());
        item.setIcon(getViewIcon());
        item.setAccelerator(KeyStroke.getKeyStroke("alt 8"));
        item.addActionListener(e -> {
            if (bus != null)
            {
                ShowInfoViewEvent event = new ShowInfoViewEvent(this);
                bus.post(event);
            }
        });
        menu.add(item);
    }

    @Override
    public JComponent getViewComponent()
    {
        return this;
    }

    @Override
    public String getViewTitle()
    {
        return "数据段结构";
    }

    @Override
    public Icon getViewIcon()
    {
        return FontIcon.of(FluentUiRegularMZ.TEXT_BULLET_LIST_TREE_20, 20, Color.decode("#89D3DF"));
    }

    @Override
    public List<JMenuItem> getContextMenuItem(ElementaryStream stream)
    {
        if (stream.isScrambled() ||
            StrUtil.contains(stream.getDescription(), "PES") ||
            StrUtil.equalsAny(stream.getCategory(), StreamTypes.CATEGORY_VIDEO, StreamTypes.CATEGORY_AUDIO))
            return List.of();

        JMenuItem item = new JMenuItem();
        item.setText("过滤私有段");
        item.addActionListener(e -> filterPrivateSection(stream));
        return List.of(item);
    }

    @Subscribe
    public void onRefreshInfoViewEvent(RefreshInfoViewEvent event)
    {
        long t1 = System.currentTimeMillis();
        if (t1 - lastTimestamp >= MIN_QUERY_INTERVAL_MILLIS && isShowing())
        {
            queryDatagrams();
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private void queryDatagrams()
    {
        Supplier<Map<String, List<PrivateSection>>> query = database::getPrivateSectionGroups;
        Consumer<Map<String, List<PrivateSection>>> consumer = sectionDatagramPanel::update;

        AsyncQueryTask<Map<String, List<PrivateSection>>> task = new AsyncQueryTask<>(application, query, consumer);
        task.execute();
    }

    private void filterPrivateSection(ElementaryStream stream)
    {
        int pid = stream.getStreamPid();
        if (pid == 0x1FFF)
        {
            String text = String.format("不支持的流类型：%s", stream.getDescription());
            JOptionPane.showMessageDialog(null, text);
            log.info(text);
            return;
        }

        log.info("添加私有段过滤器：'流{}'，类型：{}", stream.getStreamPid(), stream.getDescription());

        FilteringHook hook = new FilteringHook();
        hook.setSourceUri(database.getCurrentStreamSource().getUri());
        hook.setSubjectType("section");
        hook.setSubjectPid(stream.getStreamPid());
        hook.setSubjectTableId(-1);
        database.addFilteringHook(hook);
    }
}
