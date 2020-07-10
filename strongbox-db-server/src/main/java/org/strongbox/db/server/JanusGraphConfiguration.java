package org.strongbox.db.server;

import java.util.Collections;

import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.janusgraph.diskstorage.idmanagement.ConflictAvoidanceMode;

public interface JanusGraphConfiguration
{

    default Boolean getCacheDbCache() {
        return false;
    }
    
    // This is a percentage of the JVM memory - 0.4 means 40%
    // of -Xmx!
    default Integer cacheDbCacheSize() {
        return 0;
    }
    
    default Integer getCacheDbCacheCleanWait() {
        return 0; // can be 0 for local database
    }

    default Integer getCacheDbCacheTime() {
        return 60000;
    }

    default Integer getCacheTxCacheSize() {
        return 50000;
    }
    
    default Integer getCacheTxDirtySize() {
        return 1000;
    }
    
    default Integer getIdsBlockSize() {
       return 5000; // default: 10000
    }
    
    default Ingerger getIdsNumPartitions() {
        return 3; // default: 10
    }
    
    default Integer getIdsRenewTimeout() {
        return 300000; // default: 120000 ms
    }
    
    default Float getIdsRenewPercentage() {
        return  0.2f; // percentage; default 0.3
    }
        
    default String idsAuthorityConflictAvoidanceMode() {
        return "GLOBAL_AUTO";
    }
    
    default String getStorageBackend() {
        return "cql";
    }
    
    default String getStorageHostname() {
        return "127.0.0.1";
    }
    
    default Integer getStoragePort() {
        return 49142; 
    }
    
    default String getStorageUsername() {
        return "cassandra";
    }
    
    default String getStoragePassword() {
        return "cassandra";
    }
    
    // Whether JanusGraph should attempt to parallelize
    // storage operations
    default Boolean getStorageParallelBackendOps() {
        return true;
    }
    
    default String getStorageCqlKeyspace() {
        return "strongbox";
    }
    
    default Boolean getStorageCqlOnlyUseLocalConsistencyForSystemOperations() {
        return true;
    }
    
    default Integer getStorageCqlLocalCoreConnectionsPerHost() {
        return 1;
    }
    
    default Integer getStorageCqlLocalMaxConnectionsPerHost() {
        return 10;
    }
    
    default String getStorageCqlReadConsistencyLevel() {
        return "ONE";
    }
    
    default String getStorageCqlWriteConsistencyLevel() {
        return "ONE";
    }
    
    default Integer getStorageCassandraReplicationFactor() {
        return 1;
    }
    
    default storageCassandra.compaction-strategy-class", SizeTieredCompactionStrategy.class)
    storageCassandra.compaction-strategy-options", Collections.emptyList())
    storageCassandra.frame-size-mb", 0) // thrift is
                                               // deprecated,
                                               // but can't be
                                               // 0 for
                                               // cassandra 3!
    // disable sstable_compression prevent additional
    // unnecessary IO (disk space is cheap)
    storageCassandra.compression-type", null)
    storageLock.wait-time", 5) // default 100 (probably
                                      // ok for cluster, bad
                                      // for single node)
    storageLock.retries", 20)
    txLog-tx", true)
    // enable metrics
    metricsEnabled", true)
    metricsJmxEnabled", true);

}
