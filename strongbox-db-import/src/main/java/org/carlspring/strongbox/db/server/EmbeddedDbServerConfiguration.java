package org.carlspring.strongbox.db.server;

import org.janusgraph.core.JanusGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
    JanusGraph JanusGraph(JanusGraphServer server)
    {
        return server.getJanusGraph();
    }

    @Bean
    @ConfigurationProperties(prefix = "strongbox.db.janus-graph")
    JanusGraphConfiguration janusGraphConfiguration()
    {
        return new JanusGraphProperties();
    }

    @Bean
    CassandraEmbeddedConfiguration cassandraEmbeddedConfiguration(@Value("${strongbox.db.janus-graph.storage-root}") String storageRoot,
                                                                  JanusGraphConfiguration janusGraphConfiguration)
    {
        return CassandraEmbeddedProperties.getInstance(storageRoot,
                                                       janusGraphConfiguration.getStoragePort());
    }

}
