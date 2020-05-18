package org.strongbox.db.server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.StorageService;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.diskstorage.idmanagement.ConflictAvoidanceMode;
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

    public JanusGraph getJanusGraph()
    {
        return janusGraph;
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
            provideJanusGraphInstanceWithRetry();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("Unable to start the embedded DB server!", e);
        }
    }

    private CassandraDaemon provideCassandraInstance()
    {
        if (cassandraDaemon != null)
        {
            return cassandraDaemon;
        }

        System.setProperty("cassandra.config.loader", cassandraEmbeddedProperties.getCassandraConfigLoaderClassName());

        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "true");
        System.setProperty("cassandra.unsafesystem", "true");

        CassandraDaemon cassandraDaemonLocal = new CassandraDaemon(true);
        cassandraDaemonLocal.activate();

        StorageService.instance.removeShutdownHook();

        return this.cassandraDaemon = cassandraDaemonLocal;
    }

    private JanusGraph provideJanusGraphInstanceWithRetry()
        throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            try
            {
                return provideJanusGraphInstance();
            }
            catch (Exception e)
            {
                Thread.sleep(500);
                logger.info(String.format("Retry JanusGraph instance initialization with [%s] attempt...", i));
            }
        }

        return provideJanusGraphInstance();
    }

    private JanusGraph provideJanusGraphInstance()
        throws Exception
    {
        if (janusGraph != null)
        {
            return janusGraph;
        }

        JanusGraphFactory.Builder builder = JanusGraphFactory.build();

        builder.set("gremlin.graph", org.janusgraph.core.JanusGraphFactory.class.getName())
               .set("cache.db-cache", true)
               // This is a percentage of the JVM memory - 0.4 means 40% of -Xmx!
               .set("cache.db-cache-size", 0.3)
               .set("cache.db-cache-clean-wait", 0) // can be 0 for local database
               .set("cache.db-cache-time", 60000)
               .set("cache.tx-cache-size", 10000)
               .set("cache.tx-dirty-size", 100)
               .set("ids.block-size", 5000) // default: 10000
               .set("ids.num-partitions", 3) // default: 10
               .set("ids.renew-timeout", 300000) // default: 120000 ms
               .set("ids.renew-percentage", 0.2) // percentage; default: 0.3
               .set("ids.authority.conflict-avoidance-mode", ConflictAvoidanceMode.GLOBAL_AUTO)
               .set("storage.backend", "cql")
               .set("storage.hostname", janusGraphProperties.getStorageHost())
               .set("storage.port", janusGraphProperties.getStoragePort())
               .set("storage.username", janusGraphProperties.getStorageUsername())
               .set("storage.password", janusGraphProperties.getStoragePassword())
               .set("storage.cql.keyspace", "strongbox")
               // TODO: Make these configurable from strongbox.yaml start-->
               .set("storage.cql.only-use-local-consistency-for-system-operations", true)
               .set("storage.cql.local-core-connections-per-host", 1)
               .set("storage.cql.local-max-connections-per-host", 10)
               .set("storage.cql.read-consistency-level", "ONE")
               .set("storage.cql.write-consistency-level", "ONE")
               .set("storage.cassandra.replication-factor", 1)
               // TODO: Make these configurable from strongbox.yaml <--end
               .set("storage.cassandra.compaction-strategy-class", SizeTieredCompactionStrategy.class)
               .set("storage.cassandra.compaction-strategy-options", Collections.emptyList())
               .set("storage.cassandra.frame-size-mb", 0) // thrift is deprecated!
               // disable sstable_compression prevent additional unnecessary IO (disk space is cheap)
               .set("storage.cassandra.compression-type", null)
               .set("storage.lock.wait-time", 5) // default 100 (probably ok for cluster, bad for single node)
               .set("storage.lock.retries", 20)
               .set("tx.log-tx", false)
               .set("schema.default", "none")
               .set("schema.constraints", true)
               // enable metrics
               .set("metrics.enabled", true)
               .set("metrics.jmx.enabled", true)
        ;

        JanusGraph janusGraphLocal = builder.open();

        try
        {
            Method removeHookMethod = StandardJanusGraph.class.getDeclaredMethod("removeHook");
            removeHookMethod.setAccessible(true);
            removeHookMethod.invoke(janusGraphLocal);
        }
        catch (Exception e)
        {
            janusGraphLocal.close();
            throw e;
        }

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
        try
        {
            cassandraDaemon.deactivate();
        }
        finally
        {
            cassandraDaemon = null;
        }
    }

    private void closeJanusGraph()
    {
        if (janusGraph == null)
        {
            logger.info("Skip closing JanusGraph..");
            return;
        }
        logger.info("Shutting JanusGraph instance..");
        try
        {
            janusGraph.close();
        }
        finally
        {
            janusGraph = null;
        }
    }

}
