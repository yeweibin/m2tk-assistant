package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.CAStreamEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CAStreamEntityMapper implements RowMapper<CAStreamEntity>
{
    @Override
    public CAStreamEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        CAStreamEntity entity = new CAStreamEntity();
        entity.setId(rs.getLong("id"));
        entity.setTransactionId(rs.getLong("transaction_id"));
        entity.setSystemId(rs.getInt("cas_id"));
        entity.setStreamType(rs.getInt("stream_type"));
        entity.setStreamPid(rs.getInt("stream_pid"));
        entity.setStreamPrivateData(rs.getBytes("stream_private_data"));
        entity.setProgramNumber(rs.getInt("program_number"));
        entity.setElementaryStreamPid(rs.getInt("es_pid"));
        return entity;
    }
}
