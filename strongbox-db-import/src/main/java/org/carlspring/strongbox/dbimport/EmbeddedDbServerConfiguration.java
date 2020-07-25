package org.carlspring.strongbox.dbimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.carlspring.strongbox.db.schema.StrongboxSchema;
import org.janusgraph.core.JanusGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
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
        return new JanusGraphWithEmbeddedCassandra(cassandraConfiguration, janusGraphConfiguration);
    }

    private void extractConfigurationFile(String rootFolder,
                                          String configFileName)
        throws IOException
    {
        try (InputStream in = JanusGraphWithEmbeddedCassandra.class.getResourceAsStream(String.format("/etc/conf/%s", configFileName)))
        {
            Path configRoot = Paths.get(rootFolder).resolve("etc").resolve("conf");
            Files.createDirectories(configRoot);
            Files.copy(in,
                       configRoot.resolve(configFileName),
                       StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Bean
    JanusGraph janusGraph(JanusGraphServer server)
        throws Exception
    {
        return new StrongboxSchema().createSchema(server.getJanusGraph());
    }

    @Bean
    JanusGraphProperties dbImportJanusGraphProperties(@Value("${strongbox.dbimport.root}") String dbImportRoot)
        throws IOException
    {
        extractConfigurationFile(dbImportRoot, "janusgraph-cassandra.properties");
        Path configFilePath = Paths.get(dbImportRoot).resolve("etc").resolve("conf").resolve("janusgraph-cassandra.properties");

        return new JanusGraphProperties( String.format("file:%s", configFilePath.toAbsolutePath().toString()));
    }

    @Bean
    CassandraEmbeddedProperties dbImportCassandraEmbeddedProperties(@Value("${strongbox.dbimport.root}") String dbImportRoot)
        throws IOException
    {
        extractConfigurationFile(dbImportRoot, "cassandra.yaml");
        Path rootPath = Paths.get(dbImportRoot).toAbsolutePath();
        Path configFilePath = rootPath.resolve("etc").resolve("conf").resolve("cassandra.yaml");
        Path storageRootPath = rootPath.resolve("db");

        return new CassandraEmbeddedProperties(storageRootPath.toString(), String.format("file:%s", configFilePath.toString()));
    }

}
