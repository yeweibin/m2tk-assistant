package m2tk.assistant.analyzer;

import cn.hutool.core.io.IoUtil;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.io.ProtocolManager;
import m2tk.io.RxChannel;
import m2tk.multiplex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StreamAnalyzer
{
    private static final Logger logger = LoggerFactory.getLogger(StreamAnalyzer.class);
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
        try
        {
            input = ProtocolManager.openRxChannel(resource);
            databaseService.resetDatabase();
            databaseService.addSource((String) input.query("source name"));
        } catch (Exception ex)
        {
            logger.warn("无法获取输入通道：{}", ex.getMessage());
            input = null;
            return false;
        }

        StreamRecorder streamRecorder = new StreamRecorder(databaseService);
        PSIRecorder psiRecorder = new PSIRecorder(databaseService);

        demuxer.reset();
        demuxer.registerEventListener(new EventFilter<>(TransportStatus.class, streamRecorder::processTransportStatus));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, consumer));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, streamRecorder::processDemuxStatus));
        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, this::closeChannelWhenDemuxerStopped));
//        demuxer.registerEventListener(new EventFilter<>(DemuxStatus.class, new ResultPrinter(databaseService)));

        TSDemux.Channel channel0 = demuxer.requestChannel(TSDemuxPayload.Type.RAW);
        channel0.setPayloadHandler(streamRecorder::processTransportPacket);
        channel0.setStreamPID(TSDemux.Channel.ANY_PID);
        channel0.setEnabled(true);

        TSDemux.Channel channel1 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel1.setPayloadHandler(psiRecorder::processPAT);
        channel1.setStreamPID(0x0000);
        channel1.setEnabled(true);
        TSDemux.Channel channel2 = demuxer.requestChannel(TSDemuxPayload.Type.SECTION);
        channel2.setPayloadHandler(psiRecorder::processCAT);
        channel2.setStreamPID(0x0001);
        channel2.setEnabled(true);

        demuxer.attach(input);

        logger.info("开始分析");
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
        running = status.isRunning();
        if (!status.isRunning())
        {
            IoUtil.close(input);
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
                    logger.warn("Exception: {}", ex.getMessage(), ex);
                }
            }
        }
    }
}
