package org.strongbox.db.server;

import java.util.Collections;
import java.util.Objects;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.Config.CommitLogSync;
import org.apache.cassandra.config.Config.DiskFailurePolicy;
import org.apache.cassandra.config.ConfigurationLoader;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.exceptions.ConfigurationException;

public class CassandraEmbeddedProperties
        implements CassandraEmbeddedConfiguration
{

    private volatile static CassandraEmbeddedProperties instance;

    public static synchronized CassandraEmbeddedProperties getInstance(String storageFolder,
                                                                       int port)
    {
        if (instance != null)
        {
            return instance;
        }
        return instance = new CassandraEmbeddedProperties(storageFolder, port);
    }

    private final Config config;

    private CassandraEmbeddedProperties(String storageFolder,
                                        int port)
    {
        Objects.isNull(instance);

        Config configLocal = new Config();

        // ------------------------
        // Generic configuration
        // ------------------------
        configLocal.cluster_name = "Test Cluster";
        configLocal.snapshot_before_compaction = false;
        configLocal.auto_snapshot = false;

        // ------------------------
        // Security
        // ------------------------
        configLocal.authenticator = "PasswordAuthenticator";
        configLocal.authorizer = "AllowAllAuthorizer";
        configLocal.permissions_validity_in_ms = 2000;

        // ------------------------
        // Network
        // ------------------------
        configLocal.start_rpc = false; // disable thrift - it's deprecated and should not be used!
        configLocal.listen_address = "127.0.0.1";
        configLocal.storage_port = 7010;
        configLocal.ssl_storage_port = 7011;
        configLocal.start_native_transport = true;
        configLocal.native_transport_port = port;
        configLocal.native_transport_max_threads = 256;

        // Coordinator related
        configLocal.read_request_timeout_in_ms = 5000;
        configLocal.range_request_timeout_in_ms = 10000;
        configLocal.write_request_timeout_in_ms = 2000;
        configLocal.cas_contention_timeout_in_ms = 1000;
        configLocal.truncate_request_timeout_in_ms = 60000;
        configLocal.request_timeout_in_ms = 10000;
        configLocal.cross_node_timeout = false;

        // Addresses of hosts that are deemed contact points.
        // Cassandra nodes use this list of hosts to find each other and learn
        // the topology of the ring.  You must change this if you are running multiple nodes!
        //  - seeds is actually a comma-delimited list of addresses.
        //    Ex: "seeds: <ip1>,<ip2>,<ip3>"
        configLocal.seed_provider = new ParameterizedClass("org.apache.cassandra.locator.SimpleSeedProvider",
                                                           Collections.singletonMap("seeds", "127.0.0.1"));

        // ------------------------
        // Storage locations
        // ------------------------
        configLocal.data_file_directories = new String[]{ String.format("%s/cassandra/data", storageFolder) };
        configLocal.saved_caches_directory = String.format("%s/cassandra/saved_caches", storageFolder);
        configLocal.commitlog_directory = String.format("%s/cassandra/commitlog", storageFolder);
        // CommitLogSegments are moved to this directory on flush if cdc_enabled: true and the segment contains
        // mutations for a CDC-enabled table. This should be placed on a separate spindle than the data directories.
        // If not set, the default directory is $CASSANDRA_HOME/data/cdc_raw.
        configLocal.cdc_raw_directory = String.format("%s/cassandra/cdc", storageFolder);
        configLocal.hints_directory = String.format("%s/cassandra/hints", storageFolder);

        // ------------------------
        // Disk
        // ------------------------
        configLocal.disk_optimization_strategy = Config.DiskOptimizationStrategy.ssd;
        configLocal.disk_failure_policy = DiskFailurePolicy.stop;
        configLocal.trickle_fsync = true; // good on SSDs, probably bad on hdd
        configLocal.trickle_fsync_interval_in_kb = 10240; // default: 10240
        configLocal.concurrent_compactors = 2;
        configLocal.compaction_throughput_mb_per_sec = 128; // 128+ for SSD, 16 for hdd;
        // - Usually (16 × number_of_drives)
        configLocal.concurrent_reads = 32;
        // - Writes in Cassandra are rarely I/O bound, so the ideal number of concurrent writes depends on the number
        //   of CPU cores on the node. The recommended value is 8 × number_of_cpu_cores.
        configLocal.concurrent_writes = 16;
        // - Counter writes read the current values before incrementing and writing them back.
        //   The recommended value is (16 × number_of_drives).
        configLocal.concurrent_counter_writes = 32;
        // - Limit on the number of concurrent materialized view writes. Set this to the lesser of concurrent reads or
        //   concurrent writes, because there is a read involved in each materialized view write. (Default: 32)
        configLocal.concurrent_materialized_view_writes = 32;

        // - (Default 1024KB ) Total maximum throttle for replaying hints. Throttling is reduced proportionally to
        //   the number of nodes in the cluster.
        //configLocal.batchlog_replay_throttle_in_kb = 1024;

        // Cache and index settings
        // - (Default: 64) Granularity of the index of rows within a partition. For huge rows, decrease this setting to
        //   improve seek time. If you use key cache, be careful not to make this setting too large because key cache
        //   will be overwhelmed. If you're unsure of the size of the rows, it's best to use the default setting.
        configLocal.column_index_size_in_kb = 32; // default 64
        configLocal.column_index_cache_size_in_kb = 1024;

        // Memtable (in-memory structures where Cassandra buffers writes)
        // https://cassandra.apache.org/doc/latest/architecture/storage_engine.html#memtables
        configLocal.memtable_allocation_type = Config.MemtableAllocationType.offheap_objects;
        // - Smaller of number of disks or number of cores with a minimum of 2 and a maximum of 8
        //   If your data directories are backed by SSDs, increase this setting to the number of cores.
        configLocal.memtable_flush_writers = 2;
        // - The compaction process opens SSTables before they are completely written and uses them in place of
        //   the prior SSTables for any range previously written. This setting helps to smoothly transfer reads
        //   between the SSTables by reducing page cache churn and keeps hot rows hot.
        configLocal.sstable_preemptive_open_interval_in_mb = 64; // default 50

        // Enable / disable CDC functionality on a per-node basis. This modifies the logic used for write path allocation rejection
        // (standard: never reject. cdc: reject Mutation containing a CDC-enabled table if at space limit in cdc_raw_directory).
        configLocal.cdc_enabled = false;

        // The default option is “periodic” where writes may be ack'ed immediately and the CommitLog is simply synced
        // every commitlog_sync_period_in_ms milliseconds. What this means, is the commit is
        configLocal.commitlog_sync = CommitLogSync.periodic;
        configLocal.commitlog_sync_period_in_ms = 3000; // sync log every 5s.
        configLocal.commitlog_total_space_in_mb = 32;
        configLocal.commitlog_segment_size_in_mb = 32;
        configLocal.max_mutation_size_in_kb = (configLocal.commitlog_segment_size_in_mb * 1024) / 2;
        // configLocal.ideal_consistency_level = ConsistencyLevel.LOCAL_QUORUM; // available in cassandra 4

        // ------------------------
        // Replication
        // ------------------------

        // A partitioner determines how data is distributed across the nodes in the cluster (including replicas).
        // Basically, a partitioner is a function for deriving a token representing a row from its partition key,
        // typically by hashing. Each row of data is then distributed across the cluster by the value of the token.
        //
        // Default: Murmur3Partitioner using MurmurHash which uses 64-bit hashing function and allows for possible
        //          range of hash values is from -2^63 to +2^63-1.
        // WARNING: You cannot change the partitioner in existing clusters that use a different partitioner.
        //
        configLocal.partitioner = org.apache.cassandra.dht.Murmur3Partitioner.class.getName();

        // hinted_handoff is used to optimize cluster consistency process and anti-entropy (the synchronization of
        // replica data on nodes to ensure that the data is fresh) when a replica-owning node is not available to accept
        // the write operation (i.e. network issues/etc). This operation DOES NOT guarantee successful write operations,
        // except when a client application uses consistency level of `ANY`.
        // WARNING: This MUST be enabled for HA setups! It's currently disabled, because we're using single nodes setups.
        configLocal.hinted_handoff_enabled = false;

        // Endpoint snitch
        // This teaches Cassandra enough about your network's topology so it can route requests efficiently and spread
        // replicas by grouping machines nto `data centers` and `racks`.
        // WARNING: Switching this option CAN CAUSE DATA LOSS (read manual)
        // Default: SimpleSnitch (leave it as is for now)
        configLocal.endpoint_snitch = "SimpleSnitch";
        configLocal.dynamic_snitch_update_interval_in_ms = 100;
        configLocal.dynamic_snitch_reset_interval_in_ms = 600000;
        configLocal.dynamic_snitch_badness_threshold = 0.1;

        // Back-pressure settings # If enabled, the coordinator will apply the back-pressure strategy specified below to
        // each mutation sent to replicas, with the aim of reducing pressure on overloaded replicas.
        // Should be configured usually for cluster setup.
        configLocal.back_pressure_enabled = false;
        //configLocal.back_pressure_strategy = RateBasedBackPressure.withDefaultParams();

        // internode_compression controls whether traffic between nodes is compressed
        //  all  - all traffic is compressed
        //  dc   - traffic between nodes is compressed
        //  none - no compression (suitable for single nodes)
        configLocal.internode_compression = Config.InternodeCompression.none;

        // Wanr for GC pauses longer than 500ms (usually means heap is near limit)
        configLocal.gc_warn_threshold_in_ms = 500;

        this.config = configLocal;
    }

    public Integer getPort()
    {
        return config.native_transport_port;
    }

    @Override
    public String getCassandraConfigLoaderClassName()
    {
        return CassandraEmbeddedPropertiesLoader.class.getName();
    }

    public String getStorageFolder()
    {
        return config.data_file_directories[0].replace("/cassandra/data", "");
    }

    public static class CassandraEmbeddedPropertiesLoader
            implements ConfigurationLoader
    {

        @Override
        public Config loadConfig()
                throws ConfigurationException
        {
            Objects.nonNull(instance);

            return instance.config;
        }

    }

}