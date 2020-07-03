package org.strongbox.db.server;

public class CassandraEmbeddedProperties
        implements CassandraEmbeddedConfiguration
{

    private final Integer port;
    private final String storageRoot;
    private final String configLocatoion;

    public CassandraEmbeddedProperties(String storageRoot,
                                       int port,
                                       String configLocatoion)
    {
        this.storageRoot = storageRoot;
        this.port = port;
        this.configLocatoion = configLocatoion;
    }

    @Override
    public Integer getPort()
    {
        return port;
    }

    @Override
    public String getStorageRoot()
    {
        return storageRoot;
    }

    @Override
    public String getConfigLocatoion()
    {
        return configLocatoion;
    }

}