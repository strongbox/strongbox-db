package org.strongbox.db.server;

public interface CassandraEmbeddedConfiguration
{

    Integer getPort();

    String getStorageRoot();

    String getConfigLocatoion();
    
}
