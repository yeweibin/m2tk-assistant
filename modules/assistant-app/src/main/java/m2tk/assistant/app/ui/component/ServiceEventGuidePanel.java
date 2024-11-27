/*
 * Copyright (c) M2TK Project. All rights reserved.
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
package m2tk.assistant.app.ui.component;

import cn.hutool.core.collection.CollUtil;
import m2tk.assistant.api.domain.SIEvent;
import m2tk.assistant.api.domain.SIService;
import m2tk.assistant.app.ui.model.EventTableModel;
import m2tk.assistant.app.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class ServiceEventGuidePanel extends JPanel
{
    private JTree serviceTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode treeRoot;
    private EventTableModel eventTableModel;

    private transient List<SIService> serviceList = Collections.emptyList();
    private transient Map<SIService, List<SIEvent>> eventRegistry = Collections.emptyMap();

    public ServiceEventGuidePanel()
    {
        initUI();
    }

    private void initUI()
    {
        treeRoot = new DefaultMutableTreeNode("/");
        treeModel = new DefaultTreeModel(treeRoot);
        serviceTree = new JTree(treeModel);
        serviceTree.setRootVisible(false);
        serviceTree.setShowsRootHandles(true);
        serviceTree.setCellRenderer(new ServiceTreeCellRenderer());
        serviceTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) serviceTree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof SIService service)
            {
                Map<SIService, List<SIEvent>> registry = eventRegistry;
                if (registry != null)
                {
                    List<SIEvent> events = registry.getOrDefault(service, Collections.emptyList());
                    eventTableModel.update(events);
                }
            }
        });

        eventTableModel = new EventTableModel();
        JTable eventTable = new JTable();
        eventTable.setModel(eventTableModel);
        eventTable.getTableHeader().setReorderingAllowed(true);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer leadingRenderer = new DefaultTableCellRenderer();
        leadingRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        DefaultTableCellRenderer trailingRenderer = new DefaultTableCellRenderer();
        trailingRenderer.setHorizontalAlignment(SwingConstants.TRAILING);

        TableColumnModel columnModel = eventTable.getColumnModel();
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 70, false); // 类型
        ComponentUtil.configTableColumn(columnModel, 1, trailingRenderer, 70, false);  // 事件号
        ComponentUtil.configTableColumn(columnModel, 2, centeredRenderer, 160, false);  // 开始时间
        ComponentUtil.configTableColumn(columnModel, 3, centeredRenderer, 120, false);  // 持续时间
        ComponentUtil.configTableColumn(columnModel, 4, centeredRenderer, 60, false); // 语言
        ComponentUtil.configTableColumn(columnModel, 5, leadingRenderer, 160, true); // 标题
        ComponentUtil.configTableColumn(columnModel, 6, leadingRenderer, 160, true); // 描述

        setLayout(new MigLayout("insets 2", "[360!][grow]", "grow"));
        add(new JScrollPane(serviceTree), "grow");
        add(new JScrollPane(eventTable), "grow");
    }

    public void update(List<SIService> services, Map<SIService, List<SIEvent>> events)
    {
        if (!isSameServices(serviceList, services) || !isSameEventGroups(eventRegistry, events))
        {
            treeRoot.removeAllChildren();

            // 这里Map是排好序的
            Map<String, List<SIService>> groups1 = groupServices(services);
            for (Map.Entry<String, List<SIService>> entry : groups1.entrySet())
            {
                treeRoot.add(createServiceGroupNode(entry.getKey(), entry.getValue()));
            }

            Map<String, List<SIService>> groups2 = groupServices(events.keySet());
            for (Map.Entry<String, List<SIService>> entry : groups2.entrySet())
            {
                // 其他流的节目描述，且该业务无对应的SDT描述。
                if (!groups1.containsKey(entry.getKey()))
                {
                    treeRoot.add(createServiceGroupNode(entry.getKey(), entry.getValue()));
                }
            }

            serviceTree.expandPath(new TreePath(treeRoot));
            treeModel.reload();
            eventTableModel.update(Collections.emptyList());

            eventRegistry = events;
            serviceList = services;
        }
    }

    private boolean isSameServices(List<SIService> current, List<SIService> incoming)
    {
        return CollUtil.isEqualList(current, incoming);
    }

    private boolean isSameEventGroups(Map<SIService, List<SIEvent>> current, Map<SIService, List<SIEvent>> incoming)
    {
        if (!CollUtil.isEqualList(current.keySet(), incoming.keySet()))
            return false;

        for (SIService key : incoming.keySet())
        {
            if (!CollUtil.isEqualList(current.get(key), incoming.get(key)))
                return false;
        }
        return true;
    }

    private Map<String, List<SIService>> groupServices(Collection<SIService> services)
    {
        return services.stream()
                       .collect(groupingBy(service -> String.format("TS ID: %d（Net ID: %d）",
                                                                    service.getTransportStreamId(),
                                                                    service.getOriginalNetworkId()),
                                           TreeMap::new, toList()));
    }

    private DefaultMutableTreeNode createServiceGroupNode(String groupName, List<SIService> services)
    {
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode();
        groupNode.setUserObject(groupName);

        for (SIService service : services)
        {
            DefaultMutableTreeNode srvNode = new DefaultMutableTreeNode();
            srvNode.setUserObject(service);
            groupNode.add(srvNode);
        }

        return groupNode;
    }

    class ServiceTreeCellRenderer extends DefaultTreeCellRenderer
    {
        final Color LIGHT_BLUE = Color.decode("#89D3DF");
        final Icon TRANSMIT = FontIcon.of(FluentUiFilledMZ.SOUND_SOURCE_24, 20, LIGHT_BLUE);
        final Icon TELEVISION = FontIcon.of(FluentUiFilledMZ.MOVIES_AND_TV_24, 20, LIGHT_BLUE);

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value != treeRoot)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof String name)
                {
                    setText(name);
                    setIcon(TRANSMIT);
                }
                if (userObject instanceof SIService service)
                {
                    setText(service.getName());
                    setIcon(TELEVISION);
                }
            }

            return this;
        }
    }
}
