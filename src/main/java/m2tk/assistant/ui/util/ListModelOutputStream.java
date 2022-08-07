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
            // 在JDK8里默认的字符集编码不是UTF-8，而是系统编码。
            // 所以这里要指明字符集，否则日志显示出来就是乱码。
            return buffer.toString("UTF-8");
        } catch (UnsupportedEncodingException ex)
        {
            return buffer.toString();
        }
    }
}
