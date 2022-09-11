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

package m2tk.assistant.dbi.handler;

import cn.hutool.core.lang.generator.Generator;
import m2tk.assistant.dbi.entity.*;
import m2tk.assistant.dbi.mapper.*;
import org.jdbi.v3.core.Handle;

import java.util.List;

public class SIObjectHandler
{
    private final Generator<Long> idGenerator;
    private final SIBouquetEntityMapper bouquetEntityMapper;
    private final SINetworkEntityMapper networkEntityMapper;
    private final SIMultiplexEntityMapper multiplexEntityMapper;
    private final SIServiceEntityMapper serviceEntityMapper;
    private final SIEventEntityMapper eventEntityMapper;
    private final SIBouquetServiceMappingEntityMapper bouquetServiceMappingEntityMapper;
    private final SIMultiplexServiceCountViewMapper multiplexServiceCountViewMapper;
    private final SIDateTimeEntityMapper datetimeEntityMapper;

    public SIObjectHandler(Generator<Long> generator)
    {
        idGenerator = generator;
        bouquetEntityMapper = new SIBouquetEntityMapper();
        networkEntityMapper = new SINetworkEntityMapper();
        multiplexEntityMapper = new SIMultiplexEntityMapper();
        serviceEntityMapper = new SIServiceEntityMapper();
        eventEntityMapper = new SIEventEntityMapper();
        bouquetServiceMappingEntityMapper = new SIBouquetServiceMappingEntityMapper();
        multiplexServiceCountViewMapper = new SIMultiplexServiceCountViewMapper();
        datetimeEntityMapper = new SIDateTimeEntityMapper();
    }

    ///////////////////////////////////////////////////////////////
    // 所有数据库操作都是串行的（单线程），所以不用考虑竞争问题

    public void initTable(Handle handle)
    {
        handle.execute("DROP TABLE IF EXISTS `T_SI_BOUQUET`");
        handle.execute("CREATE TABLE `T_SI_BOUQUET` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`bqt_id` INT NOT NULL," +
                       "`bqt_name` VARCHAR(100)" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_NETWORK`");
        handle.execute("CREATE TABLE `T_SI_NETWORK` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`net_id` INT NOT NULL," +
                       "`net_name` VARCHAR(200)," +
                       "`actual_nw` BOOLEAN DEFAULT TRUE" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_MULTIPLEX`");
        handle.execute("CREATE TABLE `T_SI_MULTIPLEX` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`net_id` INT NOT NULL," +
                       "`ts_id` INT NOT NULL," +
                       "`onet_id` INT NOT NULL," +
                       "`ds_type` VARCHAR(100)," +
                       "`frequency` VARCHAR(100)" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_SERVICE`");
        handle.execute("CREATE TABLE `T_SI_SERVICE` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`ts_id` INT NOT NULL," +
                       "`onet_id` INT NOT NULL," +
                       "`srv_id` INT NOT NULL," +
                       "`ref_srv_id` INT," +
                       "`srv_type` INT," +
                       "`srv_type_name` VARCHAR(200)," +
                       "`srv_name` VARCHAR(200)," +
                       "`srv_provider` VARCHAR(200)," +
                       "`running_status` VARCHAR(100)," +
                       "`free_ca_mode` BOOLEAN DEFAULT TRUE," +
                       "`pnf_eit_enabled` BOOLEAN DEFAULT TRUE," +
                       "`sch_eit_enabled` BOOLEAN DEFAULT TRUE," +
                       "`actual_ts` BOOLEAN DEFAULT TRUE," +
                       "`nvod_reference` BOOLEAN DEFAULT FALSE," +
                       "`nvod_time_shifted` BOOLEAN DEFAULT FALSE" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_BOUQUET_SERVICE_MAPPING`");
        handle.execute("CREATE TABLE `T_SI_BOUQUET_SERVICE_MAPPING` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`ts_id` INT NOT NULL," +
                       "`onet_id` INT NOT NULL," +
                       "`srv_id` INT NOT NULL," +
                       "`bqt_id` INT NOT NULL" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_EVENT`");
        handle.execute("CREATE TABLE `T_SI_EVENT` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`ts_id` INT NOT NULL," +
                       "`onet_id` INT NOT NULL," +
                       "`srv_id` INT NOT NULL," +
                       "`evt_id` INT NOT NULL," +
                       "`ref_srv_id` INT," +
                       "`ref_evt_id` INT," +
                       "`evt_type` VARCHAR(10)," +
                       "`evt_name` VARCHAR(500)," +
                       "`evt_desc` VARCHAR(2000)," +
                       "`lang_code` VARCHAR(4)," +
                       "`start_time` VARCHAR(100)," +
                       "`duration` VARCHAR(20)," +
                       "`running_status` VARCHAR(100)," +
                       "`free_ca_mode` BOOLEAN DEFAULT TRUE," +
                       "`present` BOOLEAN DEFAULT TRUE," +
                       "`nvod_reference` BOOLEAN DEFAULT FALSE," +
                       "`nvod_time_shifted` BOOLEAN DEFAULT FALSE" +
                       ")");

        handle.execute("DROP TABLE IF EXISTS `T_SI_DATETIME`");
        handle.execute("CREATE TABLE `T_SI_DATETIME` (" +
                       "`id` BIGINT PRIMARY KEY," +
                       "`timepoint` BIGINT NOT NULL" +
                       ")");
    }

    public void resetTable(Handle handle)
    {
        handle.execute("TRUNCATE TABLE T_SI_BOUQUET");
        handle.execute("TRUNCATE TABLE T_SI_NETWORK");
        handle.execute("TRUNCATE TABLE T_SI_MULTIPLEX");
        handle.execute("TRUNCATE TABLE T_SI_SERVICE");
        handle.execute("TRUNCATE TABLE T_SI_EVENT");
        handle.execute("TRUNCATE TABLE T_SI_BOUQUET_SERVICE_MAPPING");
        handle.execute("TRUNCATE TABLE T_SI_DATETIME");
    }

    public SIBouquetEntity getBouquet(Handle handle, int bouquetId)
    {
        return handle.select("SELECT * FROM T_SI_BOUQUET WHERE `bqt_id` = ? ORDER BY `id` DESC FETCH FIRST ROW ONLY",
                             bouquetId)
                     .map(bouquetEntityMapper)
                     .findOne()
                     .orElse(null);
    }

    public SIBouquetEntity addBouquet(Handle handle, int bouquetId)
    {
        SIBouquetEntity entity = handle.select("SELECT * FROM T_SI_BOUQUET WHERE `bqt_id` = ?",
                                               bouquetId)
                                       .map(bouquetEntityMapper)
                                       .findOne()
                                       .orElse(null);
        if (entity == null)
        {
            entity = new SIBouquetEntity();
            entity.setId(idGenerator.next());
            entity.setBouquetId(bouquetId);

            handle.execute("INSERT INTO T_SI_BOUQUET (`id`, `bqt_id`) VALUES (?, ?)",
                           entity.getId(),
                           entity.getBouquetId());
        }
        return entity;
    }

    public void updateBouquetName(Handle handle, SIBouquetEntity entity)
    {
        handle.execute("UPDATE T_SI_BOUQUET SET `bqt_name` = ? WHERE `id` = ?",
                       entity.getBouquetName(),
                       entity.getId());
    }

    public void addBouquetServiceMapping(Handle handle,
                                         int bouquetId,
                                         int transportStreamID,
                                         int originalNetworkID,
                                         int serviceId)
    {
        handle.execute("INSERT INTO T_SI_BOUQUET_SERVICE_MAPPING (`id`, `bqt_id`, `ts_id`, `onet_id`, `srv_id`) " +
                       "VALUES (?,?,?,?,?)",
                       idGenerator.next(), bouquetId, transportStreamID, originalNetworkID, serviceId);
    }

    public SINetworkEntity getNetwork(Handle handle, int networkId)
    {
        return handle.select("SELECT * FROM T_SI_NETWORK WHERE `net_id` = ? ORDER BY `id` DESC FETCH FIRST ROW ONLY",
                             networkId)
                     .map(networkEntityMapper)
                     .findOne().
                     orElse(null);
    }

    public SINetworkEntity addNetwork(Handle handle, int networkId, boolean isActual)
    {
        SINetworkEntity entity = handle.select("SELECT * FROM T_SI_NETWORK WHERE `net_id` = ? AND `actual_nw` = ?",
                                               networkId, isActual)
                                       .map(networkEntityMapper)
                                       .findOne()
                                       .orElse(null);
        if (entity == null)
        {
            entity = new SINetworkEntity();
            entity.setId(idGenerator.next());
            entity.setNetworkId(networkId);
            entity.setActualNetwork(isActual);

            handle.execute("INSERT INTO T_SI_NETWORK (`id`, `net_id`, `actual_nw`) VALUES (?, ?, ?)",
                           entity.getId(), entity.getNetworkId(), entity.isActualNetwork());
        }
        return entity;
    }

    public void updateNetworkName(Handle handle, SINetworkEntity entity)
    {
        handle.execute("UPDATE T_SI_NETWORK SET `net_name` = ? WHERE `id` = ?",
                       entity.getNetworkName(), entity.getId());
    }

    public List<SINetworkEntity> listNetworks(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_NETWORK ORDER BY `id`")
                     .map(networkEntityMapper)
                     .list();
    }

    public SINetworkEntity getActualNetwork(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_NETWORK WHERE `actual_nw` = TRUE ORDER BY `id`")
                     .map(networkEntityMapper)
                     .findOne()
                     .orElse(null);
    }

    public List<SINetworkEntity> getOtherNetworks(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_NETWORK WHERE `actual_nw` = FALSE ORDER BY `id`")
                     .map(networkEntityMapper)
                     .list();
    }

    public SIMultiplexEntity addMultiplex(Handle handle,
                                          int networkId,
                                          int transportStreamId,
                                          int originalNetworkId)
    {
        SIMultiplexEntity entity = handle.select("SELECT * FROM T_SI_MULTIPLEX " +
                                                 "WHERE `net_id` = ? AND `ts_id` = ? AND `onet_id` = ?",
                                                 networkId, transportStreamId, originalNetworkId)
                                         .map(multiplexEntityMapper)
                                         .findOne()
                                         .orElse(null);
        if (entity == null)
        {
            entity = new SIMultiplexEntity();
            entity.setId(idGenerator.next());
            entity.setNetworkId(networkId);
            entity.setTransportStreamId(transportStreamId);
            entity.setOriginalNetworkId(originalNetworkId);

            handle.execute("INSERT INTO T_SI_MULTIPLEX (`id`, `net_id`, `ts_id`, `onet_id`) " +
                           "VALUES (?,?,?,?)",
                           entity.getId(),
                           entity.getNetworkId(),
                           entity.getTransportStreamId(),
                           entity.getOriginalNetworkId());
        }
        return entity;
    }

    public void updateMultiplexDeliverySystemConfigure(Handle handle, SIMultiplexEntity entity)
    {
        handle.execute("UPDATE T_SI_MULTIPLEX SET `ds_type` = ?, `frequency` = ? WHERE `id` = ?",
                       entity.getDeliverySystemType(),
                       entity.getTransmitFrequency(),
                       entity.getId());
    }

    public List<SIMultiplexEntity> listMultiplexes(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_MULTIPLEX ORDER BY `onet_id`,`ts_id`")
                     .map(multiplexEntityMapper)
                     .list();
    }

    public List<SIMultiplexEntity> listMultiplexes(Handle handle, int networkId)
    {
        return handle.select("SELECT * FROM T_SI_MULTIPLEX WHERE `net_id` = ? ORDER BY `onet_id`,`ts_id`",
                             networkId)
                     .map(multiplexEntityMapper)
                     .list();
    }

    public SIServiceEntity addService(Handle handle,
                                      int transportStreamId,
                                      int originalNetworkId,
                                      int serviceId,
                                      String runningStatus,
                                      boolean freeCAMode,
                                      boolean pnfEITEnabled,
                                      boolean schEITEnabled,
                                      boolean actualTS)
    {
        SIServiceEntity entity = handle.select("SELECT * FROM T_SI_SERVICE " +
                                               "WHERE `ts_id` = ? AND `onet_id` = ? AND `srv_id` = ?",
                                               transportStreamId,
                                               originalNetworkId,
                                               serviceId)
                                       .map(serviceEntityMapper)
                                       .findOne()
                                       .orElse(null);
        if (entity == null)
        {
            entity = new SIServiceEntity();
            entity.setId(idGenerator.next());
            entity.setTransportStreamId(transportStreamId);
            entity.setOriginalNetworkId(originalNetworkId);
            entity.setServiceId(serviceId);
            entity.setRunningStatus(runningStatus);
            entity.setFreeCAMode(freeCAMode);
            entity.setPresentFollowingEITEnabled(pnfEITEnabled);
            entity.setScheduleEITEnabled(schEITEnabled);
            entity.setActualTransportStream(actualTS);

            handle.execute("INSERT INTO T_SI_SERVICE (`id`, `ts_id`, `onet_id`, `srv_id`, `running_status`, `free_ca_mode`, `pnf_eit_enabled`, `sch_eit_enabled`, `actual_ts`) " +
                           "VALUES (?,?,?,?,?,?,?,?,?)",
                           entity.getId(),
                           entity.getTransportStreamId(),
                           entity.getOriginalNetworkId(),
                           entity.getServiceId(),
                           entity.getRunningStatus(),
                           entity.isFreeCAMode(),
                           entity.isPresentFollowingEITEnabled(),
                           entity.isScheduleEITEnabled(),
                           entity.isActualTransportStream());
        }
        return entity;
    }

    public void updateServiceType(Handle handle, SIServiceEntity entity)
    {
        handle.execute("UPDATE T_SI_SERVICE " +
                       "SET `srv_type` = ?, `srv_type_name` = ?, nvod_reference = ?, nvod_time_shifted = ? " +
                       "WHERE `id` = ?",
                       entity.getServiceType(),
                       entity.getServiceTypeName(),
                       (entity.getServiceType() == 0x04),
                       (entity.getServiceType() == 0x05),
                       entity.getId());
    }

    public void updateServiceName(Handle handle, SIServiceEntity entity)
    {
        handle.execute("UPDATE T_SI_SERVICE " +
                       "SET `srv_name` = ?, `srv_provider` = ? " +
                       "WHERE `id` = ?",
                       entity.getServiceName(),
                       entity.getServiceProvider(),
                       entity.getId());
    }

    public void updateServiceReference(Handle handle, SIServiceEntity entity)
    {
        handle.execute("UPDATE T_SI_SERVICE SET `ref_srv_id` = ? WHERE `id` = ?",
                       entity.getReferenceServiceId(),
                       entity.getId());
    }

    public List<SIServiceEntity> listServices(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_SERVICE ORDER BY `id`")
                     .map(serviceEntityMapper)
                     .list();
    }

    public List<SIServiceEntity> listServices(Handle handle, int transportStreamId)
    {
        return handle.select("SELECT * FROM T_SI_SERVICE WHERE `ts_id` = ? ORDER BY `srv_id`",
                             transportStreamId)
                     .map(serviceEntityMapper)
                     .list();
    }

    public SIEventEntity addPresentFollowingEvent(Handle handle,
                                                  int transportStreamId,
                                                  int originalNetworkId,
                                                  int serviceId,
                                                  int eventId,
                                                  String startTime,
                                                  String duration,
                                                  String runningStatus,
                                                  boolean freeCAMode,
                                                  boolean isPresent)
    {
        SIEventEntity entity = handle.select("SELECT * FROM T_SI_EVENT " +
                                             "WHERE `ts_id` = ? AND `onet_id` = ? AND `srv_id` = ? AND `evt_id` = ? AND `evt_type` = ?",
                                             transportStreamId,
                                             originalNetworkId,
                                             serviceId,
                                             eventId,
                                             SIEventEntity.TYPE_PRESENT_FOLLOWING)
                                     .map(eventEntityMapper)
                                     .findOne()
                                     .orElse(null);
        if (entity == null)
        {
            entity = new SIEventEntity();
            entity.setId(idGenerator.next());
            entity.setTransportStreamId(transportStreamId);
            entity.setOriginalNetworkId(originalNetworkId);
            entity.setServiceId(serviceId);
            entity.setEventId(eventId);
            entity.setEventType(SIEventEntity.TYPE_PRESENT_FOLLOWING);
            entity.setEventName("<未命名事件>");
            entity.setStartTime(startTime);
            entity.setDuration(duration);
            entity.setRunningStatus(runningStatus);
            entity.setFreeCAMode(freeCAMode);
            entity.setPresentEvent(isPresent);
            entity.setNvodReferenceEvent(startTime.equals("未定义"));

            handle.execute("INSERT INTO T_SI_EVENT (`id`, `ts_id`, `onet_id`, `srv_id`, `evt_id`, `evt_type`, `evt_name`, " +
                           "`start_time`, `duration`, `running_status`, `free_ca_mode`, `present`, `nvod_reference`) " +
                           "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                           entity.getId(),
                           entity.getTransportStreamId(),
                           entity.getOriginalNetworkId(),
                           entity.getServiceId(),
                           entity.getEventId(),
                           entity.getEventType(),
                           entity.getEventName(),
                           entity.getStartTime(),
                           entity.getDuration(),
                           entity.getRunningStatus(),
                           entity.isFreeCAMode(),
                           entity.isPresentEvent(),
                           entity.isNvodReferenceEvent());
        }
        return entity;
    }

    public SIEventEntity addScheduleEvent(Handle handle,
                                          int transportStreamId,
                                          int originalNetworkId,
                                          int serviceId,
                                          int eventId,
                                          String startTime,
                                          String duration,
                                          String runningStatus,
                                          boolean freeCAMode)
    {
        SIEventEntity entity = handle.select("SELECT * FROM T_SI_EVENT " +
                                             "WHERE `ts_id` = ? AND `onet_id` = ? AND `srv_id` = ? AND `evt_id` = ? AND `evt_type` = ?",
                                             transportStreamId,
                                             originalNetworkId,
                                             serviceId,
                                             eventId,
                                             SIEventEntity.TYPE_SCHEDULE)
                                     .map(eventEntityMapper)
                                     .findOne()
                                     .orElse(null);
        if (entity == null)
        {
            entity = new SIEventEntity();
            entity.setId(idGenerator.next());
            entity.setTransportStreamId(transportStreamId);
            entity.setOriginalNetworkId(originalNetworkId);
            entity.setServiceId(serviceId);
            entity.setEventId(eventId);
            entity.setEventType(SIEventEntity.TYPE_SCHEDULE);
            entity.setEventName("<未命名事件>");
            entity.setStartTime(startTime);
            entity.setDuration(duration);
            entity.setRunningStatus(runningStatus);
            entity.setFreeCAMode(freeCAMode);

            handle.execute("INSERT INTO T_SI_EVENT (`id`, `ts_id`, `onet_id`, `srv_id`, `evt_id`, `evt_type`, `evt_name`, " +
                           "`start_time`, `duration`, `running_status`, `free_ca_mode`) " +
                           "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                           entity.getId(),
                           entity.getTransportStreamId(),
                           entity.getOriginalNetworkId(),
                           entity.getServiceId(),
                           entity.getEventId(),
                           entity.getEventType(),
                           entity.getEventName(),
                           entity.getStartTime(),
                           entity.getDuration(),
                           entity.getRunningStatus(),
                           entity.isFreeCAMode());
        }
        return entity;
    }

    public void updateEventDescription(Handle handle, SIEventEntity entity)
    {
        handle.execute("UPDATE T_SI_EVENT " +
                       "SET `evt_name` = ?, `evt_desc` = ?, `lang_code` = ? " +
                       "WHERE `id` = ?",
                       entity.getEventName(),
                       entity.getEventDescription(),
                       entity.getLanguageCode(),
                       entity.getId());
    }

    public void updateEventReference(Handle handle, SIEventEntity entity)
    {
        handle.execute("UPDATE T_SI_EVENT " +
                       "SET `ref_srv_id` = ?, `ref_evt_id` = ? " +
                       "WHERE `id` = ?",
                       entity.getReferenceServiceId(),
                       entity.getReferenceEventId(),
                       entity.getId());
    }

    public List<SIEventEntity> listEvents(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_EVENT")
                     .map(eventEntityMapper)
                     .list();
    }

    public List<SIEventEntity> listServiceEvents(Handle handle, int transportStreamId, int originalNetworkId,
                                                 int serviceId)
    {
        return handle.select("SELECT * FROM T_SI_EVENT " +
                             "WHERE `ts_id` = ? AND `onet_id` = ? AND `srv_id` = ? AND `evt_type` = ?",
                             transportStreamId,
                             originalNetworkId,
                             serviceId,
                             SIEventEntity.TYPE_SCHEDULE)
                     .map(eventEntityMapper)
                     .list();
    }

    public void addDateTime(Handle handle, long timepoint)
    {
        handle.execute("INSERT INTO T_SI_DATETIME (`id`, `timepoint`) " +
                       "VALUES (?,?)",
                       idGenerator.next(), timepoint);
    }

    public SIDateTimeEntity getLatestDateTime(Handle handle)
    {
        return handle.select("SELECT * FROM T_SI_DATETIME ORDER BY `id` DESC " +
                             "FETCH FIRST ROW ONLY")
                     .map(datetimeEntityMapper)
                     .findOne()
                     .orElse(null);
    }

    public List<SIMultiplexServiceCountView> listMultiplexServiceCounts(Handle handle)
    {
        return handle.select("SELECT `ts_id`, `onet_id`, COUNT(srv_id) AS `srv_cnt` FROM T_SI_SERVICE " +
                             "GROUP BY `ts_id`, `onet_id`")
                     .map(multiplexServiceCountViewMapper)
                     .list();
    }
}
