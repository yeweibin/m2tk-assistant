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

package m2tk.assistant.ui.component;

import m2tk.assistant.core.domain.StreamSource;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Objects;

public class SourceInfoPanel extends JPanel
{
    private JTextField fieldSourceName;
    private JTextField fieldBitrate;
    private JTextField fieldFrameSize;
    private JTextField fieldPacketCount;
    private JTextField fieldTransportStreamId;
    private StreamSource currentSource;

    public SourceInfoPanel()
    {
        initUI();
    }

    private void initUI()
    {
        fieldSourceName = new JTextField();
        fieldBitrate = new JTextField();
        fieldFrameSize = new JTextField();
        fieldPacketCount = new JTextField();
        fieldTransportStreamId = new JTextField();

        fieldSourceName.setEditable(false);
        fieldBitrate.setEditable(false);
        fieldFrameSize.setEditable(false);
        fieldPacketCount.setEditable(false);
        fieldTransportStreamId.setEditable(false);

        setLayout(new MigLayout("", "[50!][100:200:][50!][50!][10!]", ""));

        add(new JLabel("输入源"), "left");
        add(fieldSourceName, "span 4, grow, wrap");

        add(new JLabel("传输包数"), "left");
        add(fieldPacketCount, "grow");
        add(new JLabel("包长度"), "left");
        add(fieldFrameSize, "span 2, grow, wrap");

        add(new JLabel("当前码率"), "left");
        add(fieldBitrate, "grow");
        add(new JLabel("传输流号"), "left");
        add(fieldTransportStreamId, "span 2, grow, wrap");

        currentSource = null;
    }

    public void resetSourceInfo()
    {
        fieldSourceName.setText(null);
        fieldBitrate.setText(null);
        fieldFrameSize.setText(null);
        fieldPacketCount.setText(null);
        fieldTransportStreamId.setText(null);
        currentSource = null;
    }

    public void updateSourceInfo(StreamSource source)
    {
        if (source == null || isSame(currentSource, source))
            return;

        String oldSourceName = fieldSourceName.getText();
        if (!Objects.equals(oldSourceName, source.getName()))
        {
            fieldSourceName.setText(source.getName());
            fieldSourceName.setToolTipText(source.getName());
        }

        fieldBitrate.setText(String.format("%,d bps", source.getBitrate()));
        fieldFrameSize.setText("" + source.getFrameSize());
        fieldPacketCount.setText(String.format("%,d", source.getPacketCount()));
        fieldTransportStreamId.setText("" + source.getTransportStreamId());
        currentSource = source;
    }

    private boolean isSame(StreamSource current, StreamSource incoming)
    {
        if (current == null || incoming == null)
            return false;

        return current.getId() == incoming.getId() &&
               current.getPacketCount() == incoming.getPacketCount();
    }
}
