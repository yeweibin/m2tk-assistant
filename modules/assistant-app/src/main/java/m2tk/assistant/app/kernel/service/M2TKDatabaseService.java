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

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.api.M2TKDatabase;
import m2tk.assistant.api.domain.*;
import m2tk.assistant.api.presets.RunningStatus;
import m2tk.assistant.api.presets.ServiceTypes;
import m2tk.assistant.app.kernel.ErrorCode;
import m2tk.assistant.app.kernel.KernelException;
import m2tk.assistant.app.kernel.entity.*;
import m2tk.assistant.app.kernel.mapper.*;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.ResourceUtil;
import org.noear.solon.data.sql.RowIterator;
import org.noear.solon.data.sql.SqlUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class M2TKDatabaseService implements M2TKDatabase
{
    @Inject("m2tk")
    private SqlUtils sqlUtils;

    @Db("m2tk")
    private PreferenceEntityMapper preferenceMapper;
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
    @Db("m2tk")
    private SINetworkViewEntityMapper networkViewMapper;
    @Db("m2tk")
    private SIMultiplexViewEntityMapper multiplexViewMapper;
    @Db("m2tk")
    private FilteringHookEntityMapper hookMapper;
    @Db("m2tk")
    private DensityBulkEntityMapper densityMapper;
    @Db("m2tk")
    private DensityStatViewEntityMapper densityStatMapper;

    @Init
    public void initDatabase()
    {
        try
        {
            log.info("准备初始化数据库");
            String initScript = ResourceUtil.getResourceAsString("/db_init.sql");
            List<String> statements = StrUtil.split(initScript, ";", true, true);
            for (String statement : statements)
                sqlUtils.sql(statement).update();
            log.info("数据库初始化完毕");
        } catch (Exception ex)
        {
            log.error("无法初始化数据库：{}", ex.getMessage(), ex);
            throw new KernelException(ErrorCode.DATABASE_ERROR, "无法初始化数据库");
        }
    }

    @Override
    public void setPreference(String key, String value)
    {
        Objects.requireNonNull(key);
        PreferenceEntity entity = new PreferenceEntity();
        entity.setName(key);
        entity.setValue(StrUtil.nullToEmpty(value));
        if (preferenceMapper.updateById(entity) == 0)
            preferenceMapper.insert(entity);
    }

    @Override
    public String getPreference(String key, String defaultValue)
    {
        Objects.requireNonNull(key);
        PreferenceEntity entity = preferenceMapper.selectById(key);
        return (entity == null || entity.getValue().isEmpty())
               ? defaultValue
               : entity.getValue();
    }

    @Override
    public List<String> listPreferenceKeys()
    {
        return preferenceMapper.selectObjs(Wrappers.lambdaQuery(PreferenceEntity.class)
                                                   .select(PreferenceEntity::getName)
                                                   .orderByAsc(PreferenceEntity::getName));
    }

    @Override
    public StreamSource beginDiagnosis(String sourceName, String sourceUri)
    {
        try
        {
            log.info("开始设置分析上下文");

            String resetScript = ResourceUtil.getResourceAsString("/db_reset.sql");
            List<String> statements = StrUtil.split(resetScript, ";", true, true);
            for (String statement : statements)
                sqlUtils.sql(statement).update();
            log.info("清空历史数据");

            StreamSourceEntity entity = new StreamSourceEntity();
            entity.setSourceName(sourceName);
            entity.setSourceUri(sourceUri);
            entity.setFrameSize(188);
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
    public void updateStreamSourceStats(int sourceRef, int bitrate, int frameSize, boolean scrambled, long packetCount, int streamCount)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(sourceRef);
        change.setBitrate(bitrate);
        change.setFrameSize(frameSize);
        change.setScrambled(scrambled);
        change.setPacketCount(packetCount);
        change.setStreamCount(streamCount);
        sourceMapper.updateById(change);
    }

    @Override
    public void updateStreamSourceTransportId(int sourceRef, int transportStreamId)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(sourceRef);
        change.setTransportStreamId(transportStreamId);
        sourceMapper.updateById(change);
    }

    @Override
    public void updateStreamSourceComponentPresence(int sourceRef, String component, boolean present)
    {
        StreamSourceEntity change = new StreamSourceEntity();
        change.setId(sourceRef);
        switch (component)
        {
            case "ECM" -> change.setEcmPresent(present);
            case "EMM" -> change.setEmmPresent(present);
            case "PAT" -> change.setPatPresent(present);
            case "PMT" -> change.setPmtPresent(present);
            case "CAT" -> change.setCatPresent(present);
            case "NIT_Actual" -> change.setNitActualPresent(present);
            case "NIT_Other" -> change.setNitOtherPresent(present);
            case "SDT_Actual" -> change.setSdtActualPresent(present);
            case "SDT_Other" -> change.setSdtOtherPresent(present);
            case "EIT_PF_Actual" -> change.setEitPnfActualPresent(present);
            case "EIT_PF_Other" -> change.setEitPnfOtherPresent(present);
            case "EIT_Schedule_Actual" -> change.setEitSchActualPresent(present);
            case "EIT_Schedule_Other" -> change.setEitSchOtherPresent(present);
            case "BAT" -> change.setBatPresent(present);
            case "TDT" -> change.setTdtPresent(present);
            case "TOT" -> change.setTotPresent(present);
        }
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
    public List<String> listStreamSourceUris()
    {
        LambdaQueryWrapper<StreamSourceEntity> query = Wrappers.lambdaQuery(StreamSourceEntity.class)
                                                               .select(StreamSourceEntity::getSourceUri)
                                                               .orderByAsc(StreamSourceEntity::getId);
        return sourceMapper.selectObjs(query);
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
                           .collect(Collectors.toList());
    }

    @Override
    public MPEGProgram addMPEGProgram(int programNumber, int transportStreamId, int pmtPid)
    {
        MPEGProgramEntity entity = new MPEGProgramEntity();
        entity.setProgramNumber(programNumber);
        entity.setTransportStreamId(transportStreamId);
        entity.setPmtPid(pmtPid);
        entity.setPcrPid(8191);
        entity.setFreeAccess(Boolean.TRUE);
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
                                                  .collect(Collectors.toList());
        for (MPEGProgram program : programs)
        {
            int bandwidth = 0;
            program.setEcmStreams(new ArrayList<>());
            program.setElementaryStreams(new ArrayList<>());

            List<Integer> streamPids = new ArrayList<>();
            streamPids.add(program.getPmtPid());

            Map<Integer, ProgramElementaryMappingEntity> esMappings = new HashMap<>();
            List<ProgramElementaryMappingEntity> elementaryMappings = programMappingMapper.selectList(Wrappers.lambdaQuery(ProgramElementaryMappingEntity.class)
                                                                                                              .eq(ProgramElementaryMappingEntity::getProgramRef, program.getId()));
            for (ProgramElementaryMappingEntity elementaryMapping : elementaryMappings)
            {
                streamPids.add(elementaryMapping.getStreamPid());
                esMappings.put(elementaryMapping.getStreamPid(), elementaryMapping);
            }

            List<CAStreamEntity> ecmStreams = caStreamMapper.selectList(Wrappers.lambdaQuery(CAStreamEntity.class)
                                                                                .eq(CAStreamEntity::getProgramRef, program.getId())
                                                                                .eq(CAStreamEntity::getStreamType, CAStreamEntity.TYPE_ECM));
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
    public void addCASystemStream(int pid, int type, int systemId, byte[] privateData, int programRef, int programNumber, int elementaryStreamPid)
    {
        CAStreamEntity entity = new CAStreamEntity();
        entity.setSystemId(systemId);
        entity.setStreamPid(pid);
        entity.setStreamType(type);
        entity.setStreamPrivateData(privateData);
        entity.setProgramRef(programRef);
        entity.setProgramNumber(programNumber);
        entity.setElementaryStreamPid(elementaryStreamPid);
        caStreamMapper.insert(entity);
    }

    @Override
    public List<CASystemStream> listCASystemStreams()
    {
        return caStreamMapper.selectList(Wrappers.emptyWrapper())
                             .stream()
                             .map(this::convert)
                             .collect(Collectors.toList());
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
        return networkViewMapper.selectList(Wrappers.emptyWrapper())
                                .stream()
                                .map(this::convert)
                                .collect(Collectors.toList());
    }

    @Override
    public SINetwork getCurrenetSINetwork()
    {
        LambdaQueryWrapper<SINetworkViewEntity> query = Wrappers.lambdaQuery(SINetworkViewEntity.class)
                                                                .eq(SINetworkViewEntity::getActualNetwork, true)
                                                                .orderByDesc(SINetworkViewEntity::getId)
                                                                .last("limit 1");
        SINetworkViewEntity entity = networkViewMapper.selectOne(query);
        return (entity == null) ? null : convert(entity);
    }

    @Override
    public List<SINetwork> getOtherSINetworks()
    {
        LambdaQueryWrapper<SINetworkViewEntity> query = Wrappers.lambdaQuery(SINetworkViewEntity.class)
                                                                .eq(SINetworkViewEntity::getActualNetwork, false);
        return networkViewMapper.selectList(query)
                                .stream()
                                .map(this::convert)
                                .collect(Collectors.toList());
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
        List<SIMultiplexViewEntity> entities = multiplexViewMapper.selectList(Wrappers.emptyWrapper());
        for (SIMultiplexViewEntity entity : entities)
        {
            SIMultiplex multiplex = convert(entity);

            multiplex.setServices(new ArrayList<>());
            LambdaQueryWrapper<MultiplexServiceMappingEntity> query =
                Wrappers.lambdaQuery(MultiplexServiceMappingEntity.class)
                        .select(MultiplexServiceMappingEntity::getServiceId)
                        .eq(MultiplexServiceMappingEntity::getMultiplexRef, entity.getId());
            List<Integer> serviceIds = multiplexMappingMapper.selectObjs(query);
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
    public List<SIMultiplex> getActualNetworkMultiplexes()
    {
        List<SIMultiplex> multiplexes = new ArrayList<>();
        List<SIMultiplexViewEntity> entities = multiplexViewMapper.selectList(Wrappers.lambdaQuery(SIMultiplexViewEntity.class)
                                                                                      .eq(SIMultiplexViewEntity::getActualNetwork, true));
        for (SIMultiplexViewEntity entity : entities)
        {
            SIMultiplex multiplex = convert(entity);

            multiplex.setServices(new ArrayList<>());
            LambdaQueryWrapper<MultiplexServiceMappingEntity> query =
                Wrappers.lambdaQuery(MultiplexServiceMappingEntity.class)
                        .select(MultiplexServiceMappingEntity::getServiceId)
                        .eq(MultiplexServiceMappingEntity::getMultiplexRef, entity.getId());
            List<Integer> serviceIds = multiplexMappingMapper.selectObjs(query);
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
    public List<SIMultiplex> getOtherNetworkMultiplexes()
    {
        List<SIMultiplex> multiplexes = new ArrayList<>();
        List<SIMultiplexViewEntity> entities = multiplexViewMapper.selectList(Wrappers.lambdaQuery(SIMultiplexViewEntity.class)
                                                                                      .eq(SIMultiplexViewEntity::getActualNetwork, false));
        for (SIMultiplexViewEntity entity : entities)
        {
            SIMultiplex multiplex = convert(entity);

            multiplex.setServices(new ArrayList<>());
            LambdaQueryWrapper<MultiplexServiceMappingEntity> query =
                Wrappers.lambdaQuery(MultiplexServiceMappingEntity.class)
                        .select(MultiplexServiceMappingEntity::getServiceId)
                        .eq(MultiplexServiceMappingEntity::getMultiplexRef, entity.getId());
            List<Integer> serviceIds = multiplexMappingMapper.selectObjs(query);
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
    public SIService addSIService(int serviceId, int transportStreamId, int originalNetworkId, boolean actualTransportStream)
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
        entity.setActualTransportStream(actualTransportStream);
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
    public List<SIService> listRegularSIServices()
    {
        LambdaQueryWrapper<SIServiceEntity> query =
            Wrappers.lambdaQuery(SIServiceEntity.class)
                    .ne(SIServiceEntity::getServiceType, 0x04)
                    .ne(SIServiceEntity::getServiceType, 0x05)
                    .orderByAsc(SIServiceEntity::getTransportStreamId)
                    .orderByAsc(SIServiceEntity::getServiceId);
        return serviceMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public List<SIService> listNVODSIServices()
    {
        LambdaQueryWrapper<SIServiceEntity> query =
            Wrappers.lambdaQuery(SIServiceEntity.class)
                    .nested(q ->
                                q.eq(SIServiceEntity::getServiceType, 0x04)
                                 .or()
                                 .eq(SIServiceEntity::getServiceType, 0x05))
                    .orderByAsc(SIServiceEntity::getTransportStreamId)
                    .orderByAsc(SIServiceEntity::getServiceId);
        return serviceMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public List<SIService> getActualTransportStreamServices()
    {
        LambdaQueryWrapper<SIServiceEntity> query =
            Wrappers.lambdaQuery(SIServiceEntity.class)
                    .eq(SIServiceEntity::getActualTransportStream, true)
                    .orderByAsc(SIServiceEntity::getTransportStreamId)
                    .orderByAsc(SIServiceEntity::getServiceId);
        return serviceMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public List<SIService> getOtherTransportStreamServices()
    {
        LambdaQueryWrapper<SIServiceEntity> query =
            Wrappers.lambdaQuery(SIServiceEntity.class)
                    .eq(SIServiceEntity::getActualTransportStream, false)
                    .orderByAsc(SIServiceEntity::getTransportStreamId)
                    .orderByAsc(SIServiceEntity::getServiceId);
        return serviceMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public SIEvent addSIEvent(int eventId, int transportStreamId, int originalNetworkId, int serviceId)
    {
        SIEventEntity entity = new SIEventEntity();
        entity.setEventId(eventId);
        entity.setTransportStreamId(transportStreamId);
        entity.setOriginalNetworkId(originalNetworkId);
        entity.setServiceId(serviceId);
        entity.setFreeAccess(true);
        entity.setLanguageCode("chi");
        entity.setEventName("未命名事件");
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
                                              .atZoneSameInstant(ZoneOffset.UTC)
                                              .toLocalDateTime();
            change.setStartTime(utcStartTime);
        }

        eventMapper.updateById(change);
    }

    @Override
    public List<SIEvent> listRegularSIEvents(int transportStreamId, int originalNetworkId, int serviceId,
                                             boolean presentOnly, boolean scheduleOnly,
                                             OffsetDateTime timeFilterBegin, OffsetDateTime timeFilterEnd)
    {
        LambdaQueryWrapper<SIEventEntity> query = Wrappers.lambdaQuery(SIEventEntity.class)
                                                          .eq(SIEventEntity::getTransportStreamId, transportStreamId)
                                                          .eq(SIEventEntity::getOriginalNetworkId, originalNetworkId)
                                                          .eq(SIEventEntity::getServiceId, serviceId)
                                                          .eq(SIEventEntity::getNvodTimeShiftedEvent, Boolean.FALSE)
                                                          .eq(presentOnly, SIEventEntity::getPresentEvent, Boolean.TRUE)
                                                          .eq(scheduleOnly, SIEventEntity::getScheduleEvent, Boolean.TRUE);
        if (timeFilterBegin != null)
            query.ge(SIEventEntity::getStartTime,
                     timeFilterBegin.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        if (timeFilterEnd != null)
            query.le(SIEventEntity::getStartTime,
                     timeFilterEnd.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

        return eventMapper.selectList(query)
                          .stream()
                          .map(this::convert)
                          .collect(Collectors.toList());
    }

    @Override
    public List<SIEvent> listNVODSIEvents(int transportStreamId, int originalNetworkId, int serviceId,
                                          boolean presentOnly, boolean scheduleOnly,
                                          OffsetDateTime timeFilterBegin, OffsetDateTime timeFilterEnd)
    {
        LambdaQueryWrapper<SIEventEntity> query = Wrappers.lambdaQuery(SIEventEntity.class)
                                                          .eq(SIEventEntity::getTransportStreamId, transportStreamId)
                                                          .eq(SIEventEntity::getOriginalNetworkId, originalNetworkId)
                                                          .eq(SIEventEntity::getServiceId, serviceId)
                                                          .eq(SIEventEntity::getNvodTimeShiftedEvent, Boolean.TRUE)
                                                          .eq(presentOnly, SIEventEntity::getPresentEvent, Boolean.TRUE)
                                                          .eq(scheduleOnly, SIEventEntity::getScheduleEvent, Boolean.TRUE);
        if (timeFilterBegin != null)
            query.ge(SIEventEntity::getStartTime,
                     timeFilterBegin.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        if (timeFilterEnd != null)
            query.le(SIEventEntity::getStartTime,
                     timeFilterEnd.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

        return eventMapper.selectList(query)
                          .stream()
                          .map(this::convert)
                          .collect(Collectors.toList());
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
                                                             .orderByDesc(SIDateTimeEntity::getId)
                                                             .last("limit 1");
        SIDateTimeEntity entity = datetimeMapper.selectOne(query);
        return (entity == null) ? null
                                : entity.getTimepoint().atOffset(ZoneOffset.UTC);
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
    public void clearTR290Events()
    {
        tr290EventMapper.delete(Wrappers.emptyWrapper());
    }

    @Override
    public List<TR290Event> listTR290Events(String type, int count)
    {
        return tr290EventMapper.selectList(Wrappers.lambdaQuery(TR290EventEntity.class)
                                                   .eq(TR290EventEntity::getType, type)
                                                   .orderByDesc(TR290EventEntity::getId)
                                                   .last("limit " + Math.min(count, 100)))
                               .stream()
                               .map(this::convert)
                               .collect(Collectors.toList());
    }

    @Override
    public List<TR290Event> listTR290Events()
    {
        return tr290EventMapper.selectList(Wrappers.emptyWrapper())
                               .stream()
                               .map(this::convert)
                               .collect(Collectors.toList());
    }

    @Override
    public TR290Stats getTR290Stats()
    {
        TR290Stats stats = new TR290Stats();
        List<TR290StatViewEntity> entities = tr290StatMapper.selectList(Wrappers.emptyWrapper());
        for (TR290StatViewEntity entity : entities)
        {
            TR290Event event = new TR290Event();
            event.setType(entity.getType());
            event.setDescription(entity.getDescription());
            event.setStream(entity.getPid());
            event.setPosition(entity.getPosition());
            event.setTimestamp(entity.getTimepoint()
                                     .atZone(ZoneId.systemDefault())
                                     .toOffsetDateTime());

            stats.setStat(entity.getType(), entity.getCount(), event);
        }
        return stats;
    }

    @Override
    public void addPCR(PCR pcr)
    {
        PCREntity entity = new PCREntity();
        entity.setPid(pcr.getPid());
        entity.setPosition(pcr.getPosition());
        entity.setValue(pcr.getValue());
        pcrMapper.insert(entity);
    }

    @Override
    public void addPCRCheck(PCRCheck check)
    {
        PCRCheckEntity entity = new PCRCheckEntity();
        entity.setPid(check.getPid());
        entity.setPreviousValue(check.getPrevValue());
        entity.setPreviousPosition(check.getPrevPosition());
        entity.setCurrentValue(check.getCurrValue());
        entity.setCurrentPosition(check.getCurrPosition());
        entity.setBitrate(check.getBitrate());
        entity.setIntervalNanos(check.getIntervalNanos());
        entity.setDiffNanos(check.getDiffNanos());
        entity.setAccuracyNanos(check.getAccuracyNanos());
        entity.setRepetitionCheckFailed(check.isRepetitionCheckFailed());
        entity.setDiscontinuityCheckFailed(check.isDiscontinuityCheckFailed());
        entity.setAccuracyCheckFailed(check.isAccuracyCheckFailed());
        pcrCheckMapper.insert(entity);
    }

    @Override
    public List<PCRStats> listPCRStats()
    {
        return pcrStatMapper.selectList(Wrappers.emptyWrapper())
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public List<PCRCheck> getRecentPCRChecks(int pid, int limit)
    {
        return pcrCheckMapper.selectList(Wrappers.lambdaQuery(PCRCheckEntity.class)
                                                 .eq(PCRCheckEntity::getPid, pid)
                                                 .last("limit " + limit))
                             .stream()
                             .map(this::convert)
                             .collect(Collectors.toList());
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
    public void removePrivateSections(String tag, int pid, int count)
    {
        sectionMapper.deleteOldestN(tag, pid, count);
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
                            .collect(Collectors.toList());
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
                            .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<PrivateSection>> getPrivateSectionGroups()
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .orderByAsc(PrivateSectionEntity::getPosition);
        return sectionMapper.selectList(query)
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.groupingBy(PrivateSection::getTag));
    }

    @Override
    public Map<Integer, List<PrivateSection>> getPrivateSectionGroups(String tag)
    {
        LambdaQueryWrapper<PrivateSectionEntity> query = Wrappers.lambdaQuery(PrivateSectionEntity.class)
                                                                 .eq(StrUtil.isNotEmpty(tag), PrivateSectionEntity::getTag, tag)
                                                                 .orderByAsc(PrivateSectionEntity::getPosition);
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
                           .collect(Collectors.toList());
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
                                 .collect(Collectors.toList());
    }

    @Override
    public void addFilteringHook(FilteringHook hook)
    {
        FilteringHookEntity entity = new FilteringHookEntity();
        entity.setSourceUri(hook.getSourceUri());
        entity.setSubjectType(hook.getSubjectType());
        entity.setSubjectPid(hook.getSubjectPid());
        entity.setSubjectTableId(hook.getSubjectTableId());
        hookMapper.insert(entity);
    }

    @Override
    public void clearFilteringHooks(String sourceUri)
    {
        hookMapper.delete(Wrappers.lambdaQuery(FilteringHookEntity.class)
                                  .eq(FilteringHookEntity::getSourceUri, sourceUri));
    }

    @Override
    public List<FilteringHook> listFilteringHooks(String sourceUri)
    {
        return hookMapper.selectList(Wrappers.lambdaQuery(FilteringHookEntity.class)
                                             .eq(FilteringHookEntity::getSourceUri, sourceUri))
                         .stream()
                         .map(this::convert)
                         .collect(Collectors.toList());
    }

    @Override
    public int addStreamDensity(int pid, long position, int count, byte[] density)
    {
        DensityBulkEntity entity = new DensityBulkEntity();
        entity.setPid(pid);
        entity.setStartPosition(position);
        entity.setBulkSize(count);
        entity.setBulkEncoding(density);
        densityMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateStreamDensity(int densityRef, int count, byte[] density, double avgDensity, long maxDensity, long minDensity)
    {
        DensityBulkEntity change = new DensityBulkEntity();
        change.setId(densityRef);
        change.setBulkSize(count);
        change.setBulkEncoding(density);
        change.setAvgDensity(avgDensity);
        change.setMaxDensity(maxDensity);
        change.setMinDensity(minDensity);
        densityMapper.updateById(change);
    }

    @Override
    public List<StreamDensityStats> listStreamDensityStats()
    {
        return densityStatMapper.selectList(Wrappers.emptyWrapper())
                                .stream()
                                .map(this::convert)
                                .collect(Collectors.toList());
    }

    @Override
    public List<StreamDensityBulk> getRecentStreamDensityBulks(int pid, int limit)
    {
        return densityMapper.selectList(Wrappers.lambdaQuery(DensityBulkEntity.class)
                                                .eq(DensityBulkEntity::getPid, pid)
                                                .orderByDesc(DensityBulkEntity::getId)
                                                .last("limit " + limit))
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList());
    }

    @Override
    public int update(String sql) throws SQLException
    {
        return sqlUtils.sql(sql).update();
    }

    @Override
    public <T> List<T> query(String sql, Class<T> clazz) throws SQLException
    {
        List<T> result = new ArrayList<>();
        try (RowIterator iterator = sqlUtils.sql(sql).queryRowIterator(100))
        {
            while (iterator.hasNext())
            {
                T obj = iterator.next().toBean(clazz);
                result.add(obj);
            }
            return result;
        }
    }

    private StreamSource convert(StreamSourceEntity entity)
    {
        StreamSource source = new StreamSource();
        source.setId(entity.getId());
        source.setName(StrUtil.nullToEmpty(entity.getSourceName()));
        source.setUri(StrUtil.nullToEmpty(entity.getSourceUri()));
        source.setBitrate(NumberUtil.nullToZero(entity.getBitrate()));
        source.setFrameSize(NumberUtil.nullToZero(entity.getFrameSize()));
        source.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        source.setPacketCount(NumberUtil.nullToZero(entity.getPacketCount()));
        source.setStreamCount(NumberUtil.nullToZero(entity.getStreamCount()));
        source.setProgramCount(NumberUtil.nullToZero(entity.getProgramCount()));
        source.setScrambled(entity.getScrambled() == Boolean.TRUE);
        source.setEcmPresent(entity.getEcmPresent() == Boolean.TRUE);
        source.setEmmPresent(entity.getEmmPresent() == Boolean.TRUE);
        source.setPatPresent(entity.getPatPresent() == Boolean.TRUE);
        source.setPmtPresent(entity.getPmtPresent() == Boolean.TRUE);
        source.setCatPresent(entity.getCatPresent() == Boolean.TRUE);
        source.setNitActualPresent(entity.getNitActualPresent() == Boolean.TRUE);
        source.setNitOtherPresent(entity.getNitOtherPresent() == Boolean.TRUE);
        source.setSdtActualPresent(entity.getSdtActualPresent() == Boolean.TRUE);
        source.setSdtOtherPresent(entity.getSdtOtherPresent() == Boolean.TRUE);
        source.setEitPnfActualPresent(entity.getEitPnfActualPresent() == Boolean.TRUE);
        source.setEitPnfOtherPresent(entity.getEitPnfOtherPresent() == Boolean.TRUE);
        source.setEitSchActualPresent(entity.getEitSchActualPresent() == Boolean.TRUE);
        source.setEitSchOtherPresent(entity.getEitSchOtherPresent() == Boolean.TRUE);
        source.setBatPresent(entity.getBatPresent() == Boolean.TRUE);
        source.setTdtPresent(entity.getTdtPresent() == Boolean.TRUE);
        source.setTotPresent(entity.getTotPresent() == Boolean.TRUE);
        return source;
    }

    private ElementaryStream convert(ElementaryStreamEntity entity)
    {
        ElementaryStream stream = new ElementaryStream();
        stream.setStreamPid(entity.getPid());
        stream.setBitrate(NumberUtil.nullToZero(entity.getBitrate()));
        stream.setRatio(NumberUtil.nullToZero(entity.getRatio()));
        stream.setLastPct(NumberUtil.nullToZero(entity.getLastPct()));
        stream.setPacketCount(NumberUtil.nullToZero(entity.getPacketCount()));
        stream.setPcrCount(NumberUtil.nullToZero(entity.getPcrCount()));
        stream.setContinuityErrorCount(NumberUtil.nullToZero(entity.getContinuityErrorCount()));
        stream.setTransportErrorCount(NumberUtil.nullToZero(entity.getTransportErrorCount()));
        stream.setCategory(StrUtil.nullToEmpty(entity.getCategory()));
        stream.setDescription(StrUtil.nullToEmpty(entity.getDescription()));
        stream.setScrambled(entity.getScrambled() == Boolean.TRUE);
        stream.setStreamType(-1);
        stream.setProgramNumber(-1);
        return stream;
    }

    private MPEGProgram convert(MPEGProgramEntity entity)
    {
        MPEGProgram program = new MPEGProgram();
        program.setId(entity.getId());
        program.setProgramNumber(NumberUtil.nullToZero(entity.getProgramNumber()));
        program.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        program.setPcrPid(NumberUtil.nullToZero(entity.getPcrPid()));
        program.setPmtPid(NumberUtil.nullToZero(entity.getPmtPid()));
        program.setPmtVersion(NumberUtil.nullToZero(entity.getPmtVersion()));
        program.setFreeAccess(entity.getFreeAccess() == Boolean.TRUE);
        return program;
    }

    private CASystemStream convert(CAStreamEntity entity)
    {
        CASystemStream stream = new CASystemStream();
        stream.setId(entity.getId());
        stream.setSystemId(NumberUtil.nullToZero(entity.getSystemId()));
        stream.setStreamPid(NumberUtil.nullToZero(entity.getStreamPid()));
        stream.setStreamType(NumberUtil.nullToZero(entity.getStreamType()));
        stream.setStreamPrivateData(entity.getStreamPrivateData());
        stream.setProgramNumber(NumberUtil.nullToZero(entity.getProgramNumber()));
        stream.setElementaryStreamPid(NumberUtil.nullToZero(entity.getElementaryStreamPid()));
        return stream;
    }

    private SIBouquet convert(SIBouquetEntity entity)
    {
        SIBouquet bouquet = new SIBouquet();
        bouquet.setId(entity.getId());
        bouquet.setBouquetId(NumberUtil.nullToZero(entity.getBouquetId()));
        bouquet.setName(StrUtil.nullToEmpty(entity.getBouquetName()));
        bouquet.setServices(Collections.emptyList());
        return bouquet;
    }

    private SINetwork convert(SINetworkEntity entity)
    {
        SINetwork network = new SINetwork();
        network.setId(entity.getId());
        network.setNetworkId(NumberUtil.nullToZero(entity.getNetworkId()));
        network.setName(StrUtil.nullToEmpty(entity.getNetworkName()));
        network.setActualNetwork(entity.getActualNetwork() == Boolean.TRUE);
        network.setMultiplexCount(0);
        return network;
    }

    private SINetwork convert(SINetworkViewEntity entity)
    {
        SINetwork network = new SINetwork();
        network.setId(entity.getId());
        network.setNetworkId(NumberUtil.nullToZero(entity.getNetworkId()));
        network.setName(StrUtil.nullToDefault(entity.getNetworkName(), "未命名网络"));
        network.setActualNetwork(entity.getActualNetwork() == Boolean.TRUE);
        network.setMultiplexCount(NumberUtil.nullToZero(entity.getMultiplexCount()));
        return network;
    }

    private SIMultiplex convert(SIMultiplexEntity entity)
    {
        SIMultiplex multiplex = new SIMultiplex();
        multiplex.setId(entity.getId());
        multiplex.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        multiplex.setOriginalNetworkId(NumberUtil.nullToZero(entity.getOriginalNetworkId()));
        multiplex.setDeliverySystemType(StrUtil.nullToEmpty(entity.getDeliveryType()));
        multiplex.setTransmitFrequency(StrUtil.nullToEmpty(entity.getTransmitFrequency()));
        multiplex.setNetworkId(multiplex.getOriginalNetworkId());
        multiplex.setNetworkName("未命名网络");
        multiplex.setServices(Collections.emptyList());
        multiplex.setActualNetwork(true);
        return multiplex;
    }

    private SIMultiplex convert(SIMultiplexViewEntity entity)
    {
        SIMultiplex multiplex = new SIMultiplex();
        multiplex.setId(entity.getId());
        multiplex.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        multiplex.setOriginalNetworkId(NumberUtil.nullToZero(entity.getOriginalNetworkId()));
        multiplex.setDeliverySystemType(StrUtil.nullToDefault(entity.getDeliveryType(), "未知"));
        multiplex.setTransmitFrequency(StrUtil.nullToEmpty(entity.getTransmitFrequency()));
        multiplex.setNetworkId(NumberUtil.nullToZero(entity.getNetworkId()));
        multiplex.setNetworkName(StrUtil.nullToDefault(entity.getNetworkName(), "未命名网络"));
        multiplex.setServices(Collections.emptyList());
        multiplex.setActualNetwork(entity.getActualNetwork() == Boolean.TRUE);
        return multiplex;
    }

    private SIService convert(SIServiceEntity entity)
    {
        SIService service = new SIService();
        service.setId(entity.getId());
        service.setName(StrUtil.nullToDefault(entity.getServiceName(), "未命名业务"));
        service.setProvider(StrUtil.nullToDefault(entity.getServiceProvider(), "未知提供商"));
        service.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        service.setOriginalNetworkId(NumberUtil.nullToZero(entity.getOriginalNetworkId()));
        service.setServiceId(NumberUtil.nullToZero(entity.getServiceId()));
        service.setServiceType(NumberUtil.nullToZero(entity.getServiceType()));
        service.setServiceTypeName(ServiceTypes.name(service.getServiceType()));
        service.setRunningStatus(NumberUtil.nullToZero(entity.getRunningStatus()));
        service.setRunningStatusName(RunningStatus.name(service.getRunningStatus()));
        service.setFreeAccess(entity.getFreeAccess() == Boolean.TRUE);
        service.setPresentFollowingEITEnabled(entity.getPresentFollowingEITEnabled() == Boolean.TRUE);
        service.setScheduleEITEnabled(entity.getScheduleEITEnabled() == Boolean.TRUE);
        service.setReferenceServiceId(NumberUtil.nullToZero(entity.getReferenceServiceId()));
        service.setActualTransportStream(entity.getActualTransportStream() == Boolean.TRUE);
        return service;
    }

    private SIEvent convert(SIEventEntity entity)
    {
        SIEvent event = new SIEvent();
        event.setId(entity.getId());
        event.setTitle(StrUtil.nullToDefault(entity.getEventName(), "未定义事件"));
        event.setDescription(StrUtil.nullToEmpty(entity.getEventDescription()));
        event.setEventId(NumberUtil.nullToZero(entity.getEventId()));
        event.setTransportStreamId(NumberUtil.nullToZero(entity.getTransportStreamId()));
        event.setOriginalNetworkId(NumberUtil.nullToZero(entity.getOriginalNetworkId()));
        event.setServiceId(NumberUtil.nullToZero(entity.getServiceId()));
        event.setDuration(NumberUtil.nullToZero(entity.getDuration()));
        event.setRunningStatus(NumberUtil.nullToZero(entity.getRunningStatus()));
        event.setFreeAccess(entity.getFreeAccess() == Boolean.TRUE);
        event.setPresentEvent(entity.getPresentEvent() == Boolean.TRUE);
        event.setScheduleEvent(entity.getScheduleEvent() == Boolean.TRUE);

        if (entity.getStartTime() != null)
        {
            event.setStartTime(entity.getStartTime().atOffset(ZoneOffset.UTC));
        }

        event.setNvodTimeShiftedEvent(entity.getNvodTimeShiftedEvent() == Boolean.TRUE);
        event.setReferenceServiceId(NumberUtil.nullToZero(entity.getReferenceServiceId()));
        event.setReferenceEventId(NumberUtil.nullToZero(entity.getReferenceEventId()));
        return event;
    }

    private TR290Event convert(TR290EventEntity entity)
    {
        TR290Event event = new TR290Event();
        event.setType(entity.getType());
        event.setDescription(StrUtil.nullToEmpty(entity.getDescription()));
        event.setStream(entity.getStream());
        event.setPosition(entity.getPosition());
        event.setTimestamp(entity.getTimestamp()
                                 .atZone(ZoneId.systemDefault())
                                 .toOffsetDateTime());
        return event;
    }

    private PCRStats convert(PCRStatViewEntity entity)
    {
        PCRStats stats = new PCRStats();
        stats.setPid(entity.getPid());
        stats.setPcrCount(entity.getPcrCount());
        stats.setAvgBitrate(entity.getAvgBitrate());
        stats.setAvgInterval(entity.getAvgInterval());
        stats.setMinInterval(entity.getMinInterval());
        stats.setMaxInterval(entity.getMaxInterval());
        stats.setAvgAccuracy(entity.getAvgAccuracy());
        stats.setMinAccuracy(entity.getMinAccuracy());
        stats.setMaxAccuracy(entity.getMaxAccuracy());
        stats.setRepetitionErrors(entity.getRepetitionErrors());
        stats.setDiscontinuityErrors(entity.getDiscontinuityErrors());
        stats.setAccuracyErrors(entity.getAccuracyErrors());
        return stats;
    }

    private PCRCheck convert(PCRCheckEntity entity)
    {
        PCRCheck check = new PCRCheck();
        check.setPid(entity.getPid());
        check.setPrevValue(entity.getPreviousValue());
        check.setPrevPosition(entity.getPreviousPosition());
        check.setCurrValue(entity.getCurrentValue());
        check.setCurrPosition(entity.getCurrentPosition());
        check.setBitrate(entity.getBitrate());
        check.setIntervalNanos(entity.getIntervalNanos());
        check.setDiffNanos(entity.getDiffNanos());
        check.setAccuracyNanos(entity.getAccuracyNanos());
        check.setRepetitionCheckFailed(entity.getRepetitionCheckFailed());
        check.setDiscontinuityCheckFailed(entity.getDiscontinuityCheckFailed());
        check.setAccuracyCheckFailed(entity.getAccuracyCheckFailed());
        return check;
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

    private FilteringHook convert(FilteringHookEntity entity)
    {
        FilteringHook hook = new FilteringHook();
        hook.setId(entity.getId());
        hook.setSourceUri(entity.getSourceUri());
        hook.setSubjectType(entity.getSubjectType());
        hook.setSubjectPid(entity.getSubjectPid());
        hook.setSubjectTableId(entity.getSubjectTableId());
        return hook;
    }

    private StreamDensityStats convert(DensityStatViewEntity entity)
    {
        StreamDensityStats stats = new StreamDensityStats();
        stats.setPid(entity.getPid());
        stats.setCount(entity.getCount());
        stats.setAvgDensity(entity.getAvgDensity());
        stats.setMaxDensity(entity.getMaxDensity());
        stats.setMinDensity(entity.getMinDensity());
        return stats;
    }

    private StreamDensityBulk convert(DensityBulkEntity entity)
    {
        StreamDensityBulk bulk = new StreamDensityBulk();
        bulk.setId(entity.getId());
        bulk.setPid(entity.getPid());
        bulk.setBulkSize(entity.getBulkSize());
        bulk.setBulkEncoding(entity.getBulkEncoding());
        bulk.setStartPosition(entity.getStartPosition());
        bulk.setAvgDensity(entity.getAvgDensity());
        bulk.setMaxDensity(entity.getMaxDensity());
        bulk.setMinDensity(entity.getMinDensity());
        return bulk;
    }
}
