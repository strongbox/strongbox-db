package org.strongbox.db.server.cassandra;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.YamlConfigurationLoader;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.strongbox.db.server.CassandraEmbeddedConfiguration;

public class CassandraEmbeddedPropertiesLoader extends YamlConfigurationLoader
{

    private static CassandraEmbeddedConfiguration configuration;

    public static synchronized Class<CassandraEmbeddedPropertiesLoader> bootstrap(CassandraEmbeddedConfiguration configuration)
    {
        if (CassandraEmbeddedPropertiesLoader.configuration != null)
        {
            throw new IllegalStateException();
        }

        CassandraEmbeddedPropertiesLoader.configuration = configuration;

        return CassandraEmbeddedPropertiesLoader.class;
    }

    @Override
    public Config loadConfig()
        throws ConfigurationException
    {
        CassandraEmbeddedConfiguration configurationLocal;
        if ((configurationLocal = CassandraEmbeddedPropertiesLoader.configuration) == null)
        {
            throw new IllegalStateException();
        }

        Config config;
        try
        {
            config = super.loadConfig(new URL(configurationLocal.getConfigLocatoion()));
        }
        catch (MalformedURLException e)
        {
            throw new ConfigurationException(e.getMessage(), e);
        }

        String storageFolder = configuration.getStorageRoot();
        // ------------------------
        // Storage locations
        // ------------------------
        config.data_file_directories = new String[] { String.format("%s/cassandra/data", storageFolder) };
        config.saved_caches_directory = String.format("%s/cassandra/saved_caches", storageFolder);
        config.commitlog_directory = String.format("%s/cassandra/commitlog", storageFolder);
        // CommitLogSegments are moved to this directory on flush if
        // cdc_enabled: true and the segment contains
        // mutations for a CDC-enabled table. This should be placed on a
        // separate spindle than the data directories.
        // If not set, the default directory is $CASSANDRA_HOME/data/cdc_raw.
        config.cdc_raw_directory = String.format("%s/cassandra/cdc", storageFolder);
        config.hints_directory = String.format("%s/cassandra/hints", storageFolder);

        return config;
    }

}
