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

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NetworkTimePanel extends JPanel
{
    private final JLabel text;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public NetworkTimePanel()
    {
        text = new JLabel("--/--/-- --:--:--");
        text.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        text.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setLayout(new BorderLayout());
        add(text, BorderLayout.CENTER);
    }

    public void updateTime(OffsetDateTime time)
    {
        if (time == null)
        {
            text.setText("--/--/-- --:--:--");
        } else
        {
            LocalDateTime utcTime = time.atZoneSameInstant(ZoneId.systemDefault())
                                        .toLocalDateTime();
            text.setText(utcTime.format(formatter));
        }
    }
}
