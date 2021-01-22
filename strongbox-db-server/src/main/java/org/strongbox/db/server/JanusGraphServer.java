package org.strongbox.db.server;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.diskstorage.configuration.ReadConfiguration;
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.graphdb.configuration.builder.GraphDatabaseConfigurationBuilder;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strongbox.db.server.janusgraph.CustomGraphDatabaseConfiguration;

/**
 * @author sbespalov
 *
 */
public abstract class JanusGraphServer implements EmbeddedDbServer
{
    private static final Logger logger = LoggerFactory.getLogger(JanusGraphServer.class);

    private final JanusGraphConfiguration janusGraphProperties;
    private final Supplier<String> idBlockQueueSupplier;
    private volatile JanusGraph janusGraph;

    public JanusGraphServer(JanusGraphConfiguration janusGraphProperties, Supplier<String> idBlockQueueSupplier)
    {
        this.janusGraphProperties = janusGraphProperties;
        this.idBlockQueueSupplier = idBlockQueueSupplier;
    }

    public JanusGraph getJanusGraph()
    {
        return janusGraph;
    }

    @PostConstruct
    public synchronized void start()
        throws Exception
    {
        try
        {
            provideJanusGraphInstance();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("Unable to start the embedded DB server!", e);
        }
    }

    protected JanusGraph provideJanusGraphInstance()
        throws Exception
    {
        if (janusGraph != null)
        {
            return janusGraph;
        }

        JanusGraph janusGraphLocal = buildJanusGraph(janusGraphProperties);

        try
        {
            Method removeHookMethod = StandardJanusGraph.class.getDeclaredMethod("removeHook");
            removeHookMethod.setAccessible(true);
            removeHookMethod.invoke(janusGraphLocal);
        }
        catch (Exception e)
        {
            janusGraphLocal.close();
            throw e;
        }

        return this.janusGraph = janusGraphLocal;
    }

    protected JanusGraph buildJanusGraph(JanusGraphConfiguration configuration)
    {
        String configLocation = configuration.getConfigLocation();
        try
        {
            URL configLocationUrl = new URL(configLocation);
            ReadConfiguration janusGraphLocalConfig = new CommonsConfiguration(new PropertiesConfiguration(configLocationUrl));
            GraphDatabaseConfigurationBuilder configBuilder = new GraphDatabaseConfigurationBuilder();
            GraphDatabaseConfiguration janusGraphDbConfig = configBuilder.build(janusGraphLocalConfig);

            return new StandardJanusGraph(new CustomGraphDatabaseConfiguration(janusGraphDbConfig, idBlockQueueSupplier));
        }
        catch (MalformedURLException|ConfigurationException e)
        {
            throw new RuntimeException(String.format("Invalid configuration [%s].", configLocation), e);
        }
    }

    @PreDestroy
    @Override
    public synchronized void stop()
        throws Exception
    {
        try
        {
            closeJanusGraph();
        }
        catch (Exception e)
        {
            logger.error("Failed to close JanusGraph", e);
        }

        logger.info("JanusGraph instance stopped!");
    }

    private void closeJanusGraph()
    {
        if (janusGraph == null)
        {
            logger.info("Skip closing JanusGraph..");
            return;
        }
        logger.info("Shutting JanusGraph instance..");
        try
        {
            janusGraph.close();
        }
        finally
        {
            janusGraph = null;
        }
    }

}
