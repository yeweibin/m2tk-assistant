package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.SectionEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SectionEntityMapper implements RowMapper<SectionEntity>
{
    @Override
    public SectionEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        SectionEntity entity = new SectionEntity();
        entity.setId(rs.getLong("id"));
        entity.setEncoding(rs.getBytes("encoding"));
        entity.setStreamPid(rs.getInt("stream_pid"));
        entity.setPosition(rs.getLong("position"));
        entity.setTableId(rs.getInt("table_id"));
        entity.setName(rs.getString("name"));
        return entity;
    }
}
