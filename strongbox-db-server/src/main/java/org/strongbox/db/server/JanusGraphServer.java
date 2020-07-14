package org.strongbox.db.server;

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 *
 */
public abstract class JanusGraphServer implements EmbeddedDbServer
{
    private static final Logger logger = LoggerFactory.getLogger(JanusGraphServer.class);

    private final JanusGraphConfiguration janusGraphProperties;
    private volatile JanusGraph janusGraph;

    public JanusGraphServer(JanusGraphConfiguration janusGraphProperties)
    {
        this.janusGraphProperties = janusGraphProperties;
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
        return JanusGraphFactory.open(configuration.getConfigLocation());
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
