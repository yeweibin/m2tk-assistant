package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.ProgramStreamMappingEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProgramStreamMappingEntityMapper implements RowMapper<ProgramStreamMappingEntity>
{
    @Override
    public ProgramStreamMappingEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ProgramStreamMappingEntity entity = new ProgramStreamMappingEntity();
        entity.setId(rs.getLong("id"));
        entity.setTransactionId(rs.getLong("transaction_id"));
        entity.setProgramNumber(rs.getInt("prg_num"));
        entity.setStreamPid(rs.getInt("es_pid"));
        entity.setStreamType(rs.getInt("es_type"));
        entity.setStreamCategory(rs.getString("es_cate"));
        entity.setStreamDescription(rs.getString("es_desc"));
        return entity;
    }
}
