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

package m2tk.assistant.template;

import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StreamLoader
{
    private final static Logger log = LoggerFactory.getLogger(StreamLoader.class);
    private final ExecutorService executor;
    private final TSDemux demux;
    private RxChannel input;

    public StreamLoader()
    {
        executor = Executors.newSingleThreadExecutor();
        demux = TSDemux.newDefaultDemux(executor);
    }

    public boolean start(String url, Consumer<DemuxStatus> consumer)
    {
        try
        {
            input = ProtocolManager.openRxChannel(url);

            demux.reset();
            demux.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
            demux.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxStopped));
            demux.attach(input);

            return true;
        } catch (Exception ex)
        {
            log.warn("启动Demux时异常：{}", ex.getMessage());

            demux.detach();
            closeSilently(input);

            input = null;
            return false;
        }
    }

    public void stop()
    {
        demux.detach();
    }

    public void shutdown()
    {
        demux.shutdown();
        executor.shutdownNow();
    }

    public void join()
    {
        demux.join();
    }

    private void closeChannelWhenDemuxStopped(DemuxStatus status)
    {
        if (!status.isRunning())
        {
            closeSilently(input);
            log.info("停止分析");
        }
    }

    private void closeSilently(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            } catch (Exception ignored)
            {
            }
        }
    }

    static class EventFilter<T extends TSDemuxEvent> implements Consumer<TSDemuxEvent>
    {
        private final Class<T> type;
        private final Consumer<T> consumer;

        EventFilter(Class<T> type, Consumer<T> consumer)
        {
            this.type = type;
            this.consumer = consumer;
        }

        @Override
        public void accept(TSDemuxEvent event)
        {
            if (type.isInstance(event))
            {
                try
                {
                    consumer.accept(type.cast(event));
                } catch (Exception ex)
                {
                    log.warn("Exception: {}", ex.getMessage());
                }
            }
        }
    }
}
