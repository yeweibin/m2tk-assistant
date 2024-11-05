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
