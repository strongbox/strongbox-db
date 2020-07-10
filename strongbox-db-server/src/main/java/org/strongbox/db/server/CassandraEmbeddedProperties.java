package org.strongbox.db.server;

public class CassandraEmbeddedProperties
        implements CassandraEmbeddedConfiguration
{

    private final String storageRoot;
    private final String configLocatoion;

    public CassandraEmbeddedProperties(String storageRoot,
                                       String configLocatoion)
    {
        this.storageRoot = storageRoot;
        this.configLocatoion = configLocatoion;
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