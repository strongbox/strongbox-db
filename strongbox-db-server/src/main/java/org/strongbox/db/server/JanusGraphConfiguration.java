package org.strongbox.db.server;

public interface JanusGraphConfiguration
{

    String getStorageUsername();

    String getStoragePassword();

    String getStorageHost();

    Integer getStoragePort();

    String getStorageRoot();

}
