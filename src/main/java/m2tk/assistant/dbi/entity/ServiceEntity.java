package m2tk.assistant.dbi.entity;

public class ServiceEntity
{
    private long id;
    private int serviceId;
    private int transportStreamId;
    private int originalNetworkId;
    private boolean freeAccess;
    private String serviceType;
    private String serviceName;
    private String serviceProvider;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getServiceId()
    {
        return serviceId;
    }

    public void setServiceId(int serviceId)
    {
        this.serviceId = serviceId;
    }

    public int getTransportStreamId()
    {
        return transportStreamId;
    }

    public void setTransportStreamId(int transportStreamId)
    {
        this.transportStreamId = transportStreamId;
    }

    public int getOriginalNetworkId()
    {
        return originalNetworkId;
    }

    public void setOriginalNetworkId(int originalNetworkId)
    {
        this.originalNetworkId = originalNetworkId;
    }

    public boolean isFreeAccess()
    {
        return freeAccess;
    }

    public void setFreeAccess(boolean freeAccess)
    {
        this.freeAccess = freeAccess;
    }

    public String getServiceType()
    {
        return serviceType;
    }

    public void setServiceType(String serviceType)
    {
        this.serviceType = serviceType;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public String getServiceProvider()
    {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }
}
