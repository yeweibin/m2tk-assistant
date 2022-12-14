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

package m2tk.assistant.analyzer;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.Global;
import m2tk.assistant.analyzer.tracer.*;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class StreamAnalyzer
{
    private final ExecutorService executor;
    private final TSDemux demuxer;
    private RxChannel input;
    private DatabaseService databaseService;
    private volatile boolean running;

    public StreamAnalyzer()
    {
        executor = Executors.newSingleThreadExecutor();
        demuxer = TSDemux.newDefaultDemux(executor);
    }

    public void setDatabaseService(DatabaseService service)
    {
        databaseService = Objects.requireNonNull(service);
    }

    public boolean start(String resource, Consumer<DemuxStatus> consumer)
    {
        long transactionId;
        try
        {
            transactionId = databaseService.requestTransactionId();
            input = ProtocolManager.openRxChannel(resource);
            databaseService.addSource(transactionId, (String) input.query("source name"));
        } catch (Exception ex)
        {
            log.warn("???????????????????????????{}", ex.getMessage());
            input = null;
            return false;
        }

        demuxer.reset();

        List<Tracer> tracers = Arrays.asList(new StreamTracer(databaseService, transactionId),
                                             new PSITracer(databaseService, transactionId),
                                             new SITracer(databaseService, transactionId),
                                             new TR290Tracer1(databaseService, transactionId),
                                             new TR290Tracer2(databaseService, transactionId),
                                             new UserPrivateSectionTracer(databaseService, transactionId,
                                                                          Global.getUserPrivateSectionStreamList(),
                                                                          Global.getPrivateSectionFilteringLimit()),
                                             new EBTSectionTracer(databaseService, transactionId)
                                            );
        tracers.forEach(tracer -> tracer.configureDemux(demuxer));

        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxerStopped));
        //        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, new SectionPrinter(databaseService)));

        demuxer.attach(input);
        running = true;

        Global.setCurrentTransactionId(transactionId);

        log.info("????????????");
        return true;
    }

    public void stop()
    {
        demuxer.detach();
    }

    public void shutdown()
    {
        demuxer.shutdown();
        executor.shutdownNow();
    }

    private void closeChannelWhenDemuxerStopped(DemuxStatus status)
    {
        if (!status.isRunning())
        {
            IoUtil.close(input);
            running = false;
            log.info("????????????");
        }
    }

    public boolean isRunning()
    {
        return running;
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
                    log.warn("Exception: {}", ex.getMessage(), ex);
                }
            }
        }
    }
}
