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
        entity.setTag(rs.getString("tag"));
        entity.setStream(rs.getInt("stream"));
        entity.setPosition(rs.getLong("position"));
        entity.setEncoding(rs.getBytes("encoding"));
        return entity;
    }
}
