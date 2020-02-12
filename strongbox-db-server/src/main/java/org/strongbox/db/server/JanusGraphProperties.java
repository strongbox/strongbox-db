package org.strongbox.db.server;

public class JanusGraphProperties implements JanusGraphConfiguration
{

    private String storageUsername;
    private String storagePassword;
    private String storageHost;
    private Integer storagePort;
    private String storageRoot;

    public JanusGraphProperties()
    {
    }

    public JanusGraphProperties(String storageUsername,
                                String storagePassword,
                                String storageHost,
                                Integer storagePort,
                                String storageRoot)
    {
        this.storageUsername = storageUsername;
        this.storagePassword = storagePassword;
        this.storageHost = storageHost;
        this.storagePort = storagePort;
        this.storageRoot = storageRoot;
    }

    @Override
    public String getStorageUsername()
    {
        return storageUsername;
    }

    @Override
    public String getStoragePassword()
    {
        return storagePassword;
    }

    @Override
    public String getStorageHost()
    {
        return storageHost;
    }

    @Override
    public Integer getStoragePort()
    {
        return storagePort;
    }

    @Override
    public String getStorageRoot()
    {
        return storageRoot;
    }

    public void setStorageUsername(String storageUsername)
    {
        this.storageUsername = storageUsername;
    }

    public void setStoragePassword(String storagePassword)
    {
        this.storagePassword = storagePassword;
    }

    public void setStorageHost(String storageHost)
    {
        this.storageHost = storageHost;
    }

    public void setStoragePort(Integer storagePort)
    {
        this.storagePort = storagePort;
    }

    public void setStorageRoot(String storageRoot)
    {
        this.storageRoot = storageRoot;
    }

}
