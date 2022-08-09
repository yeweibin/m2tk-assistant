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

import m2tk.assistant.dbi.entity.SIDateTimeEntity;
import m2tk.dvb.DVB;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDateTime;
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

        TitledBorder border = BorderFactory.createTitledBorder("网络时间");
        border.setTitleJustification(TitledBorder.LEFT);
        setBorder(border);
    }

    public void updateTime(SIDateTimeEntity entity)
    {
        if (entity == null)
            return;

        LocalDateTime time = DVB.decodeTimepointIntoLocalDateTime(entity.getTimepoint());
        text.setText(time.format(formatter));
    }

    public void resetTime()
    {
        text.setText("--/--/-- --:--:--");
    }
}
