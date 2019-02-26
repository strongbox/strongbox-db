package org.strongbox.db.server;

public class OrientDbServerProperties implements OrientDbServerConfiguration
{

    private String username;
    private String password;
    private String protocol;
    private String host;
    private String port;
    private String database;
    private String path;

    @Override
    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @Override
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    @Override
    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    @Override
    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    @Override
    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    @Override
    public String getUrl()
    {
        if ("memory".equals(protocol))
        {
            return String.format("%s:%s", protocol, database);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(protocol);
        if (host != null)
        {
            sb.append(":").append(host);
        }
        if (port != null)
        {
            sb.append(":").append(port);
        }
        sb.append("/").append(database);

        return sb.toString();
    }

}
