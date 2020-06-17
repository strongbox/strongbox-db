package org.strongbox.db.server;

import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphFactory.Builder;

/**
 * @author sbespalov
 */
public class InMemoryJanusGraphServer extends JanusGraphServer
{

    public InMemoryJanusGraphServer(JanusGraphConfiguration janusGraphProperties)
    {
        super(janusGraphProperties);
    }

    @Override
    protected Builder buildJanusGraph(JanusGraphConfiguration configuration)
    {
        return JanusGraphFactory.build()
                                .set("storage.backend", "inmemory")
                                .set("schema.default", "none")
                                .set("schema.constraints", true);

    }

}
