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

import m2tk.assistant.dbi.entity.TR290EventEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TR290EventEntityMapper implements RowMapper<TR290EventEntity>
{
    @Override
    public TR290EventEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        TR290EventEntity entity = new TR290EventEntity();
        entity.setId(rs.getLong("id"));
        entity.setTransactionId(rs.getLong("transaction_id"));
        entity.setType(rs.getString("type"));
        entity.setDescription(rs.getString("description"));
        entity.setStreamPid(rs.getInt("stream_pid"));
        entity.setPosition(rs.getLong("position"));
        entity.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        return entity;
    }
}
