package org.strongbox.db.server;

public interface CassandraEmbeddedConfiguration
{

    Integer getPort();

    String getStorageFolder();

    String getCassandraConfigLoaderClassName();
}
