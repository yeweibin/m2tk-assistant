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

public class ButtonBuilder
{
    private JButton button;

    public ButtonBuilder create(Action action)
    {
        button = new JButton(action);
        return this;
    }

    public ButtonBuilder icon(Icon icon)
    {
        button.setIcon(icon);
        return this;
    }

    public ButtonBuilder disabledIcon(Icon icon)
    {
        button.setDisabledIcon(icon);
        return this;
    }

    public ButtonBuilder text(String text)
    {
        button.setText(text);
        return this;
    }

    public ButtonBuilder tooltip(String tooltip)
    {
        button.setToolTipText(tooltip);
        return this;
    }

    public ButtonBuilder mnemonic(int mnemonic)
    {
        button.setMnemonic(mnemonic);
        return this;
    }

    public JButton get()
    {
        return button;
    }
}
