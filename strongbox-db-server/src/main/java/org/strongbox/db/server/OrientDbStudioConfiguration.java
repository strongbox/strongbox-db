package org.strongbox.db.server;

public interface OrientDbStudioConfiguration
{

    boolean isEnabled();

    String getIpAddress();

    int getPort();

    String getPath();

}
