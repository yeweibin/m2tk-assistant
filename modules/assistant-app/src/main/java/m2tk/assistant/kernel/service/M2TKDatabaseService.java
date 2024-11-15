package m2tk.assistant.kernel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.core.M2TKDatabase;
import m2tk.assistant.core.domain.*;
import m2tk.assistant.core.presets.RunningStatus;
import m2tk.assistant.core.presets.ServiceTypes;
import m2tk.assistant.kernel.ErrorCode;
import m2tk.assistant.kernel.KernelException;
import m2tk.assistant.kernel.entity.*;
import m2tk.assistant.kernel.mapper.*;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.data.sql.SqlUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class M2TKDatabaseService implements M2TKDatabase
{
    @Inject("m2tk")
    private SqlUtils sqlUtils;

    @Db("m2tk")
    private StreamSourceEntityMapper sourceMapper;
    @Db("m2tk")
    private ElementaryStreamEntityMapper streamMapper;
    @Db("m2tk")
    private MPEGProgramEntityMapper programMapper;
    @Db("m2tk")
    private CAStreamEntityMapper caStreamMapper;
    @Db("m2tk")
    private PCREntityMapper pcrMapper;
    @Db("m2tk")
    private PCRCheckEntityMapper pcrCheckMapper;
    @Db("m2tk")
    private PCRStatViewEntityMapper pcrStatMapper;
    @Db("m2tk")
    private TR290EventEntityMapper tr290EventMapper;
    @Db("m2tk")
    private TR290StatViewEntityMapper tr290StatMapper;
    @Db("m2tk")
    private SIBouquetEntityMapper bouquetMapper;
    @Db("m2tk")
    private SINetworkEntityMapper networkMapper;
    @Db("m2tk")
    private SIMultiplexEntityMapper multiplexMapper;
    @Db("m2tk")
    private SIServiceEntityMapper serviceMapper;
    @Db("m2tk")
    private SIEventEntityMapper eventMapper;
    @Db("m2tk")
    private SIDateTimeEntityMapper datetimeMapper;
    @Db("m2tk")
    private TableVersionEntityMapper tableVersionMapper;
    @Db("m2tk")
    private PrivateSectionEntityMapper sectionMapper;
    @Db("m2tk")
    private TransportPacketEntityMapper packetMapper;
    @Db("m2tk")
    private ProgramElementaryMappingEntityMapper programMappingMapper;
    @Db("m2tk")
    private MultiplexServiceMappingEntityMapper multiplexMappingMapper;
    @Db("m2tk")
    private BouquetServiceMappingEntityMapper bouquetMappingMapper;

    @Init
    public void initDatabase()
    {
        try
        {
            log.info("准备初始化数据库");
            String initScript = ResourceUtil.getResourceAsString("/db_init.sql");
            for (String statement : initScript.split(";"))
            {
                statement = statement.trim();
                if (!statement.isEmpty())
                {
                    sqlUtils.sql(statement).update();
                }
            }
            log.info("数据库初始化完毕");
        } catch (Exception ex)
        {
            log.error("无法初始化数据库：{}", ex.getMessage(), ex);
            throw new KernelException(ErrorCode.DATABASE_ERROR, "无法初始化数据库");
        }
    }

    @Override
    public StreamSource beginDiagnosis(String sourceName, String sourceUri)
    {
        try
        {
            log.info("开始设置分析上下文");

            String resetScript = ResourceUtil.getResourceAsString("/db_reset.sql");
            for (String statement : resetScript.split(";"))
            {
                statement = statement.trim();
                if (!statement.isEmpty())
                {
                    sqlUtils.sql(statement).update();
                }
            }
            log.info("清空历史数据");

            StreamSourceEntity entity = new StreamSourceEntity();
            entity.setSourceName(sourceName);
            entity.setSourceUri(sourceUri);
            entity.setBitrate(0);
            entity.setFrameSize(188);
            entity.setPacketCount(0L);
            entity.setTransportStreamId(-1);
            sourceMapper.insert(entity);
            log.info("本次数据源：{}", sourceUri);

            log.info("分析上下文设置完毕");
            return convert(entity);
        } catch (Exception ex)
        {
            log.error("数据存储异常：{}", ex.getMessage(), ex);
            throw new KernelException(ErrorCode.DATABASE_ERROR, "无法设置分析上下文");
        }
    }

    @Override
    public void updateStreamSourceStats(int streamRef, int bitrate, int frameSize, long packetCount)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(streamRef);
        change.setBitrate(streamRef);
        change.setFrameSize(frameSize);
        change.setPacketCount(packetCount);
        sourceMapper.updateById(change);
    }

    @Override
    public void updateStreamSourceTransportId(int streamRef, int transportStreamId)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(streamRef);
        change.setTransportStreamId(transportStreamId);
        sourceMapper.updateById(change);
    }

    @Override
    public void updateStreamSource(StreamSource source)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(source.getId());
        change.setBitrate(source.getBitrate());
        change.setFrameSize(source.getFrameSize());
        change.setPacketCount(source.getPacketCount());
        change.setTransportStreamId(source.getTransportStreamId());
        sourceMapper.updateById(change);
    }

    @Override
    public StreamSource getCurrentStreamSource()
    {
        StreamSourceEntity entity = sourceMapper.selectOne(Wrappers.lambdaQuery(StreamSourceEntity.class)
                                                                   .orderByDesc(StreamSourceEntity::getId)
                                                                   .last("limit 1"));
        return (entity == null) ? null : convert(entity);
    }

    @Override
    public List<StreamSource> listStreamSources()
    {
        LambdaQueryWrapper<StreamSourceEntity> query = Wrappers.lambdaQuery(StreamSourceEntity.class)
                                                               .orderByAsc(StreamSourceEntity::getId);
        return sourceMapper.selectList(query)
                           .stream()
                           .map(this::convert)
                           .toList();
    }

    @Override
    public void updateElementaryStreamStats(int pid, long pktCount, long pcrCount, int bitrate, double ratio, boolean scrambled)
    {
        ElementaryStreamEntity change = new ElementaryStreamEntity();
        change.setPid(pid);
        change.setBitrate(bitrate);
        change.setRatio(ratio);
        change.setPacketCount(pktCount);
        change.setPcrCount(pcrCount);
        change.setScrambled(scrambled);
        streamMapper.updateById(change);
    }

    @Override
    public void updateElementaryStreamStats(ElementaryStream stream)
    {
        ElementaryStreamEntity change = new ElementaryStreamEntity();
        change.setPid(stream.getStreamPid());
        change.setBitrate(stream.getBitrate());
        change.setRatio(stream.getRatio());
        change.setPacketCount(stream.getPacketCount());
        change.setPcrCount(stream.getPcrCount());
        change.setScrambled(stream.isScrambled());
        streamMapper.updateById(change);
    }

    @Override
    public void updateElementaryStreamUsage(int pid, String category, String description)
    {
        ElementaryStreamEntity change = new ElementaryStreamEntity();
        change.setPid(pid);
        change.setCategory(category);
        change.setDescription(description);
        streamMapper.updateById(change);
    }

    @Override
    public void updateElementaryStreamUsage(ElementaryStream stream)
    {
        ElementaryStreamEntity change = new ElementaryStreamEntity();
        change.setPid(stream.getStreamPid());
        change.setCategory(stream.getCategory());
        change.setDescription(stream.getDescription());
        streamMapper.updateById(change);
    }

    @Override
    public void accumulateElementaryStreamErrors(int pid, int transportErrors, int continuityErrors)
    {
        streamMapper.accumulateStreamErrors(pid, transportErrors, continuityErrors);
    }

    @Override
    public ElementaryStream getElementaryStream(int pid)
    {
        ElementaryStreamEntity entity = streamMapper.selectById(pid & 0x1FFF);
        return convert(entity);
    }

    @Override
    public List<ElementaryStream> listElementaryStreams(boolean presentOnly)
    {
        LambdaQueryWrapper<ElementaryStreamEntity> query = Wrappers.lambdaQuery(ElementaryStreamEntity.class)
                                                                   .gt(presentOnly, ElementaryStreamEntity::getPacketCount, 0)
                                                                   .orderByAsc(ElementaryStreamEntity::getPid);
        return streamMapper.selectList(query)
                           .stream()
                           .map(this::convert)
                           .toList();
    }

    @Override
    public MPEGProgram addMPEGProgram(int programNumber, int transportStreamId, int pmtPid)
    {
        MPEGProgramEntity entity = new MPEGProgramEntity();
        entity.setProgramNumber(programNumber);
        entity.setTransportStreamId(transportStreamId);
        entity.setPmtPid(pmtPid);
        entity.setPcrPid(8191);
        entity.setPmtVersion(0);
        entity.setFreeAccess(true);
        programMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateMPEGProgram(int programRef, int pcrPid, int pmtVersion, boolean freeAccess)
    {
        MPEGProgramEntity change = new MPEGProgramEntity();
        change.setId(programRef);
        change.setPcrPid(pcrPid);
        change.setPmtVersion(pmtVersion);
        change.setFreeAccess(freeAccess);
        programMapper.updateById(change);
    }

    @Override
    public void clearMPEGPrograms()
    {
        programMapper.delete(Wrappers.emptyWrapper());
    }

    @Override
    public void addProgramElementaryMapping(int programRef, int streamPid, int streamType)
    {
        ProgramElementaryMappingEntity entity = new ProgramElementaryMappingEntity();
        entity.setProgramRef(programRef);
        entity.setStreamPid(streamPid);
        entity.setStreamType(streamType);
        programMappingMapper.insert(entity);
    }

    @Override
    public List<MPEGProgram> listMPEGPrograms()
    {
        List<MPEGProgram> programs = programMapper.selectList(Wrappers.emptyWrapper())
                                                  .stream()
                                                  .map(this::convert)
                                                  .toList();
        for (MPEGProgram program : programs)
        {
            int bandwidth = 0;
            program.setEcmStreams(new ArrayList<>());
            program.setElementaryStreams(new ArrayList<>());

            List<Integer> streamPids = new ArrayList<>();
            streamPids.add(program.getPmtPid());

            Map<Integer, ProgramElementaryMappingEntity> esMappings = new HashMap<>();
            List<ProgramElementaryMappingEntity> elementaryMappings = programMappingMapper.selectList(Wrappers.lambdaQuery(ProgramElementaryMappingEntity.class)
                                                                                                              .eq(ProgramElementaryMappingEntity::getProgramRef,
                                                                                                                  program.getId()));
            for (ProgramElementaryMappingEntity elementaryMapping : elementaryMappings)
            {
                streamPids.add(elementaryMapping.getStreamPid());
                esMappings.put(elementaryMapping.getStreamPid(), elementaryMapping);
            }
            List<CAStreamEntity> ecmStreams = caStreamMapper.selectEcmStreams(program.getId());
            for (CAStreamEntity ecmStream : ecmStreams)
            {
                streamPids.add(ecmStream.getStreamPid());
                program.getEcmStreams().add(convert(ecmStream));
            }

            List<ElementaryStreamEntity> streams = streamMapper.selectBatchIds(streamPids);
            for (ElementaryStreamEntity stream : streams)
            {
                bandwidth += stream.getBitrate();
                ProgramElementaryMappingEntity mapping = esMappings.get(stream.getPid());
                if (mapping != null)
                {
                    ElementaryStream es = convert(stream);
                    es.setStreamType(mapping.getStreamType());
                    es.setProgramNumber(program.getProgramNumber());
                    program.getElementaryStreams().add(es);
                }
            }
            program.setBandwidth(bandwidth);
        }
        return programs;
    }

    @Override
    public CASystemStream addCASystemStream(int pid, int type, int systemId, byte[] privateData, int programRef, int elementaryStreamPid)
    {
        CAStreamEntity entity = new CAStreamEntity();
        entity.setSystemId(systemId);
        entity.setStreamPid(pid);
        entity.setStreamType(type);
        entity.setStreamPrivateData(privateData);
        entity.setProgramRef(programRef);
        entity.setElementaryStreamPid(elementaryStreamPid);
        caStreamMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public List<CASystemStream> listCASystemStreams()
    {
        return caStreamMapper.selectList(Wrappers.emptyWrapper())
                             .stream()
                             .map(this::convert)
                             .toList();
    }

    @Override
    public SIBouquet addSIBouquet(int bouquetId)
    {
        SIBouquetEntity entity = new SIBouquetEntity();
        entity.setBouquetId(bouquetId);
        entity.setBouquetName("未命名业务群");
        bouquetMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateSIBouquet(SIBouquet bouquet)
    {
        SIBouquetEntity change = new SIBouquetEntity();
        change.setId(bouquet.getId());
        change.setBouquetId(bouquet.getBouquetId());
        change.setBouquetName(bouquet.getName());
        bouquetMapper.updateById(change);
    }

    @Override
    public void addBouquetServiceMapping(int bouquetRef, int transportStreamId, int originalNetworkId, int serviceId)
    {
        BouquetServiceMappingEntity entity = new BouquetServiceMappingEntity();
        entity.setBouquetRef(bouquetRef);
        entity.setTransportStreamId(transportStreamId);
        entity.setOriginalNetworkId(originalNetworkId);
        entity.setServiceId(serviceId);
        bouquetMappingMapper.insert(entity);
    }

    @Override
    public List<SIBouquet> listSIBouquets()
    {
        List<SIBouquet> bouquets = new ArrayList<>();
        List<SIBouquetEntity> entities = bouquetMapper.selectList(Wrappers.emptyWrapper());
        for (SIBouquetEntity entity : entities)
        {
            SIBouquet bouquet = convert(entity);
            bouquet.setServices(new ArrayList<>());
            List<BouquetServiceMappingEntity> mappings = bouquetMappingMapper.selectList(Wrappers.lambdaQuery(BouquetServiceMappingEntity.class)
                                                                                                 .eq(BouquetServiceMappingEntity::getBouquetRef,
                                                                                                     entity.getId()));
            for (BouquetServiceMappingEntity mapping : mappings)
            {
                SIServiceLocator locator = new SIServiceLocator();
                locator.setTransportStreamId(mapping.getTransportStreamId());
                locator.setOriginalNetworkId(mapping.getOriginalNetworkId());
                locator.setServiceId(mapping.getServiceId());
                bouquet.getServices().add(locator);
            }
            bouquets.add(bouquet);
        }
        return bouquets;
    }

    @Override
    public SINetwork addSINetwork(int networkId, boolean actualNetwork)
    {
        SINetworkEntity entity = new SINetworkEntity();
        entity.setNetworkId(networkId);
        entity.setNetworkName("未命名网络");
        entity.setActualNetwork(actualNetwork);
        networkMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateSINetwork(SINetwork network)
    {
        SINetworkEntity change = new SINetworkEntity();
        change.setId(network.getId());
        change.setNetworkId(network.getNetworkId());
        change.setNetworkName(network.getName());
        networkMapper.updateById(change);
    }

    @Override
    public List<SINetwork> listSINetworks()
    {
        List<SINetwork> networks = new ArrayList<>();
        List<SINetworkEntity> entities = networkMapper.selectList(Wrappers.emptyWrapper());
        for (SINetworkEntity entity : entities)
        {
            SINetwork network = convert(entity);
            int multiplexCount = Math.toIntExact(multiplexMapper.selectCount(Wrappers.lambdaQuery(SIMultiplexEntity.class)
                                                                                     .eq(SIMultiplexEntity::getNetworkRef,
                                                                                         entity.getId())));
            network.setMultiplexCount(multiplexCount);
            networks.add(network);
        }
        return networks;
    }

    @Override
    public SIMultiplex addSIMultiplex(int networkRef, int transportStreamId, int originalNetworkId)
    {
        SIMultiplexEntity entity = new SIMultiplexEntity();
        entity.setNetworkRef(networkRef);
        entity.setTransportStreamId(transportStreamId);
        entity.setOriginalNetworkId(originalNetworkId);
        entity.setDeliveryType("未知");
        entity.setTransmitFrequency("");
        multiplexMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateSIMultiplex(SIMultiplex multiplex)
    {
        SIMultiplexEntity change = new SIMultiplexEntity();
        change.setId(multiplex.getId());
        change.setTransportStreamId(multiplex.getTransportStreamId());
        change.setOriginalNetworkId(multiplex.getOriginalNetworkId());
        change.setDeliveryType(multiplex.getDeliverySystemType());
        change.setTransmitFrequency(multiplex.getTransmitFrequency());
        multiplexMapper.updateById(change);
    }

    @Override
    public void addMultiplexServiceMapping(int multiplexRef, int serviceId)
    {
        MultiplexServiceMappingEntity entity = new MultiplexServiceMappingEntity();
        entity.setMultiplexRef(multiplexRef);
        entity.setServiceId(serviceId);
        multiplexMappingMapper.insert(entity);
    }

    @Override
    public List<SIMultiplex> listSIMultiplexes()
    {
        List<SIMultiplex> multiplexes = new ArrayList<>();
        List<SIMultiplexEntity> entities = multiplexMapper.selectList(Wrappers.emptyWrapper());
        for (SIMultiplexEntity entity : entities)
        {
            SIMultiplex multiplex = convert(entity);

            SINetworkEntity networkEntity = networkMapper.selectById(entity.getNetworkRef());
            multiplex.setNetworkId(networkEntity.getNetworkId());
            multiplex.setNetworkName(networkEntity.getNetworkName());

            multiplex.setServices(new ArrayList<>());
            List<Integer> serviceIds = multiplexMappingMapper.selectObjs(Wrappers.lambdaQuery(MultiplexServiceMappingEntity.class)
                                                                                 .select(MultiplexServiceMappingEntity::getServiceId)
                                                                                 .eq(MultiplexServiceMappingEntity::getMultiplexRef,
                                                                                     entity.getId()));
            for (Integer serviceId : serviceIds)
            {
                SIServiceLocator locator = new SIServiceLocator();
                locator.setTransportStreamId(multiplex.getTransportStreamId());
                locator.setOriginalNetworkId(multiplex.getOriginalNetworkId());
                locator.setServiceId(serviceId);
                multiplex.getServices().add(locator);
            }
            multiplexes.add(multiplex);
        }
        return multiplexes;
    }

    @Override
    public SIService addSIService(int serviceId, int transportStreamId, int originalNetworkId)
    {
        SIServiceEntity entity = new SIServiceEntity();
        entity.setServiceId(serviceId);
        entity.setTransportStreamId(transportStreamId);
        entity.setOriginalNetworkId(originalNetworkId);
        entity.setServiceName("未命名业务");
        entity.setServiceProvider("未知提供商");
        entity.setServiceType(1);
        entity.setRunningStatus(0);
        entity.setReferenceServiceId(-1);
        entity.setFreeAccess(true);
        entity.setScheduleEITEnabled(true);
        entity.setPresentFollowingEITEnabled(true);
        entity.setNvodReferenceService(false);
        entity.setActualTransportStream(true);
        serviceMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateSIService(SIService service)
    {
        SIServiceEntity change = new SIServiceEntity();
        change.setId(service.getId());
        change.setServiceId(service.getServiceId());
        change.setTransportStreamId(service.getTransportStreamId());
        change.setOriginalNetworkId(service.getOriginalNetworkId());
        change.setServiceType(service.getServiceType());
        change.setServiceName(service.getName());
        change.setServiceProvider(service.getProvider());
        change.setRunningStatus(service.getRunningStatus());
        change.setFreeAccess(service.isFreeAccess());
        change.setActualTransportStream(service.isActualTransportStream());
        change.setPresentFollowingEITEnabled(service.isPresentFollowingEITEnabled());
        change.setScheduleEITEnabled(service.isScheduleEITEnabled());
        change.setReferenceServiceId(service.getReferenceServiceId());
        change.setNvodReferenceService(service.isNVODReferenceService());
        change.setNvodTimeShiftedService(service.isNVODTimeShiftedService());
        serviceMapper.updateById(change);
    }

    @Override
    public List<SIService> listSIServices()
    {
        return serviceMapper.selectList(Wrappers.emptyWrapper())
                            .stream()
                            .map(this::convert)
                            .toList();
    }

    @Override
    public SIEvent addSIEvent(int eventId, int transportStreamId, int originalNetworkId, int serviceId)
    {
        SIEventEntity entity = new SIEventEntity();
        entity.setEventId(eventId);
        entity.setTransportStreamId(transportStreamId);
        entity.setOriginalNetworkId(originalNetworkId);
        entity.setServiceId(serviceId);
        entity.setDuration(0);
        entity.setRunningStatus(0);
        entity.setFreeAccess(true);
        entity.setLanguageCode("chi");
        entity.setEventName("未命名事件");
        entity.setEventDescription("");
        entity.setReferenceEventId(-1);
        entity.setReferenceServiceId(-1);
        entity.setNvodTimeShiftedEvent(false);
        entity.setPresentEvent(false);
        entity.setScheduleEvent(true);
        eventMapper.insert(entity);
        return convert(entity);
    }

    @Override
    public void updateSIEvent(SIEvent event)
    {
        SIEventEntity change = new SIEventEntity();
        change.setId(event.getId());
        change.setTransportStreamId(event.getTransportStreamId());
        change.setOriginalNetworkId(event.getOriginalNetworkId());
        change.setServiceId(event.getServiceId());
        change.setEventId(event.getEventId());
        change.setEventName(event.getTitle());
        change.setEventDescription(event.getDescription());
        change.setLanguageCode(event.getLanguageCode());
        change.setRunningStatus(event.getRunningStatus());
        change.setDuration(event.getDuration());
        change.setFreeAccess(event.isFreeAccess());
        change.setPresentEvent(event.isPresentEvent());
        change.setScheduleEvent(event.isScheduleEvent());
        change.setNvodTimeShiftedEvent(event.isNvodTimeShiftedEvent());
        change.setReferenceServiceId(event.getReferenceServiceId());
        change.setReferenceEventId(event.getReferenceEventId());

        if (event.getStartTime() != null)
        {
            LocalDateTime utcStartTime = event.getStartTime()
                                              .atZoneSameInstant(ZoneId.of("UTC"))
                                              .toLocalDateTime();
            change.setStartTime(utcStartTime);
        }

        eventMapper.updateById(change);
    }

    @Override
    public List<SIEvent> listSIEvents(int transportStreamId, int originalNetworkId, int serviceId,
                                      boolean presentOnly, boolean scheduleOnly,
                                      OffsetDateTime timeFilterBegin, OffsetDateTime timeFilterEnd)
    {
        return List.of();
    }

    @Override
    public void addTimestamp(OffsetDateTime timestamp)
    {
        SIDateTimeEntity entity = new SIDateTimeEntity();
        entity.setTimepoint(timestamp.atZoneSameInstant(ZoneId.of("UTC"))
                                     .toLocalDateTime());
        datetimeMapper.insert(entity);
    }

    @Override
    public OffsetDateTime getLastTimestamp()
    {
        LambdaQueryWrapper<SIDateTimeEntity> query = Wrappers.lambdaQuery(SIDateTimeEntity.class)
                                                             .select(SIDateTimeEntity::getTimepoint)
                                                             .orderByDesc(SIDateTimeEntity::getId)
                                                             .last("limit 1");
        List<LocalDateTime> times = datetimeMapper.selectObjs(query);
        return times.isEmpty() ? null
                               : times.getFirst().atOffset(ZoneOffset.UTC);
    }

    @Override
    public void addTR290Event(TR290Event event)
    {
        TR290EventEntity entity = new TR290EventEntity();
        entity.setTimestamp(event.getTimestamp()
                                 .atZoneSameInstant(ZoneId.systemDefault())
                                 .toLocalDateTime());
        entity.setType(event.getType());
        entity.setDescription(event.getDescription());
        entity.setStream(event.getStream());
        entity.setPosition(event.getPosition());
        tr290EventMapper.insert(entity);
    }

    @Override
    public List<TR290Event> listTR290Events()
    {
        return tr290EventMapper.selectList(Wrappers.emptyWrapper())
                               .stream()
                               .map(this::convert)
                               .toList();
    }

    @Override
    public List<TR290Stats> listTR290Stats()
    {
        return List.of();
    }

    @Override
    public void addPCR(PCR pcr)
    {

    }

    @Override
    public void addPCRCheck(PCRCheck check)
    {

    }

    @Override
    public List<PCRStats> listPCRStats()
    {
        return List.of();
    }

    @Override
    public List<PCRCheck> getRecentPCRChecks(int pid, int limit)
    {
        return List.of();
    }

    @Override
    public void addPrivateSection(String tag, int pid, long position, byte[] encoding)
    {
        PrivateSectionEntity entity = new PrivateSectionEntity();
        entity.setTag(tag);
        entity.setStream(pid);
        entity.setPosition(position);
        entity.setEncoding(encoding);
        sectionMapper.insert(entity);
    }

    @Override
    public void removePrivateSections(String tag, int pid)
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .eq(PrivateSectionEntity::getTag, tag)
                                                                 .eq(PrivateSectionEntity::getStream, pid);
        sectionMapper.delete(query);
    }

    @Override
    public List<PrivateSection> getPrivateSections(int pid, int count)
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .eq(PrivateSectionEntity::getStream, pid)
                                                                 .orderByAsc(PrivateSectionEntity::getPosition)
                                                                 .last("limit " + count);
        return sectionMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .toList();
    }

    @Override
    public List<PrivateSection> getPrivateSections(String tag, int pid, int count)
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .eq(PrivateSectionEntity::getTag, tag)
                                                                 .eq(PrivateSectionEntity::getStream, pid)
                                                                 .orderByAsc(PrivateSectionEntity::getPosition)
                                                                 .last("limit " + count);
        return sectionMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .toList();
    }

    @Override
    public Map<Integer, List<PrivateSection>> getPrivateSectionGroups(String tag)
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .eq(PrivateSectionEntity::getTag, tag)
                                                                 .orderByAsc(PrivateSectionEntity::getPosition)
                                                                 .last("limit 1000");
        return sectionMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.groupingBy(PrivateSection::getPid));
    }

    @Override
    public void addTransportPacket(String tag, int pid, long position, byte[] encoding)
    {
        TransportPacketEntity entity = new TransportPacketEntity();
        entity.setTag(tag);
        entity.setStream(pid);
        entity.setPosition(position);
        entity.setEncoding(encoding);
        packetMapper.insert(entity);
    }

    @Override
    public List<TransportPacket> getTransportPackets(String tag, int pid, int count)
    {
        LambdaQueryWrapper<TransportPacketEntity> query = Wrappers.lambdaQuery(TransportPacketEntity.class)
                                                                  .eq(TransportPacketEntity::getStream, pid)
                                                                  .last("limit " + count);
        return packetMapper.selectList(query)
                           .stream()
                           .map(this::convert)
                           .toList();
    }

    @Override
    public void addTableVersion(TableVersion version)
    {
        TableVersionEntity entity = new TableVersionEntity();
        entity.setTableId(version.getTableId());
        entity.setTableIdExtension(version.getTableIdExtension());
        entity.setVersion(version.getVersion());
        entity.setStream(version.getStream());
        entity.setPosition(version.getPosition());
        entity.setTag(version.getTag());
        tableVersionMapper.insert(entity);
    }

    @Override
    public List<TableVersion> listTableVersions()
    {
        return tableVersionMapper.selectList(Wrappers.emptyWrapper())
                                 .stream()
                                 .map(this::convert)
                                 .toList();
    }

    @Override
    public void addFilteringHook(FilteringHook hook)
    {

    }

    @Override
    public void removeFilteringHook(long hookRef)
    {

    }

    @Override
    public List<FilteringHook> listFilteringHooks()
    {
        return List.of();
    }

    private StreamSource convert(StreamSourceEntity entity)
    {
        StreamSource source = new StreamSource();
        source.setId(entity.getId());
        source.setName(entity.getSourceName());
        source.setUri(entity.getSourceUri());
        source.setBitrate(entity.getBitrate());
        source.setFrameSize(entity.getFrameSize());
        source.setPacketCount(entity.getPacketCount());
        source.setTransportStreamId(entity.getTransportStreamId());
        return source;
    }

    private ElementaryStream convert(ElementaryStreamEntity entity)
    {
        ElementaryStream stream = new ElementaryStream();
        stream.setStreamPid(entity.getPid());
        stream.setBitrate(entity.getBitrate());
        stream.setRatio(entity.getRatio());
        stream.setLastPct(entity.getLastPct());
        stream.setPacketCount(entity.getPacketCount());
        stream.setPcrCount(entity.getPcrCount());
        stream.setContinuityErrorCount(entity.getContinuityErrorCount());
        stream.setTransportErrorCount(entity.getTransportErrorCount());
        stream.setCategory(entity.getCategory());
        stream.setDescription(entity.getDescription());
        stream.setScrambled(entity.getScrambled());
        stream.setStreamType(-1);
        stream.setProgramNumber(-1);
        return stream;
    }

    private MPEGProgram convert(MPEGProgramEntity entity)
    {
        MPEGProgram program = new MPEGProgram();
        program.setId(entity.getId());
        program.setProgramNumber(entity.getProgramNumber());
        program.setTransportStreamId(entity.getTransportStreamId());
        program.setPmtPid(entity.getPmtPid());
        program.setPmtVersion(entity.getPmtVersion());
        program.setFreeAccess(entity.getFreeAccess());
        return program;
    }

    private CASystemStream convert(CAStreamEntity entity)
    {
        CASystemStream stream = new CASystemStream();
        stream.setId(entity.getId());
        stream.setSystemId(entity.getSystemId());
        stream.setStreamPid(entity.getStreamPid());
        stream.setStreamType(entity.getStreamType());
        stream.setStreamPrivateData(entity.getStreamPrivateData());
        stream.setProgramNumber(entity.getProgramRef());
        stream.setElementaryStreamPid(entity.getElementaryStreamPid());
        return stream;
    }

    private SIBouquet convert(SIBouquetEntity entity)
    {
        SIBouquet bouquet = new SIBouquet();
        bouquet.setId(entity.getId());
        bouquet.setBouquetId(entity.getBouquetId());
        bouquet.setName(entity.getBouquetName());
        return bouquet;
    }

    private SINetwork convert(SINetworkEntity entity)
    {
        SINetwork network = new SINetwork();
        network.setId(entity.getId());
        network.setNetworkId(entity.getNetworkId());
        network.setName(entity.getNetworkName());
        network.setActualNetwork(entity.getActualNetwork());
        return network;
    }

    private SIMultiplex convert(SIMultiplexEntity entity)
    {
        SIMultiplex multiplex = new SIMultiplex();
        multiplex.setId(entity.getId());
        multiplex.setTransportStreamId(entity.getTransportStreamId());
        multiplex.setOriginalNetworkId(entity.getOriginalNetworkId());
        return multiplex;
    }

    private SIService convert(SIServiceEntity entity)
    {
        SIService service = new SIService();
        service.setId(entity.getId());
        service.setName(entity.getServiceName());
        service.setProvider(entity.getServiceProvider());
        service.setTransportStreamId(entity.getTransportStreamId());
        service.setOriginalNetworkId(entity.getOriginalNetworkId());
        service.setServiceId(entity.getServiceId());
        service.setServiceType(entity.getServiceType());
        service.setServiceTypeName(ServiceTypes.name(entity.getServiceType()));
        service.setRunningStatus(entity.getRunningStatus());
        service.setRunningStatusName(RunningStatus.name(entity.getRunningStatus()));
        service.setFreeAccess(entity.getFreeAccess());
        service.setPresentFollowingEITEnabled(entity.getPresentFollowingEITEnabled());
        service.setScheduleEITEnabled(entity.getScheduleEITEnabled());
        service.setReferenceServiceId(entity.getReferenceServiceId());
        return service;
    }

    private SIEvent convert(SIEventEntity entity)
    {
        SIEvent event = new SIEvent();
        event.setId(entity.getId());
        event.setTitle(entity.getEventName());
        event.setDescription(entity.getEventDescription());
        event.setEventId(entity.getEventId());
        event.setTransportStreamId(entity.getTransportStreamId());
        event.setOriginalNetworkId(entity.getOriginalNetworkId());
        event.setServiceId(entity.getServiceId());
        event.setDuration(entity.getDuration());
        event.setRunningStatus(entity.getRunningStatus());
        event.setFreeAccess(entity.getFreeAccess());
        event.setPresentEvent(entity.getPresentEvent());
        event.setScheduleEvent(entity.getScheduleEvent());

        if (entity.getStartTime() != null)
        {
            event.setStartTime(entity.getStartTime().atOffset(ZoneOffset.UTC));
        }

        event.setNvodTimeShiftedEvent(entity.getNvodTimeShiftedEvent());
        event.setReferenceServiceId(entity.getReferenceServiceId());
        event.setReferenceEventId(entity.getReferenceEventId());
        return event;
    }

    private TR290Event convert(TR290EventEntity entity)
    {
        TR290Event event = new TR290Event();
        event.setType(entity.getType());
        event.setDescription(entity.getDescription());
        event.setStream(entity.getStream());
        event.setPosition(entity.getPosition());
        event.setTimestamp(entity.getTimestamp()
                                 .atZone(ZoneId.systemDefault())
                                 .toOffsetDateTime());
        return event;
    }

    private PrivateSection convert(PrivateSectionEntity entity)
    {
        PrivateSection section = new PrivateSection();
        section.setTag(entity.getTag());
        section.setPid(entity.getStream());
        section.setPosition(entity.getPosition());
        section.setEncoding(entity.getEncoding());
        return section;
    }

    private TransportPacket convert(TransportPacketEntity entity)
    {
        TransportPacket packet = new TransportPacket();
        packet.setTag(entity.getTag());
        packet.setPid(entity.getStream());
        packet.setPosition(entity.getPosition());
        packet.setEncoding(entity.getEncoding());
        return packet;
    }

    private TableVersion convert(TableVersionEntity entity)
    {
        TableVersion version = new TableVersion();
        version.setTableId(entity.getTableId());
        version.setTableIdExtension(entity.getTableIdExtension());
        version.setVersion(entity.getVersion());
        version.setStream(entity.getStream());
        version.setPosition(entity.getPosition());
        version.setTag(entity.getTag());
        return version;
    }
}
