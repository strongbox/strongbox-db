package org.strongbox.db.server;

public class OrientDbStudioProperties implements OrientDbStudioConfiguration
{

    private boolean enabled;
    private String ipAddress;
    private int port;
    private String path;

    /* (non-Javadoc)
     * @see org.strongbox.db.server.OrientDbStudioConfiguration#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean studioEnabled)
    {
        this.enabled = studioEnabled;
    }

    /* (non-Javadoc)
     * @see org.strongbox.db.server.OrientDbStudioConfiguration#getIpAddress()
     */
    @Override
    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String studioIpAddress)
    {
        this.ipAddress = studioIpAddress;
    }

    /* (non-Javadoc)
     * @see org.strongbox.db.server.OrientDbStudioConfiguration#getPort()
     */
    @Override
    public int getPort()
    {
        return port;
    }

    public void setPort(int studioPort)
    {
        this.port = studioPort;
    }

    /* (non-Javadoc)
     * @see org.strongbox.db.server.OrientDbStudioConfiguration#getPath()
     */
    @Override
    public String getPath()
    {
        return path;
    }

    public void setPath(String studioPath)
    {
        this.path = studioPath;
    }

}
