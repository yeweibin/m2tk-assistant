package m2tk.assistant.util;

import ch.qos.logback.core.OutputStreamAppender;

import java.io.FilterOutputStream;
import java.io.OutputStream;

public class TextListLogAppender<E> extends OutputStreamAppender<E>
{
    private static final DelegatingOutputStream DELEGATING_OUTPUT_STREAM = new DelegatingOutputStream(null);

    @Override
    public void start()
    {
        setOutputStream(DELEGATING_OUTPUT_STREAM);
        super.start();
    }

    public static void setStaticOutputStream(OutputStream os)
    {
        DELEGATING_OUTPUT_STREAM.setOutputStream(os);
    }

    private static class DelegatingOutputStream extends FilterOutputStream
    {
        /**
         * Creates a delegating output stream with a NO-OP delegate
         */
        public DelegatingOutputStream(OutputStream out)
        {
            super(new OutputStream()
            {
                @Override
                public void write(int b)
                {
                }
            });
        }

        void setOutputStream(OutputStream os)
        {
            this.out = os;
        }
    }
}
