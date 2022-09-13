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
import m2tk.assistant.analyzer.domain.NVODEvent;
import m2tk.assistant.analyzer.domain.NVODService;
import m2tk.assistant.ui.model.NVODEventTableModel;
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

public class NVODServiceEventGuidePanel extends JPanel
{
    private JTree serviceTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode treeRoot;
    private NVODEventTableModel eventTableModel;
    private transient Map<String, NVODService> serviceRegistry = Collections.emptyMap();
    private transient Map<String, NVODEvent> eventRegistry = Collections.emptyMap();

    public NVODServiceEventGuidePanel()
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
            if (node != null)
            {
                NVODService service = (NVODService) node.getUserObject();
                List<NVODEvent> events = new ArrayList<>();

                if (service.isTimeShiftedService())
                {
                    Map<String, NVODEvent> registry = eventRegistry;
                    if (registry != null)
                    {
                        registry.values().forEach(event -> {
                            if (event.getParentId().equals(service.getId()))
                                events.add(event);
                        });
                        events.sort(Comparator.comparing(NVODEvent::getStartTime));
                    }
                }
                eventTableModel.update(events);
            }
        });

        eventTableModel = new NVODEventTableModel();
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
        ComponentUtil.configTableColumn(columnModel, 2, centeredRenderer, 150, false);  // 开始时间
        ComponentUtil.configTableColumn(columnModel, 3, centeredRenderer, 90, false);  // 持续时间
        ComponentUtil.configTableColumn(columnModel, 4, centeredRenderer, 60, false); // 语言
        ComponentUtil.configTableColumn(columnModel, 5, leadingRenderer, 160, true); // 标题
        ComponentUtil.configTableColumn(columnModel, 6, leadingRenderer, 160, true); // 描述

        setLayout(new MigLayout("insets 2", "[360!][grow]", "grow"));
        add(new JScrollPane(serviceTree), "grow");
        add(new JScrollPane(eventTable), "grow");
    }

    public void reset()
    {
        treeRoot.removeAllChildren();
        treeModel.reload();
        eventTableModel.update(Collections.emptyList());
        serviceRegistry = Collections.emptyMap();
        eventRegistry = Collections.emptyMap();
    }

    public void update(Map<String, NVODService> services, Map<String, NVODEvent> events)
    {
        if (!isSameServices(serviceRegistry, services) || !isSameEventGroups(eventRegistry, events))
        {
            treeRoot.removeAllChildren();

            Map<NVODService, List<NVODService>> groups = groupServices(services);
            for (Map.Entry<NVODService, List<NVODService>> group : groups.entrySet())
            {
                treeRoot.add(createServiceGroupNode(group.getKey(), group.getValue()));
            }

            serviceTree.expandPath(new TreePath(treeRoot));
            treeModel.reload();

            eventRegistry = events;
            serviceRegistry = services;
        }
    }

    private boolean isSameServices(Map<String, NVODService> current, Map<String, NVODService> incoming)
    {
        return Objects.equals(current.keySet(), incoming.keySet());
    }

    private boolean isSameEventGroups(Map<String, NVODEvent> current, Map<String, NVODEvent> incoming)
    {
        return Objects.equals(current.keySet(), incoming.keySet());
    }

    private Map<NVODService, List<NVODService>> groupServices(Map<String, NVODService> services)
    {
        Map<NVODService, List<NVODService>> groups = new TreeMap<>();
        for (NVODService service : services.values())
        {
            if (service.isTimeShiftedService())
            {
                NVODService refService = services.get(service.getReferenceId());
                if (refService == null)
                    continue; // 缺少描述
                for (NVODService refSrv : groups.keySet())
                {
                    if (NVODService.isSameReferenceService(refService, refSrv))
                    {
                        refService = refSrv;
                        break;
                    }
                }
                groups.computeIfAbsent(refService, key -> new ArrayList<>())
                      .add(service);
                Collections.sort(groups.get(refService));
            }
        }
        return groups;
    }

    private DefaultMutableTreeNode createServiceGroupNode(NVODService referenceService, List<NVODService> shiftedServices)
    {
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode();
        groupNode.setUserObject(referenceService);

        for (NVODService shiftedService : shiftedServices)
        {
            DefaultMutableTreeNode srvNode = new DefaultMutableTreeNode();
            srvNode.setUserObject(shiftedService);
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
                NVODService service = (NVODService) node.getUserObject();
                if (service.isReferenceService())
                {
                    setText(String.format("索引业务%d", service.getServiceId()));
                    setIcon(SmallIcons.TRANSMIT);
                } else
                {
                    setText(String.format("时移业务%d", service.getServiceId()));
                    setIcon(SmallIcons.TELEVISION);
                }
            }

            return this;
        }
    }
}
