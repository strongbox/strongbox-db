package org.strongbox.db.server;

public interface EmbeddedDbServer
{

    void start() throws Exception;

    void stop() throws Exception;

}
