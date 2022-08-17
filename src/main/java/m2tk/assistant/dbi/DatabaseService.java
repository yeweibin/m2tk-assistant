package m2tk.assistant.dbi;

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

        SnowflakeGenerator generator = new SnowflakeGenerator();

        sourceHandler = new SourceHandler(generator);
        streamHandler = new StreamHandler(generator);
        psiHandler = new PSIObjectHandler(generator);
        siHandler = new SIObjectHandler(generator);
        tr290Handler = new TR290EventHandler(generator);
        pcrHandler = new PCRHandler(generator);
        sectionHandler = new SectionHandler(generator);
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

    public void resetDatabase()
    {
        dbi.useHandle(handle -> {
            sourceHandler.resetTable(handle);
            streamHandler.resetTable(handle);
            psiHandler.resetTable(handle);
            siHandler.resetTable(handle);
            tr290Handler.resetTable(handle);
            pcrHandler.resetTable(handle);
            sectionHandler.resetTable(handle);
        });
    }

    public StreamEntity getStream(int pid)
    {
        return dbi.withHandle(handle -> streamHandler.getStream(handle, pid));
    }

    public void updateStreamStatistics(StreamEntity stream)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamStatistics(handle, stream));
    }

    public void cumsumStreamErrorCounts(int pid, long transportErrors, long continuityErrors)
    {
        dbi.useHandle(handle -> streamHandler.cumsumStreamErrorCounts(handle, pid, transportErrors, continuityErrors));
    }

    public void updateStreamUsage(int pid, String category, String description)
    {
        dbi.useHandle(handle -> streamHandler.updateStreamUsage(handle, pid, category, description));
    }

    public List<StreamEntity> listStreams()
    {
        return dbi.withHandle(streamHandler::listPresentStreams);
    }

    public Map<Integer, StreamEntity> getStreamRegistry()
    {
        return dbi.withHandle(handle -> streamHandler.listPresentStreams(handle)
                                                     .stream()
                                                     .collect(toMap(StreamEntity::getPid, Function.identity()))
                             );
    }

    public void addSource(String name)
    {
        dbi.useHandle(handle -> sourceHandler.addSource(handle, name));
    }

    public void updateSourceStatistics(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceStatistics(handle, source));
    }

    public void updateSourceTransportId(SourceEntity source)
    {
        dbi.useHandle(handle -> sourceHandler.updateSourceTransportId(handle, source));
    }

    public SourceEntity getSource()
    {
        return dbi.withHandle(sourceHandler::getLatestSource);
    }

    public void clearPrograms()
    {
        dbi.useHandle(psiHandler::clearProgramAndMappingStreams);
    }

    public ProgramEntity addProgram(int tsid, int number, int pmtpid)
    {
        return dbi.withHandle(handle -> psiHandler.addProgram(handle, tsid, number, pmtpid));
    }

    public void updateProgram(ProgramEntity program)
    {
        dbi.useHandle(handle -> psiHandler.updateProgram(handle, program));
    }

    public void addProgramStreamMapping(int program, int pid, int type, String category, String description)
    {
        dbi.useHandle(handle -> psiHandler.addProgramStreamMapping(handle, program, pid, type, category, description));
    }

    public Map<ProgramEntity, List<ProgramStreamMappingEntity>> getProgramMappings()
    {
        return dbi.withHandle(handle -> {
            List<ProgramEntity> programs = psiHandler.listPrograms(handle);
            Map<ProgramEntity, List<ProgramStreamMappingEntity>> mappings = new HashMap<>();
            for (ProgramEntity program : programs)
            {
                mappings.put(program,
                             psiHandler.listProgramStreamMappings(handle, program.getProgramNumber()));
            }
            return mappings;
        });
    }

    public Map<Integer, List<CAStreamEntity>> listECMGroups()
    {
        return dbi.withHandle(psiHandler::listECMGroups);
    }

    public void addEMMStream(int systemId, int pid, byte[] privateData)
    {
        dbi.useHandle(handle -> psiHandler.addEMMStream(handle, systemId, pid, privateData));
    }

    public void addECMStream(int systemId, int pid, byte[] privateData, int programNumber, int esPid)
    {
        dbi.useHandle(handle -> psiHandler.addECMStream(handle, systemId, pid, privateData, programNumber, esPid));
    }

    public List<CAStreamEntity> listCAStreams()
    {
        return dbi.withHandle(psiHandler::listCAStreams);
    }

    public SIBouquetEntity addBouquet(int bouquetId)
    {
        return dbi.withHandle(handle -> siHandler.addBouquet(handle, bouquetId));
    }

    public void updateBouquetName(SIBouquetEntity bouquet)
    {
        dbi.useHandle(handle -> siHandler.updateBouquetName(handle, bouquet));
    }

    public void addBouquetServiceMapping(int bouquetId,
                                         int transportStreamId,
                                         int originalNetworkId,
                                         int serviceId)
    {
        dbi.useHandle(handle -> siHandler.addBouquetServiceMapping(handle,
                                                                   bouquetId,
                                                                   transportStreamId,
                                                                   originalNetworkId,
                                                                   serviceId));
    }

    public SINetworkEntity addNetwork(int networkId, boolean isActual)
    {
        return dbi.withHandle(handle -> siHandler.addNetwork(handle, networkId, isActual));
    }

    public void updateNetworkName(SINetworkEntity network)
    {
        dbi.useHandle(handle -> siHandler.updateNetworkName(handle, network));
    }

    public SIMultiplexEntity addMultiplex(int networkId, int transportStreamId, int originalNetworkId)
    {
        return dbi.withHandle(handle -> siHandler.addMultiplex(handle,
                                                               networkId,
                                                               transportStreamId,
                                                               originalNetworkId));
    }

    public SIServiceEntity addService(int transportStreamId,
                                      int originalNetworkId,
                                      int serviceId,
                                      String runningStatus,
                                      boolean isFreeCAMode,
                                      boolean isPnfEITEnabled,
                                      boolean isSchEITEnabled,
                                      boolean isActualTS)
    {
        return dbi.withHandle(handle -> siHandler.addService(handle,
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

    public SIEventEntity addPresentFollowingEvent(int transportStreamId,
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

    public SIEventEntity addScheduleEvent(int transportStreamId,
                                          int originalNetworkId,
                                          int serviceId,
                                          int eventId,
                                          String startTime,
                                          String duration,
                                          String runningStatus,
                                          boolean isFreeCAMode)
    {
        return dbi.withHandle(handle -> siHandler.addScheduleEvent(handle,
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

    public void addDateTime(long timepoint)
    {
        dbi.useHandle(handle -> siHandler.addDateTime(handle, timepoint));
    }

    public SIDateTimeEntity getLatestDateTime()
    {
        return dbi.withHandle(siHandler::getLatestDateTime);
    }

    public List<SINetworkEntity> listNetworks()
    {
        return dbi.withHandle(siHandler::listNetworks);
    }

    public List<SIMultiplexEntity> listMultiplexes()
    {
        return dbi.withHandle(siHandler::listMultiplexes);
    }

    public List<SIServiceEntity> listServices()
    {
        return dbi.withHandle(siHandler::listServices);
    }

    public List<SIServiceEntity> listServices(int tsid)
    {
        return dbi.withHandle(handle -> siHandler.listServices(handle, tsid));
    }

    public List<SIMultiplexServiceCountView> listMultiplexServiceCounts()
    {
        return dbi.withHandle(siHandler::listMultiplexServiceCounts);
    }

    public void updateMultiplexDeliverySystemConfigure(SIMultiplexEntity multiplex)
    {
        dbi.useHandle(handle -> siHandler.updateMultiplexDeliverySystemConfigure(handle, multiplex));
    }

    public List<SIEventEntity> listEvents()
    {
        return dbi.withHandle(siHandler::listEvents);
    }

    public void addTR290Event(LocalDateTime timestamp, String type, String description, long position, int pid)
    {
        dbi.useHandle(handle -> tr290Handler.addTR290Event(handle, timestamp, type, description, position, pid));
    }

    public List<TR290EventEntity> listTR290Events(long start, int count)
    {
        return dbi.withHandle(handle -> tr290Handler.listEvents(handle, start, count));
    }

    public List<TR290EventEntity> listTR290Events(String type, int count)
    {
        return dbi.withHandle(handle -> tr290Handler.listEvents(handle, type, count));
    }

    public List<TR290StatEntity> listTR290Stats()
    {
        return dbi.withHandle(tr290Handler::listStats);
    }

    public void addPCR(int pid, long position, long value)
    {
        dbi.useHandle(handle -> pcrHandler.addPCR(handle, pid, position, value));
    }

    public void addPCRCheck(int pid,
                            long prevValue, long prevPosition,
                            long currValue, long currPosition,
                            long bitrate,
                            long interval, long diff, long accuracy,
                            boolean repetitionCheckFailed,
                            boolean discontinuityCheckFailed,
                            boolean accuracyCheckFailed)
    {
        dbi.useHandle(handle -> pcrHandler.addPCRCheck(handle,
                                                       pid,
                                                       prevValue, prevPosition,
                                                       currValue, currPosition,
                                                       bitrate,
                                                       interval, diff, accuracy,
                                                       repetitionCheckFailed,
                                                       discontinuityCheckFailed,
                                                       accuracyCheckFailed));
    }

    public List<PCRStatEntity> listPCRStats()
    {
        return dbi.withHandle(pcrHandler::listPCRStats);
    }

    public List<PCREntity> getRecentPCRs(int pid, int limit)
    {
        return dbi.withHandle(handle -> pcrHandler.listRecentPCRs(handle, pid, limit));
    }

    public List<PCRCheckEntity> getRecentPCRChecks(int pid, int limit)
    {
        return dbi.withHandle(handle -> pcrHandler.listRecentPCRChecks(handle, pid, limit));
    }

    public void addSection(String tag, int pid, long position, byte[] encoding)
    {
        dbi.useHandle(handle -> sectionHandler.addSection(handle, tag, pid, position, encoding));
    }

    public List<SectionEntity> getSectionGroups(String tagPrefix)
    {
        return dbi.withHandle(handle -> sectionHandler.getSections(handle, tagPrefix));
    }

    public Map<String, List<SectionEntity>> getSectionGroups()
    {
        return dbi.withHandle(sectionHandler::getSectionGroups);
    }
}
