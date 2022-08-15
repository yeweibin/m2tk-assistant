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

import m2tk.assistant.dbi.entity.PCRStatEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PCRStatEntityMapper implements RowMapper<PCRStatEntity>
{
    @Override
    public PCRStatEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        PCRStatEntity entity = new PCRStatEntity();
        entity.setPid(rs.getInt("pid"));
        entity.setPcrCount(rs.getLong("pcr_count"));
        entity.setAvgBitrate(rs.getLong("avg_bitrate"));
        entity.setAvgInterval(rs.getLong("avg_interval"));
        entity.setMinInterval(rs.getLong("min_interval"));
        entity.setMaxInterval(rs.getLong("max_interval"));
        entity.setAvgAccuracy(rs.getLong("avg_accuracy"));
        entity.setMinAccuracy(rs.getLong("min_accuracy"));
        entity.setMaxAccuracy(rs.getLong("max_accuracy"));
        entity.setRepetitionErrors(rs.getLong("repetition_errors"));
        entity.setDiscontinuityErrors(rs.getLong("discontinuity_errors"));
        entity.setAccuracyErrors(rs.getLong("accuracy_errors"));
        return entity;
    }
}
