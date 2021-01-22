package org.strongbox.db.server;

public class JanusGraphProperties implements JanusGraphConfiguration
{

    private final String configLocation;

    public JanusGraphProperties(String configLocation)
    {
        this.configLocation = configLocation;
    }

    @Override
    public String getConfigLocation()
    {
        return configLocation;
    }

}
