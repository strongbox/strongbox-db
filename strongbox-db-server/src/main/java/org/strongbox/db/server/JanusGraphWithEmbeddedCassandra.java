package org.strongbox.db.server;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strongbox.db.server.cassandra.CassandraEmbeddedPropertiesLoader;

/**
 * Embedded JanusGraph+Cassandra server
 * 
 * @author sbespalov
 */
public class JanusGraphWithEmbeddedCassandra extends JanusGraphWithRemoteCassandra
{

    private static final Logger logger = LoggerFactory.getLogger(JanusGraphWithEmbeddedCassandra.class);

    private final CassandraEmbeddedConfiguration cassandraEmbeddedProperties;
    private volatile CassandraDaemon cassandraDaemon;

    public JanusGraphWithEmbeddedCassandra(CassandraEmbeddedConfiguration cassandraEmbeddedProperties,
                                           JanusGraphConfiguration janusGraphProperties)
    {
        super(janusGraphProperties);

        this.cassandraEmbeddedProperties = cassandraEmbeddedProperties;
    }

    public CassandraDaemon getCassandraDaemon()
    {
        return cassandraDaemon;
    }

    @PostConstruct
    public synchronized void start()
        throws Exception
    {
        try
        {
            provideCassandraInstance();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("Unable to start the embedded DB server!", e);
        }

        super.start();
    }

    private CassandraDaemon provideCassandraInstance()
    {
        if (cassandraDaemon != null)
        {
            return cassandraDaemon;
        }

        Class<CassandraEmbeddedPropertiesLoader> configLoaderClass = CassandraEmbeddedPropertiesLoader.bootstrap(cassandraEmbeddedProperties);
        System.setProperty("cassandra.config.loader", configLoaderClass.getName());

        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "true");
        System.setProperty("cassandra.unsafesystem", "true");

        CassandraDaemon cassandraDaemonLocal = new CassandraDaemon(true);
        cassandraDaemonLocal.activate();

        StorageService.instance.removeShutdownHook();

        return this.cassandraDaemon = cassandraDaemonLocal;
    }

    @PreDestroy
    @Override
    public synchronized void stop()
        throws Exception
    {
        super.stop();

        try
        {
            closeCassandra();
        }
        catch (Exception e)
        {
            logger.error("Failed to close Cassandra", e);
        }

        logger.info("Cassandra embedded service stopped!");
    }

    private void closeCassandra()
        throws IOException,
        InterruptedException,
        ExecutionException
    {
        if (cassandraDaemon == null)
        {
            logger.info("Skip closing cassandra daemon..");
            return;
        }
        logger.info("Shutting down cassandra daemon..");
        try
        {
            StorageService.instance.drain();
        }
        catch (Exception e)
        {
            logger.error("Failed to drain cassandra service.", e);
        }
        try
        {
            cassandraDaemon.deactivate();
        }
        finally
        {
            cassandraDaemon = null;
        }
    }

}
