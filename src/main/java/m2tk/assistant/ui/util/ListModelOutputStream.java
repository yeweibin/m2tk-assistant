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

package m2tk.assistant.ui.util;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

public class ListModelOutputStream extends OutputStream
{
    private final DefaultListModel<String> model;
    private final ByteArrayOutputStream buffer;

    public ListModelOutputStream(DefaultListModel<String> model)
    {
        this.model = Objects.requireNonNull(model);
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b)
    {
        buffer.write(b);
        if (b == '\n')
        {
            String line = getBufferedString();
            if (SwingUtilities.isEventDispatchThread())
                model.add(model.getSize(), line);
            else
                SwingUtilities.invokeLater(() -> model.add(model.getSize(), line));
            buffer.reset();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        buffer.write(b);
        if (b[off + len - 1] == '\n')
        {
            String line = getBufferedString();
            if (SwingUtilities.isEventDispatchThread())
                model.add(model.getSize(), line);
            else
                SwingUtilities.invokeLater(() -> model.add(model.getSize(), line));
            buffer.reset();
        }
    }

    @Override
    public void flush()
    {
        Runnable clearLogs = () ->
        {
            if (model.getSize() >= 1000)
                model.clear();
        };
        if (SwingUtilities.isEventDispatchThread())
            clearLogs.run();
        else
            SwingUtilities.invokeLater(clearLogs);
    }

    private String getBufferedString()
    {
        try
        {
            // ???JDK8?????????????????????????????????UTF-8????????????????????????
            // ????????????????????????????????????????????????????????????????????????
            return buffer.toString("UTF-8");
        } catch (UnsupportedEncodingException ex)
        {
            return buffer.toString();
        }
    }
}
