package m2tk.assistant.dbi;

import cn.hutool.core.lang.generator.Generator;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import m2tk.assistant.dbi.entity.*;
import m2tk.assistant.dbi.handler.*;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class DatabaseService
{
    private final Jdbi dbi;
    private final Generator<Long> idGenerator;
    private final SourceHandler sourceHandler;
    private final StreamHandler streamHandler;
    private final PSIObjectHandler psiHandler;
    private final SIObjectHandler siHandler;
    private final TR290EventHandler tr290Handler;
    private final PCRHandler pcrHandler;
    private final SectionHandler sectionHandler;

    public DatabaseService()
    {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:m2tk");

        dbi = Jdbi.create(new HikariDataSource(config)).installPlugin(new H2DatabasePlugin());

        idGenerator = new SnowflakeGenerator();

        sourceHandler = new SourceHandler(idGenerator);
        streamHandler = new StreamHandler(idGenerator);
        psiHandler = new PSIObjectHandler(idGenerator);
        siHandler = new SIObjectHandler(idGenerator);
        tr290Handler = new TR290EventHandler(idGenerator);
        pcrHandler = new PCRHandler(idGenerator);
        sectionHandler = new SectionHandler(idGenerator);
    }

    public void initDatabase()
    {
        dbi.useTransaction(handle -> {
            sourceHandler.initTable(handle);
            streamHandler.initTable(handle);
            psiHandler.initTable(handle);
            siHandler.initTable(handle);
            tr290Handler.initTable(handle);
            pcrHandler.initTable(handle);
            sectionHandler.initTable(handle);
        });
    }

    public void initStreamContext(long transactionId)
    {
        dbi.useHandle(handle -> streamHandler.initForTransaction(handle, transactionId));
    }

    public long requestTransactionId()
    {
        return idGenerator.next();
    }

    public StreamEntity getStream(long transactionId, int pid)
    {
        return dbi.withHandle(handle -> streamHandler.getStream(handle, transactionId, pid));
    }

    public void updateStreamStatistics(StreamEntity stream)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamStatistics(handle, stream));
    }

    public void cumsumStreamErrorCounts(long transactionId, int pid, long transportErrors, long continuityErrors)
    {
        dbi.useHandle(handle -> streamHandler.cumsumStreamErrorCounts(handle, transactionId, pid, transportErrors, continuityErrors));
    }

    public void updateStreamUsage(long transactionId, int pid, String category, String description)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamUsage(handle, transactionId, pid, category, description));
    }

    public List<StreamEntity> listStreams(long transactionId)
    {
        return dbi.withHandle(handle -> streamHandler.listPresentStreams(handle, transactionId));
    }

    public Map<Integer, StreamEntity> getStreamRegistry(long transactionId)
    {
        return dbi.withHandle(handle -> streamHandler.listPresentStreams(handle, transactionId)
                                                     .stream()
                                                     .collect(toMap(StreamEntity::getPid, Function.identity()))
                             );
    }

    public void addSource(long transactionId, String name)
    {
        dbi.useHandle(handle -> sourceHandler.addSource(handle, transactionId, name));
    }

    public void updateSourceStatistics(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceStatistics(handle, source));
    }

    public void updateSourceTransportId(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceTransportId(handle, source));
    }

    public SourceEntity getSource(long transactionId)
    {
        return dbi.withHandle(handle -> sourceHandler.getSource(handle, transactionId));
    }

    public void clearPrograms(long transactionId)
    {
        dbi.useHandle(handle -> psiHandler.clearProgramAndMappingStreams(handle, transactionId));
    }

    public ProgramEntity addProgram(long transactionId, int tsid, int number, int pmtpid)
    {
        return dbi.withHandle(handle -> psiHandler.addProgram(handle, transactionId, tsid, number, pmtpid));
    }

    public void updateProgram(ProgramEntity program)
    {
        dbi.useHandle(handle -> psiHandler.updateProgram(handle, program));
    }

    public void addProgramStreamMapping(long transactionId, int program, int pid, int type, String category, String description)
    {
        dbi.useHandle(handle -> psiHandler.addProgramStreamMapping(handle, transactionId, program, pid, type, category, description));
    }

    public Map<ProgramEntity, List<ProgramStreamMappingEntity>> getProgramMappings(long transactionId)
    {
        return dbi.withHandle(handle -> {
            List<ProgramEntity> programs = psiHandler.listPrograms(handle, transactionId);
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = new HashMap<>();
            for (ProgramEntity program : programs)
            {
                mappings.put(program,
                             psiHandler.listProgramStreamMappings(handle, transactionId, program.getProgramNumber()));
            }
            return mappings;
        });
    }

    public Map<Integer, List<CAStreamEntity>> listECMGroups(long transactionId)
    {
        return dbi.withHandle(handle -> psiHandler.listECMGroups(handle, transactionId));
    }

    public void addEMMStream(long transactionId, int systemId, int pid, byte[] privateData)
    {
        dbi.useHandle(handle -> psiHandler.addEMMStream(handle, transactionId, systemId, pid, privateData));
    }

    public void addECMStream(long transactionId, int systemId, int pid, byte[] privateData, int programNumber, int esPid)
    {
        dbi.useHandle(handle -> psiHandler.addECMStream(handle, transactionId, systemId, pid, privateData, programNumber, esPid));
    }

    public List<CAStreamEntity> listCAStreams(long transactionId)
    {
        return dbi.withHandle(handle -> psiHandler.listCAStreams(handle, transactionId));
    }

    public SIBouquetEntity addBouquet(long transactionId, int bouquetId)
    {
        return dbi.withHandle(handle -> siHandler.addBouquet(handle, transactionId, bouquetId));
    }

    public void updateBouquetName(SIBouquetEntity bouquet)
    {
        dbi.useHandle(handle -> siHandler.updateBouquetName(handle, bouquet));
    }

    public void addBouquetServiceMapping(long transactionId,
                                         int bouquetId,
                                         int transportStreamId,
                                         int originalNetworkId,
                                         int serviceId)
    {
        dbi.useHandle(handle -> siHandler.addBouquetServiceMapping(handle,
                                                                   transactionId,
                                                                   bouquetId,
                                                                   transportStreamId,
                                                                   originalNetworkId,
                                                                   serviceId));
    }

    public SINetworkEntity addNetwork(long transactionId, int networkId, boolean isActual)
    {
        return dbi.withHandle(handle -> siHandler.addNetwork(handle, transactionId, networkId, isActual));
    }

    public void updateNetworkName(SINetworkEntity network)
    {
        dbi.useHandle(handle -> siHandler.updateNetworkName(handle, network));
    }

    public SIMultiplexEntity addMultiplex(long transactionId, int networkId, int transportStreamId, int originalNetworkId)
    {
        return dbi.withHandle(handle -> siHandler.addMultiplex(handle,
                                                               transactionId,
                                                               networkId,
                                                               transportStreamId,
                                                               originalNetworkId));
    }

    public SIServiceEntity addService(long transactionId,
                                      int transportStreamId,
                                      int originalNetworkId,
                                      int serviceId,
                                      String runningStatus,
                                      boolean isFreeCAMode,
                                      boolean isPnfEITEnabled,
                                      boolean isSchEITEnabled,
                                      boolean isActualTS)
    {
        return dbi.withHandle(handle -> siHandler.addService(handle,
                                                             transactionId,
                                                             transportStreamId,
                                                             originalNetworkId,
                                                             serviceId,
                                                             runningStatus,
                                                             isFreeCAMode,
                                                             isPnfEITEnabled,
                                                             isSchEITEnabled,
                                                             isActualTS));
    }

    public void updateServiceDetails(SIServiceEntity service)
    {
        dbi.useHandle(handle -> {
            siHandler.updateServiceType(handle, service);
            siHandler.updateServiceName(handle, service);
        });
    }

    public void updateServiceReference(SIServiceEntity service)
    {
        dbi.useHandle(handle -> siHandler.updateServiceReference(handle, service));
    }

    public SIEventEntity addPresentFollowingEvent(long transactionId,
                                                  int transportStreamId,
                                                  int originalNetworkId,
                                                  int serviceId,
                                                  int eventId,
                                                  String startTime,
                                                  String duration,
                                                  String runningStatus,
                                                  boolean isFreeCAMode,
                                                  boolean isPresent)
    {
        return dbi.withHandle(handle -> siHandler.addPresentFollowingEvent(handle,
                                                                           transactionId,
                                                                           transportStreamId,
                                                                           originalNetworkId,
                                                                           serviceId,
                                                                           eventId,
                                                                           startTime,
                                                                           duration,
                                                                           runningStatus,
                                                                           isFreeCAMode,
                                                                           isPresent));
    }

    public SIEventEntity addScheduleEvent(long transactionId,
                                          int transportStreamId,
                                          int originalNetworkId,
                                          int serviceId,
                                          int eventId,
                                          String startTime,
                                          String duration,
                                          String runningStatus,
                                          boolean isFreeCAMode)
    {
        return dbi.withHandle(handle -> siHandler.addScheduleEvent(handle,
                                                                   transactionId,
                                                                   transportStreamId,
                                                                   originalNetworkId,
                                                                   serviceId,
                                                                   eventId,
                                                                   startTime,
                                                                   duration,
                                                                   runningStatus,
                                                                   isFreeCAMode));
    }

    public void updateEventDescription(SIEventEntity event)
    {
        dbi.useHandle(handle -> siHandler.updateEventDescription(handle, event));
    }

    public void updateEventReference(SIEventEntity event)
    {
        dbi.useHandle(handle -> siHandler.updateEventReference(handle, event));
    }

    public void addDateTime(long transactionId, long timepoint)
    {
        dbi.useHandle(handle -> siHandler.addDateTime(handle, transactionId, timepoint));
    }

    public SIDateTimeEntity getLatestDateTime(long transactionId)
    {
        return dbi.withHandle(handler -> siHandler.getLatestDateTime(handler, transactionId));
    }

    public List<SINetworkEntity> listNetworks(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listNetworks(handle, transactionId));
    }

    public List<SIMultiplexEntity> listMultiplexes(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listMultiplexes(handle, transactionId));
    }

    public List<SIServiceEntity> listServices(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listServices(handle, transactionId));
    }

    public List<SIServiceEntity> listServices(long transactionId, int tsid)
    {
        return dbi.withHandle(handle -> siHandler.listServices(handle, transactionId, tsid));
    }

    public List<SIServiceEntity> listNVODServices(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listNVODServices(handle, transactionId));
    }

    public List<SIMultiplexServiceCountView> listMultiplexServiceCounts(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listMultiplexServiceCounts(handle, transactionId));
    }

    public void updateMultiplexDeliverySystemConfigure(SIMultiplexEntity multiplex)
    {
        dbi.useHandle(handle -> siHandler.updateMultiplexDeliverySystemConfigure(handle, multiplex));
    }

    public List<SIEventEntity> listEvents(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listEvents(handle, transactionId));
    }

    public List<SIEventEntity> listNVODEvents(long transactionId)
    {
        return dbi.withHandle(handle -> siHandler.listNVODEvents(handle, transactionId));
    }

    public void addTR290Event(long transactionId,
                              LocalDateTime timestamp, String type, String description, long position, int pid)
    {
        dbi.useHandle(handle -> tr290Handler.addTR290Event(handle,
                                                           transactionId,
                                                           timestamp, type, description, position, pid));
    }

    public List<TR290EventEntity> listTR290Events(long transactionId, long start, int count)
    {
        return dbi.withHandle(handle -> tr290Handler.listEvents(handle, transactionId, start, count));
    }

    public List<TR290EventEntity> listTR290Events(long transactionId, String type, int count)
    {
        return dbi.withHandle(handle -> tr290Handler.listEvents(handle, transactionId, type, count));
    }

    public List<TR290StatEntity> listTR290Stats(long transactionId)
    {
        return dbi.withHandle(handle -> tr290Handler.listStats(handle, transactionId));
    }

    public void addPCR(long transactionId, int pid, long position, long value)
    {
        dbi.useHandle(handle -> pcrHandler.addPCR(handle, transactionId, pid, position, value));
    }

    public void addPCRCheck(long transactionId,
                            int pid,
                            long prevValue, long prevPosition,
                            long currValue, long currPosition,
                            long bitrate,
                            long interval, long diff, long accuracy,
                            boolean repetitionCheckFailed,
                            boolean discontinuityCheckFailed,
                            boolean accuracyCheckFailed)
    {
        dbi.useHandle(handle -> pcrHandler.addPCRCheck(handle,
                                                       transactionId,
                                                       pid,
                                                       prevValue, prevPosition,
                                                       currValue, currPosition,
                                                       bitrate,
                                                       interval, diff, accuracy,
                                                       repetitionCheckFailed,
                                                       discontinuityCheckFailed,
                                                       accuracyCheckFailed));
    }

    public List<PCRStatEntity> listPCRStats(long transactionId)
    {
        return dbi.withHandle(handle -> pcrHandler.listPCRStats(handle, transactionId));
    }

    public List<PCRCheckEntity> getRecentPCRChecks(long transactionId, int pid, int limit)
    {
        return dbi.withHandle(handle -> pcrHandler.listRecentPCRChecks(handle, transactionId, pid, limit));
    }

    public void addSection(long transactionId, String tag, int pid, long position, byte[] encoding)
    {
        dbi.useHandle(handle -> sectionHandler.addSection(handle, transactionId, tag, pid, position, encoding));
    }

    public List<SectionEntity> getSectionGroups(long transactionId, String tagPrefix)
    {
        return dbi.withHandle(handle -> sectionHandler.getSections(handle, transactionId, tagPrefix));
    }

    public Map<String, List<SectionEntity>> getSectionGroups(long transactionId)
    {
        return dbi.withHandle(handle -> sectionHandler.getSectionGroups(handle, transactionId));
    }
}
