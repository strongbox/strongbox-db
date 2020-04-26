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
 */
public class InMemoryJanusGraphServer implements EmbeddedDbServer
{
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedJanusGraphWithCassandraServer.class);

    private final JanusGraphConfiguration janusGraphProperties;
    private volatile JanusGraph janusGraph;

    public InMemoryJanusGraphServer(JanusGraphConfiguration janusGraphProperties)
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
            provideJanusGraphInstanceWithRetry();
        }
        catch (Exception e)
        {
            stop();
            throw new RuntimeException("Unable to start the embedded DB server!", e);
        }
    }

    private JanusGraph provideJanusGraphInstanceWithRetry()
        throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            try
            {
                return provideJanusGraphInstance();
            }
            catch (Exception e)
            {
                Thread.sleep(500);
                logger.info(String.format("Retry JanusGraph instance initialization with [%s] attempt...", i));
            }
        }

        return provideJanusGraphInstance();
    }

    private JanusGraph provideJanusGraphInstance()
        throws Exception
    {
        if (janusGraph != null)
        {
            return janusGraph;
        }

        JanusGraph janusGraphLocal = JanusGraphFactory.build()
                                                      .set("storage.backend", "inmemory")
                                                      .set("schema.default", "none")
                                                      .set("schema.constraints", true)
                                                      .open();

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

        logger.info("All embedded DB services stopped!");
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
