package org.strongbox.db.server;

import java.util.Collections;

import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphFactory.Builder;
import org.janusgraph.diskstorage.idmanagement.ConflictAvoidanceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 */
public class JanusGraphWithRemoteCassandra extends JanusGraphServer
{

    private static final Logger logger = LoggerFactory.getLogger(JanusGraphWithEmbeddedCassandra.class);

    public JanusGraphWithRemoteCassandra(JanusGraphConfiguration janusGraphProperties)
    {
        super(janusGraphProperties);
    }

    @Override
    protected JanusGraph provideJanusGraphInstance()
        throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            try
            {
                return super.provideJanusGraphInstance();
            }
            catch (Exception e)
            {
                Thread.sleep(500);
                logger.info(String.format("Retry JanusGraph instance initialization with [%s] attempt...", i));
            }
        }

        return super.provideJanusGraphInstance();
    }

    @Override
    protected Builder buildJanusGraph(JanusGraphConfiguration configuration)
    {
        JanusGraphFactory.Builder builder = JanusGraphFactory.build();

        return builder.set("gremlin.graph", org.janusgraph.core.JanusGraphFactory.class.getName())
                      // Disabling database cache since it might lead to
                      // inconsistencies.
                      .set("cache.db-cache", false)
                      // This is a percentage of the JVM memory - 0.4 means 40%
                      // of -Xmx!
                      .set("cache.db-cache-size", 0)
                      .set("cache.db-cache-clean-wait", 0) // can be 0 for local
                                                           // database
                      .set("cache.db-cache-time", 60000)
                      .set("cache.tx-cache-size", 50000)
                      .set("cache.tx-dirty-size", 1000)
                      .set("ids.block-size", 5000) // default: 10000
                      .set("ids.num-partitions", 3) // default: 10
                      .set("ids.renew-timeout", 300000) // default: 120000 ms
                      .set("ids.renew-percentage", 0.2) // percentage; default:
                                                        // 0.3
                      .set("ids.authority.conflict-avoidance-mode", ConflictAvoidanceMode.GLOBAL_AUTO)
                      .set("storage.backend", "cql")
                      .set("storage.hostname", configuration.getStorageHost())
                      .set("storage.port", configuration.getStoragePort())
                      .set("storage.username", configuration.getStorageUsername())
                      .set("storage.password", configuration.getStoragePassword())
                      // Whether JanusGraph should attempt to parallelize
                      // storage operations
                      .set("storage.parallel-backend-ops", true)
                      .set("storage.cql.keyspace", "strongbox")
                      // TODO: Make these configurable from strongbox.yaml
                      // start-->
                      .set("storage.cql.only-use-local-consistency-for-system-operations", true)
                      .set("storage.cql.local-core-connections-per-host", 1)
                      .set("storage.cql.local-max-connections-per-host", 10)
                      .set("storage.cql.read-consistency-level", "ONE")
                      .set("storage.cql.write-consistency-level", "ONE")
                      .set("storage.cassandra.replication-factor", 1)
                      // TODO: Make these configurable from strongbox.yaml
                      // <--end
                      .set("storage.cassandra.compaction-strategy-class", SizeTieredCompactionStrategy.class)
                      .set("storage.cassandra.compaction-strategy-options", Collections.emptyList())
                      .set("storage.cassandra.frame-size-mb", 0) // thrift is
                                                                 // deprecated,
                                                                 // but can't be
                                                                 // 0 for
                                                                 // cassandra 3!
                      // disable sstable_compression prevent additional
                      // unnecessary IO (disk space is cheap)
                      .set("storage.cassandra.compression-type", null)
                      .set("storage.lock.wait-time", 5) // default 100 (probably
                                                        // ok for cluster, bad
                                                        // for single node)
                      .set("storage.lock.retries", 20)
                      .set("tx.log-tx", true)
                      .set("schema.default", "none")
                      .set("schema.constraints", true)
                      // enable metrics
                      .set("metrics.enabled", true)
                      .set("metrics.jmx.enabled", true);
    }

}
