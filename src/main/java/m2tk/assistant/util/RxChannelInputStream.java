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

package m2tk.assistant.util;

import m2tk.io.RxChannel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class RxChannelInputStream extends InputStream
{
    private final RxChannel channel;
    private final byte[] oneByte = new byte[1];

    public RxChannelInputStream(RxChannel channel)
    {
        this.channel = Objects.requireNonNull(channel);
    }

    @Override
    public int read(byte[] buf) throws IOException
    {
        return channel.read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException
    {
        return channel.read(buf, off, len);
    }

    @Override
    public int read() throws IOException
    {
        int n = read(oneByte);
        return (n > 0) ? oneByte[0] : -1;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}
