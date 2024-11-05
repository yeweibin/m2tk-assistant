/*
 * Copyright (c) M2TK Project. All rights reserved.
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

import m2tk.assistant.dbi.entity.ProgramEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProgramEntityMapper implements RowMapper<ProgramEntity>
{
    @Override
    public ProgramEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ProgramEntity entity = new ProgramEntity();
        entity.setId(rs.getLong("id"));
        entity.setTransactionId(rs.getLong("transaction_id"));
        entity.setProgramNumber(rs.getInt("prg_num"));
        entity.setTransportStreamId(rs.getInt("ts_id"));
        entity.setPmtPid(rs.getInt("pmt_pid"));
        entity.setPcrPid(rs.getInt("pcr_pid"));
        entity.setPmtVersion(rs.getInt("pmt_version"));
        entity.setFreeAccess(rs.getBoolean("free_access"));
        return entity;
    }
}
