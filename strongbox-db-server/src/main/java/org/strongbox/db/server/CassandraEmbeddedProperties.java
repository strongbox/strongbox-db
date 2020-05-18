package org.strongbox.db.server;

import java.util.Collections;
import java.util.Objects;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.Config.CommitLogSync;
import org.apache.cassandra.config.Config.DiskFailurePolicy;
import org.apache.cassandra.config.ConfigurationLoader;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.exceptions.ConfigurationException;

public class CassandraEmbeddedProperties implements CassandraEmbeddedConfiguration
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
        configLocal.cluster_name = "Test Cluster";
        configLocal.hinted_handoff_enabled = true;
        configLocal.max_hint_window_in_ms = 10800000; // 3 hours
        configLocal.hinted_handoff_throttle_in_kb = 1024;
        configLocal.max_hints_delivery_threads = 2;
        configLocal.hints_directory = String.format("%s/cassandra/hints", storageFolder);
        configLocal.authenticator = "PasswordAuthenticator";
        configLocal.authorizer = "AllowAllAuthorizer";
        configLocal.permissions_validity_in_ms = 2000;
        configLocal.partitioner = "org.apache.cassandra.dht.Murmur3Partitioner";
        configLocal.data_file_directories = new String[] { String.format("%s/cassandra/data", storageFolder) };
        configLocal.commitlog_directory = String.format("%s/cassandra/commitlog", storageFolder);
        configLocal.cdc_raw_directory = String.format("%s/cassandra/cdc", storageFolder);
        configLocal.disk_failure_policy = DiskFailurePolicy.stop;
        configLocal.key_cache_save_period = 14400;
        configLocal.row_cache_size_in_mb = 0;
        configLocal.row_cache_save_period = 0;
        configLocal.saved_caches_directory = String.format("%s/cassandra/saved_caches", storageFolder);
        configLocal.commitlog_sync = CommitLogSync.periodic;
        configLocal.commitlog_sync_period_in_ms = 10000;
        configLocal.commitlog_segment_size_in_mb = 8;
        configLocal.max_mutation_size_in_kb = 4096;
        configLocal.seed_provider = new ParameterizedClass("org.apache.cassandra.locator.SimpleSeedProvider",
                Collections.singletonMap("seeds", "127.0.0.1"));
        configLocal.concurrent_reads = 32;
        configLocal.concurrent_writes = 32;
        configLocal.trickle_fsync = false;
        configLocal.trickle_fsync_interval_in_kb = 10240;
        configLocal.storage_port = 7010;
        configLocal.ssl_storage_port = 7011;
        configLocal.listen_address = "127.0.0.1";

        configLocal.start_native_transport = true;
        configLocal.native_transport_port = port;

        configLocal.incremental_backups = false;
        configLocal.snapshot_before_compaction = false;
        configLocal.auto_snapshot = false;
        configLocal.column_index_size_in_kb = 64;
        configLocal.compaction_throughput_mb_per_sec = 16;
        configLocal.read_request_timeout_in_ms = 5000;
        configLocal.range_request_timeout_in_ms = 10000;
        configLocal.write_request_timeout_in_ms = 2000;
        configLocal.cas_contention_timeout_in_ms = 1000;
        configLocal.truncate_request_timeout_in_ms = 60000;
        configLocal.request_timeout_in_ms = 10000;
        configLocal.cross_node_timeout = false;
        configLocal.endpoint_snitch = "SimpleSnitch";
        configLocal.dynamic_snitch_update_interval_in_ms = 100;
        configLocal.dynamic_snitch_reset_interval_in_ms = 600000;
        configLocal.dynamic_snitch_badness_threshold = 0.1;

        this.config = configLocal;
    }

    public Integer getPort()
    {
        return config.native_transport_port;
    }

    @Override
    public String getCassandraConfigLoaderClassName()
    {
        return "org.strongbox.db.server.CassandraEmbeddedProperties$CassandraEmbeddedPropertiesLoader";
    }

    public String getStorageFolder()
    {
        return config.data_file_directories[0].replace("/cassandra/data", "");
    }

    public static class CassandraEmbeddedPropertiesLoader implements ConfigurationLoader
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