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

package m2tk.assistant.app.kernel.service;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.Tracer;
import m2tk.assistant.api.domain.StreamSource;
import m2tk.assistant.api.event.SourceAttachedEvent;
import m2tk.assistant.api.event.SourceDetachedEvent;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Component
public class StreamAnalyzer
{
    private final ExecutorService executor;
    private final TSDemux demux;

    @Inject
    private EventBus bus;
    @Inject
    private M2TKDatabase database;

    private RxChannel input;

    public StreamAnalyzer()
    {
        executor = Executors.newSingleThreadExecutor();
        demux = TSDemux.newDefaultDemux(executor);
    }

    /**
     * 开始分析输入流
     * @param uri 输入流地址
     * @param tracers 相关码流分析器
     * @param consumer 解复用消息监听器
     * @return 是否开始分析任务
     */
    public boolean start(String uri, List<Tracer> tracers, Consumer<DemuxStatus> consumer)
    {
        try
        {
            input = ProtocolManager.openRxChannel(uri);
        } catch (Exception ex)
        {
            log.error("无法获取输入通道：{}", ex.getMessage());
            input = null;
            return false;
        }

        demux.reset();

        StreamSource source = database.beginDiagnosis((String) input.query("source name"), uri);
        tracers.forEach(tracer -> tracer.configure(source, demux, database));

        demux.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
        demux.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxStopped));

        demux.attach(input);

        bus.post(new SourceAttachedEvent(source));
        log.info("开始分析");

        return true;
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

    private void closeChannelWhenDemuxStopped(DemuxStatus status)
    {
        if (!status.isRunning())
        {
            try
            {
                input.close();
            } catch (IOException ex)
            {
                log.error("关闭通道时异常：{}", ex.getMessage());
            }

            bus.post(new SourceDetachedEvent());
            log.info("停止分析");
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
                    log.error("处理 {} 消息时异常：{}", type.getSimpleName(), ex.getMessage(), ex);
                }
            }
        }
    }
}
