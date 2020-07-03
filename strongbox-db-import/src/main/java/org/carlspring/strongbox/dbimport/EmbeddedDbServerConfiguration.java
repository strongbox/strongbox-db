package org.carlspring.strongbox.dbimport;

import org.carlspring.strongbox.db.schema.StrongboxSchema;
import org.janusgraph.core.JanusGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
class EmbeddedDbServerConfiguration
{

    @Bean
    JanusGraphServer embeddedDbServer(CassandraEmbeddedConfiguration cassandraConfiguration,
                                      JanusGraphConfiguration janusGraphConfiguration)
    {
        return new JanusGraphWithEmbeddedCassandra(cassandraConfiguration, janusGraphConfiguration);
    }

    @Bean
    JanusGraph janusGraph(JanusGraphServer server)
        throws Exception
    {
        return new StrongboxSchema().createSchema(server.getJanusGraph());
    }

    @Bean
    @ConfigurationProperties(prefix = "strongbox.db.janus-graph")
    JanusGraphConfiguration janusGraphConfiguration()
    {
        return new JanusGraphProperties();
    }

    @ConstructorBinding
    @ConfigurationProperties(prefix = "strongbox.db.cassandra")
    public static class DbImportCassandraEmbeddedProperties extends CassandraEmbeddedProperties {

        public DbImportCassandraEmbeddedProperties(@Value("${strongbox.db.janusgraph.storage.root}") String storageRoot,
                                                   int port,
                                                   String configLocatoion)
        {
            super(storageRoot, port, configLocatoion);
        }
        
    }
}
