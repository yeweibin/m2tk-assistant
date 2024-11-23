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

package m2tk.assistant.api;

import com.google.common.eventbus.EventBus;
import org.jdesktop.application.Application;
import org.pf4j.ExtensionPoint;

import javax.swing.*;

public interface InfoView extends ExtensionPoint
{
    void setupApplication(Application application);

    void setupDataSource(EventBus bus, M2TKDatabase database);

    void setupMenu(JMenu menu);

    JComponent getViewComponent();

    String getViewTitle();

    Icon getViewIcon();
}
