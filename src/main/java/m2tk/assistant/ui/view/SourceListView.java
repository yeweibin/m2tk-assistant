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
