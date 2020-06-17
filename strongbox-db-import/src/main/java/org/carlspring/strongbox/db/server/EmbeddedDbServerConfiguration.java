package org.carlspring.strongbox.db.server;

import org.janusgraph.core.JanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.strongbox.db.server.CassandraEmbeddedConfiguration;
import org.strongbox.db.server.CassandraEmbeddedProperties;
import org.strongbox.db.server.EmbeddedDbServer;
import org.strongbox.db.server.JanusGraphWithEmbeddedCassandra;
import org.strongbox.db.server.JanusGraphConfiguration;
import org.strongbox.db.server.JanusGraphProperties;

/**
 * @author Przemyslaw Fusik
 * @author sbespalov
 */
@Configuration
class EmbeddedDbServerConfiguration
{

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedDbServerConfiguration.class);

    @Bean
    EmbeddedDbServer embeddedDbServer(CassandraEmbeddedConfiguration cassandraConfiguration,
                                      JanusGraphConfiguration janusGraphConfiguration)
    {
        return new JanusGraphWithEmbeddedCassandra(cassandraConfiguration, janusGraphConfiguration);
    }

    @Bean
    JanusGraph JanusGraph(EmbeddedDbServer server)
    {
        return ((JanusGraphWithEmbeddedCassandra) server).getJanusGraph();
    }

    @Bean
    @ConfigurationProperties(prefix = "strongbox.db.janus-graph")
    JanusGraphConfiguration janusGraphConfiguration()
    {
        return new JanusGraphProperties();
    }

    @Bean
    CassandraEmbeddedConfiguration cassandraEmbeddedConfiguration(JanusGraphConfiguration janusGraphConfiguration)
    {
        return CassandraEmbeddedProperties.getInstance(janusGraphConfiguration.getStorageRoot(),
                                                       janusGraphConfiguration.getStoragePort());
    }

}
