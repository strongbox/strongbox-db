package org.strongbox.db.server;

import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 */
public class JanusGraphWithRemoteCassandra extends JanusGraphServer
{

    private static final Logger logger = LoggerFactory.getLogger(JanusGraphWithEmbeddedCassandra.class);

    public JanusGraphWithRemoteCassandra(JanusGraphConfiguration janusGraphProperties)
    {
        super(janusGraphProperties);
    }

    @Override
    protected JanusGraph provideJanusGraphInstance()
        throws Exception
    {
        for (int i = 0; i < 20; i++)
        {
            try
            {
                return super.provideJanusGraphInstance();
            }
            catch (Exception e)
            {
                Thread.sleep(500);
                logger.info(String.format("Retry JanusGraph instance initialization with [%s] attempt...", i));
            }
        }

        return super.provideJanusGraphInstance();
    }

}
