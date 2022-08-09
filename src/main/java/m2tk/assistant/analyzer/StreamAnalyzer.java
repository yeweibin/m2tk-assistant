package m2tk.assistant.analyzer;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import m2tk.multiplex.DemuxStatus;
import m2tk.multiplex.TSDemux;
import m2tk.multiplex.TSDemuxEvent;
import m2tk.multiplex.TSDemuxPayload;

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

        StreamTracer streamTracer = new StreamTracer(databaseService);
        PSITracer psiTracer = new PSITracer(databaseService);
        SITracer siTracer = new SITracer(databaseService);

        demuxer.reset();
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, streamTracer::processDemuxStatus));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxerStopped));
//        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, new ResultPrinter(databaseService)));

        TSDemux.Channel channel0 = demuxer.requestChannel(TSDemuxPayload.Type.RAW);
        channel0.setPayloadHandler(streamTracer::processTransportPacket);
        channel0.setStreamPID(TSDemux.Channel.ANY_PID);
        channel0.setEnabled(true);

        TSDemux.Channel channel1 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel1.setPayloadHandler(psiTracer::processPAT);
        channel1.setStreamPID(0x0000);
        channel1.setEnabled(true);
        TSDemux.Channel channel2 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel2.setPayloadHandler(psiTracer::processCAT);
        channel2.setStreamPID(0x0001);
        channel2.setEnabled(true);

        TSDemux.Channel channel3 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel3.setPayloadHandler(siTracer::processNIT);
        channel3.setStreamPID(0x0010);
        channel3.setEnabled(true);
        TSDemux.Channel channel4 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel4.setPayloadHandler(siTracer::processBAT);
        channel4.setStreamPID(0x0011);
        channel4.setEnabled(true);
        TSDemux.Channel channel5 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel5.setPayloadHandler(siTracer::processSDT);
        channel5.setStreamPID(0x0011);
        channel5.setEnabled(true);
        TSDemux.Channel channel6 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel6.setPayloadHandler(siTracer::processEIT);
        channel6.setStreamPID(0x0012);
        channel6.setEnabled(true);
        TSDemux.Channel channel7 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel7.setPayloadHandler(siTracer::processTDT);
        channel7.setStreamPID(0x0014);
        channel7.setEnabled(true);

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
