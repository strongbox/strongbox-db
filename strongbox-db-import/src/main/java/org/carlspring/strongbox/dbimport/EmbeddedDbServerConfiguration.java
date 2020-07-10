package org.carlspring.strongbox.dbimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.carlspring.strongbox.db.schema.StrongboxSchema;
import org.janusgraph.core.JanusGraph;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.strongbox.db.server.CassandraEmbeddedConfiguration;
import org.strongbox.db.server.CassandraEmbeddedProperties;
import org.strongbox.db.server.JanusGraphConfiguration;
import org.strongbox.db.server.JanusGraphProperties;
import org.strongbox.db.server.JanusGraphServer;
import org.strongbox.db.server.JanusGraphWithEmbeddedCassandra;

/**
 * @author Przemyslaw Fusik
 * @author sbespalov
 */
@Configuration
@ConfigurationPropertiesScan
class EmbeddedDbServerConfiguration
{

    @Bean
    JanusGraphServer embeddedDbServer(CassandraEmbeddedConfiguration cassandraConfiguration,
                                      JanusGraphConfiguration janusGraphConfiguration)
        throws IOException
    {
        try (InputStream in = JanusGraphWithEmbeddedCassandra.class.getResourceAsStream("/etc/conf/cassandra.yaml"))
        {
            Path storageRoot = Paths.get(cassandraConfiguration.getStorageRoot())
                                    .resolve("..")
                                    .resolve("etc")
                                    .resolve("conf");
            Files.createDirectories(storageRoot);
            Files.copy(in,
                       storageRoot.resolve("cassandra.yaml"),
                       StandardCopyOption.REPLACE_EXISTING);
        }

        return new JanusGraphWithEmbeddedCassandra(cassandraConfiguration, janusGraphConfiguration);
    }

    @Bean
    JanusGraph janusGraph(JanusGraphServer server)
        throws Exception
    {
        return new StrongboxSchema().createSchema(server.getJanusGraph());
    }

    @Bean
    @ConfigurationProperties(prefix = "strongbox.db.janusgraph.storage")
    JanusGraphConfiguration janusGraphConfiguration()
    {
        return new JanusGraphProperties();
    }

    @ConstructorBinding
    @ConfigurationProperties(prefix = "strongbox.db.cassandra")
    public static class DbImportCassandraEmbeddedProperties extends CassandraEmbeddedProperties
    {

        public DbImportCassandraEmbeddedProperties(String storageRoot,
                                                   String configLocatoion)
        {
            super(storageRoot, configLocatoion);
        }

    }
}
