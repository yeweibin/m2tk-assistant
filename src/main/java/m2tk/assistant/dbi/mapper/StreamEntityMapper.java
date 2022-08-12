package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.StreamEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StreamEntityMapper implements RowMapper<StreamEntity>
{
    @Override
    public StreamEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        StreamEntity entity = new StreamEntity();
        entity.setId(rs.getLong("id"));
        entity.setPid(rs.getInt("pid"));
        entity.setPacketCount(rs.getLong("pkt_cnt"));
        entity.setContinuityErrorCount(rs.getInt("cc_error_cnt"));
        entity.setBitrate(rs.getInt("bitrate"));
        entity.setRatio(rs.getDouble("ratio"));
        entity.setScrambled(rs.getBoolean("scrambled"));
        entity.setCategory(rs.getString("category"));
        entity.setDescription(rs.getString("description"));
        entity.setMarked(rs.getBoolean("marked"));
        return entity;
    }
}
