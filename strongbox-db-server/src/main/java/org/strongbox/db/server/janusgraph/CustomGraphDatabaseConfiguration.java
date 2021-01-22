package org.strongbox.db.server.janusgraph;

import java.util.function.Supplier;

import org.janusgraph.diskstorage.Backend;
import org.janusgraph.diskstorage.configuration.BasicConfiguration;
import org.janusgraph.diskstorage.configuration.ModifiableConfiguration;
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.graphdb.database.idassigner.VertexIDAssigner;

/**
 * @author sbespalov
 */
public class CustomGraphDatabaseConfiguration extends GraphDatabaseConfiguration
{

    private final Supplier<String> idBlockQueueSupplier;

    public CustomGraphDatabaseConfiguration(GraphDatabaseConfiguration target, Supplier<String> idBlockQueueSupplier)
    {
        super(new CommonsConfiguration(target.getConfigurationAtOpen()),
              new ModifiableConfiguration(GraphDatabaseConfiguration.ROOT_NS,
                                          new CommonsConfiguration(target.getLocalConfiguration()),
                                          BasicConfiguration.Restriction.NONE),
              target.getUniqueGraphId(),
              target.getConfiguration());

        this.idBlockQueueSupplier = idBlockQueueSupplier;
    }

    @Override
    public VertexIDAssigner getIDAssigner(Backend backend)
    {
        return new TransactionalVertexIDAssigner(getConfiguration(), backend.getIDAuthority(), backend.getStoreFeatures(), idBlockQueueSupplier);
    }

}
