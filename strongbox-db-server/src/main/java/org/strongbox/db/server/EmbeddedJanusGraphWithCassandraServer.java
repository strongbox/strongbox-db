package org.strongbox.db.server;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.StorageService;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded JanusGraph+Cassandra server
 * 
 * @author sbespalov
 */
public class EmbeddedJanusGraphWithCassandraServer
        implements EmbeddedDbServer
{

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedJanusGraphWithCassandraServer.class);

    private final CassandraEmbeddedConfiguration cassandraEmbeddedProperties;
    private final JanusGraphConfiguration janusGraphProperties;
    private volatile JanusGraph janusGraph;
    private volatile CassandraDaemon cassandraDaemon;

    public EmbeddedJanusGraphWithCassandraServer(CassandraEmbeddedConfiguration cassandraEmbeddedProperties,
                                                 JanusGraphConfiguration janusGraphProperties)
    {
        this.janusGraphProperties = janusGraphProperties;
        this.cassandraEmbeddedProperties = cassandraEmbeddedProperties;
    }

    @PostConstruct
    public void start()
        throws Exception
    {
        try
        {
            provideCassandraInstance();
            provideGanusGraphInstance();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("Unable to start the embedded DB server!", e);
        }
    }

    private synchronized CassandraDaemon provideCassandraInstance()
    {
        if (cassandraDaemon != null)
        {
            return cassandraDaemon;
        }

        System.setProperty("cassandra.config.loader",
                           cassandraEmbeddedProperties.getCassandraConfigLoaderClassName());

        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "false");
        System.setProperty("cassandra.unsafesystem", "true");

        CassandraDaemon cassandraDaemonLocal = new CassandraDaemon(true);
        cassandraDaemonLocal.activate();

        StorageService.instance.removeShutdownHook();

        return this.cassandraDaemon = cassandraDaemonLocal;
    }

    private synchronized JanusGraph provideGanusGraphInstance()
        throws Exception
    {
        if (janusGraph != null)
        {
            return janusGraph;
        }

        JanusGraph janusGraphLocal = JanusGraphFactory.build()
                                                      .set("storage.backend", "cql")
                                                      .set("storage.hostname", janusGraphProperties.getStorageHost())
                                                      .set("storage.port", janusGraphProperties.getStoragePort())
                                                      .set("storage.username",
                                                           janusGraphProperties.getStorageUsername())
                                                      .set("storage.password",
                                                           janusGraphProperties.getStoragePassword())
                                                      .set("storage.cql.keyspace", "strongbox")
                                                      .set("tx.log-tx", true)
                                                      .open();

        StandardJanusGraph.class.getMethod("removeHook").invoke(janusGraphLocal);

        return this.janusGraph = janusGraphLocal;
    }

    @PreDestroy
    @Override
    public synchronized void stop()
        throws Exception
    {
        try
        {
            closeJanusGraph();
        }
        catch (Exception e)
        {
            logger.error("Failed to close JanusGraph", e);
        }

        try
        {
            closeCassandra();
        }
        catch (Exception e)
        {
            logger.error("Failed to close Cassandra", e);
        }

        logger.info("All embedded DB services stopped!");
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
        cassandraDaemon.deactivate();
    }

    private void closeJanusGraph()
    {
        if (janusGraph == null)
        {
            logger.info("Skip closing JanusGraph..");
            return;
        }
        logger.info("Shutting JanusGraph instance..");
        janusGraph.close();
    }

}
