package org.strongbox.db.server;

import java.util.function.Supplier;

/**
 * @author sbespalov
 */
public class InMemoryJanusGraphServer extends JanusGraphServer
{

    public InMemoryJanusGraphServer(JanusGraphConfiguration janusGraphProperties,
                                    Supplier<String> idBlockQueueSupplier)
    {
        super(janusGraphProperties, idBlockQueueSupplier);
    }

}
