package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.SourceEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SourceEntityMapper implements RowMapper<SourceEntity>
{
    @Override
    public SourceEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        SourceEntity entity = new SourceEntity();
        entity.setId(rs.getLong("id"));
        entity.setBitrate(rs.getInt("bitrate"));
        entity.setFrameSize(rs.getInt("frame_size"));
        entity.setTransportStreamId(rs.getInt("transport_stream_id"));
        entity.setPacketCount(rs.getLong("packet_count"));
        entity.setSourceName(rs.getString("source_name"));
        return entity;
    }
}
