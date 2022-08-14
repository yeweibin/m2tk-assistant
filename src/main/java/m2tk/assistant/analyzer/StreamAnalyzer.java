package m2tk.assistant.analyzer;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
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
        try
        {
            input = ProtocolManager.openRxChannel(resource);
            databaseService.resetDatabase();
            databaseService.addSource((String) input.query("source name"));
        } catch (Exception ex)
        {
            log.warn("无法获取输入通道：{}", ex.getMessage());
            input = null;
            return false;
        }

        demuxer.reset();

        List<Tracer> tracers = Arrays.asList(new StreamTracer(databaseService),
                                             new PSITracer(databaseService),
                                             new SITracer(databaseService),
                                             new TR290Tracer1(databaseService),
                                             new TR290Tracer2(databaseService)
                                            );
        tracers.forEach(tracer -> tracer.configureDemux(demuxer));

        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxerStopped));
//        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, new TR290Printer(databaseService)));

        demuxer.attach(input);

        log.info("开始分析");
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
                    log.warn("Exception: {}", ex.getMessage(), ex);
                }
            }
        }
    }
}
