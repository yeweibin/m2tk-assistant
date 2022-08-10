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

package m2tk.assistant.ui.component;

import m2tk.assistant.SmallIcons;
import m2tk.assistant.analyzer.domain.SIEvent;
import m2tk.assistant.analyzer.domain.SIService;
import m2tk.assistant.ui.model.EventTableModel;
import m2tk.assistant.ui.util.ComponentUtil;
import net.miginfocom.swing.MigLayout;

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
    private final List<SIService> serviceList = new ArrayList<>();
    private final List<SIEvent> eventList = new ArrayList<>();
    private Map<String, List<SIEvent>> eventRegistry;

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
            if (node != null && node.getUserObject() instanceof SIService)
            {
                SIService service = (SIService) node.getUserObject();
                Map<String, List<SIEvent>> registry = eventRegistry;
                if (registry != null)
                {
                    List<SIEvent> events = registry.getOrDefault(service.getId(), Collections.emptyList());
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
        ComponentUtil.configTableColumn(columnModel, 0, centeredRenderer, 40, false); // 类型
        ComponentUtil.configTableColumn(columnModel, 1, trailingRenderer, 80, false);  // 事件号
        ComponentUtil.configTableColumn(columnModel, 2, trailingRenderer, 140, false);  // 开始时间
        ComponentUtil.configTableColumn(columnModel, 3, trailingRenderer, 80, false);  // 持续时间
        ComponentUtil.configTableColumn(columnModel, 4, leadingRenderer, 160, true); // 标题
        ComponentUtil.configTableColumn(columnModel, 5, leadingRenderer, 160, true); // 描述
        ComponentUtil.configTableColumn(columnModel, 6, centeredRenderer, 40, false); // 语言

        setLayout(new MigLayout("insets 2", "[360!][grow]", "grow"));
        add(new JScrollPane(serviceTree), "grow");
        add(new JScrollPane(eventTable), "grow");
    }

    public void reset()
    {
        treeRoot.removeAllChildren();
        treeModel.reload();
        eventTableModel.update(Collections.emptyList());
        serviceList.clear();
        eventList.clear();
        if (eventRegistry != null)
        {
            eventRegistry.clear();
            eventRegistry = null;
        }
    }

    public void updateServiceList(List<SIService> services)
    {
        if (services == null || isSameServices(serviceList, services))
            return;

        treeRoot.removeAllChildren();

        // 这里Map是排好序的
        Map<String, List<SIService>> groups = groupServices(services);
        for (Map.Entry<String, List<SIService>> entry : groups.entrySet())
        {
            treeRoot.add(createServiceGroupNode(entry.getKey(), entry.getValue()));
        }

        serviceTree.expandPath(new TreePath(treeRoot));
        treeModel.reload();

        serviceList.clear();
        serviceList.addAll(services);
    }

    public void updateEventRegistry(List<SIEvent> events)
    {
        if (events == null || isSameEvents(eventList, events))
            return;

        Map<String, List<SIEvent>> currRegistry = eventRegistry;
        if (currRegistry != null)
            currRegistry.clear();

        eventRegistry = events.stream().collect(groupingBy(SIEvent::getParentId));
        eventList.clear();
        eventList.addAll(events);
    }

    private boolean isSameServices(List<SIService> current, List<SIService> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparing(SIService::getId));

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            SIService s1 = current.get(i);
            SIService s2 = incoming.get(i);

            if (!Objects.equals(s1, s2))
                return false;
        }

        return true;
    }

    private boolean isSameEvents(List<SIEvent> current, List<SIEvent> incoming)
    {
        if (current.size() != incoming.size())
            return false;

        incoming.sort(Comparator.comparing(SIEvent::getId));

        int n = current.size();
        for (int i = 0; i < n; i++)
        {
            SIEvent e1 = current.get(i);
            SIEvent e2 = incoming.get(i);

            if (!Objects.equals(e1, e2))
                return false;
        }

        return true;
    }

    private Map<String, List<SIService>> groupServices(List<SIService> services)
    {
        return services.stream()
                       .collect(groupingBy(service -> String.format("TS ID: %05d（Net ID: %d）",
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
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value != treeRoot)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof String)
                {
                    setText((String) userObject);
                    setIcon(SmallIcons.TRANSMIT_BLUE);
                }
                if (userObject instanceof SIService)
                {
                    SIService service = (SIService) userObject;
                    setText(service.getServiceName());
                    setIcon(SmallIcons.TELEVISION);
                }
            }

            return this;
        }
    }
}
