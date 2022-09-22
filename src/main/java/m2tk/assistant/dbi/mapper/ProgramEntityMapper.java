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
