package m2tk.assistant.dbi.mapper;

import m2tk.assistant.dbi.entity.ServiceEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceEntityMapper implements RowMapper<ServiceEntity>
{
    @Override
    public ServiceEntity map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(rs.getLong("id"));
        entity.setServiceId(rs.getInt("srv_id"));
        entity.setTransportStreamId(rs.getInt("ts_id"));
        entity.setOriginalNetworkId(rs.getInt("org_net_id"));
        entity.setFreeAccess(rs.getBoolean("free_access"));
        entity.setServiceType(rs.getString("srv_type"));
        entity.setServiceName(rs.getString("srv_name"));
        entity.setServiceProvider(rs.getString("srv_provider"));
        return entity;
    }
}
