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

package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.SIServiceEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SIServiceEntityMapper implements RowMapper<SIServiceEntity>
{
    @Override
    public SIServiceEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        SIServiceEntity entity = new SIServiceEntity();
        entity.setId(rs.getLong("id"));
        entity.setTransportStreamId(rs.getInt("ts_id"));
        entity.setOriginalNetworkId(rs.getInt("onet_id"));
        entity.setServiceId(rs.getInt("srv_id"));
        entity.setReferenceServiceId(rs.getInt("ref_srv_id"));
        entity.setServiceType(rs.getInt("srv_type"));
        entity.setServiceTypeName(rs.getString("srv_type_name"));
        entity.setServiceName(rs.getString("srv_name"));
        entity.setServiceProvider(rs.getString("srv_provider"));
        entity.setRunningStatus(rs.getString("running_status"));
        entity.setFreeCAMode(rs.getBoolean("free_ca_mode"));
        entity.setActualTransportStream(rs.getBoolean("actual_ts"));
        entity.setPresentFollowingEITEnabled(rs.getBoolean("pnf_eit_enabled"));
        entity.setScheduleEITEnabled(rs.getBoolean("sch_eit_enabled"));
        entity.setNvodReferenceService(rs.getBoolean("nvod_reference"));
        entity.setNvodTimeShiftedService(rs.getBoolean("nvod_time_shifted"));
        return entity;
    }
}
