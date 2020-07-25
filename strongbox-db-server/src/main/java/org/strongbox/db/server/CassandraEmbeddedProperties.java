package org.strongbox.db.server;

public class CassandraEmbeddedProperties
        implements CassandraEmbeddedConfiguration
{

    private final String storageRoot;
    private final String configLocation;

    public CassandraEmbeddedProperties(String storageRoot,
                                       String configLocation)
    {
        this.storageRoot = storageRoot;
        this.configLocation = configLocation;
    }

    @Override
    public String getStorageRoot()
    {
        return storageRoot;
    }

    @Override
    public String getConfigLocation()
    {
        return configLocation;
    }

}