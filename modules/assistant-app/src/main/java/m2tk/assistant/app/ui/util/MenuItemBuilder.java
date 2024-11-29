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
package m2tk.assistant.app.ui.util;

import javax.swing.*;

public final class MenuItemBuilder
{
    private JMenuItem item;

    public MenuItemBuilder create(Action action)
    {
        item = new JMenuItem(action);
        return this;
    }

    public MenuItemBuilder createCheckbox(Action action)
    {
        item = new JCheckBoxMenuItem(action);
        return this;
    }

    public MenuItemBuilder icon(Icon icon)
    {
        item.setIcon(icon);
        return this;
    }

    public MenuItemBuilder disabledIcon(Icon icon)
    {
        item.setDisabledIcon(icon);
        return this;
    }

    public MenuItemBuilder text(String text)
    {
        item.setText(text);
        return this;
    }

    public MenuItemBuilder tooltip(String tooltip)
    {
        item.setToolTipText(tooltip);
        return this;
    }

    public MenuItemBuilder mnemonic(int mnemonic)
    {
        item.setMnemonic(mnemonic);
        return this;
    }

    public JMenuItem get()
    {
        return item;
    }
}
