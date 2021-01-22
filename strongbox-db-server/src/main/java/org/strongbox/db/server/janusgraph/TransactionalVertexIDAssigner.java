package org.strongbox.db.server.janusgraph;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.janusgraph.core.VertexLabel;
import org.janusgraph.diskstorage.BackendException;
import org.janusgraph.diskstorage.IDAuthority;
import org.janusgraph.diskstorage.IDBlock;
import org.janusgraph.diskstorage.configuration.Configuration;
import org.janusgraph.diskstorage.keycolumnvalue.KeyRange;
import org.janusgraph.diskstorage.keycolumnvalue.StoreFeatures;
import org.janusgraph.graphdb.database.idassigner.IDBlockSizer;
import org.janusgraph.graphdb.database.idassigner.IDPool;
import org.janusgraph.graphdb.database.idassigner.VertexIDAssigner;
import org.janusgraph.graphdb.idmanagement.IDManager;
import org.janusgraph.graphdb.internal.InternalRelation;
import org.janusgraph.graphdb.internal.InternalVertex;

/**
 * Provides separate {@link VertexIDAssigner} instances for `idBlockQueue`s
 * which is usually supplied as part of a transaction. This allows for separate
 * {@link IDPool} for different application threads to eliminate the influence
 * of threads on each other.
 *
 * @author sbespalov
 */
public class TransactionalVertexIDAssigner extends VertexIDAssigner
{

    public static final String QUEUE_DEFAULT = "default";
    public static final String TX_RESOURCE_QUEUE = TransactionalVertexIDAssigner.class.getName() + ".queue";

    private Map<String, VertexIDAssigner> queueMap = new ConcurrentHashMap<>();
    private final Function<String, VertexIDAssigner> newInstanceFactory;
    private final Supplier<String> idBlockQueueSupplier;

    /**
     * This will create 3 groups of VertexIDAssigner - default, cron-job and super (since we're invoking the super constructor)
     */
    public TransactionalVertexIDAssigner(Configuration config,
                                         IDAuthority idAuthority,
                                         StoreFeatures idAuthFeatures,
                                         Supplier<String> idBlockQueueSupplier)
    {
        super(config, idAuthority, idAuthFeatures);
        newInstanceFactory = (queueId) -> new VertexIDAssigner(config, new ReadOnlyIDAuthority(idAuthority), idAuthFeatures);
        this.idBlockQueueSupplier = idBlockQueueSupplier;
    }

    @Override
    public IDManager getIDManager()
    {
        return super.getIDManager();
    }

    @Override
    public synchronized void close()
    {
        queueMap.values().forEach(VertexIDAssigner::close);
        // close any connections made by the VertexIDAssigner
        super.close();
    }

    @Override
    public void assignID(InternalRelation relation)
    {
        VertexIDAssigner currentIDAssigner = getCurrentIDAssigner();
        if (currentIDAssigner == this)
        {
            super.assignID(relation);
        }
        else
        {
            currentIDAssigner.assignID(relation);
        }
    }

    @Override
    public void assignID(InternalVertex vertex,
                         VertexLabel label)
    {
        VertexIDAssigner currentIDAssigner = getCurrentIDAssigner();
        if (currentIDAssigner == this)
        {
            super.assignID(vertex, label);
        }
        else
        {
            currentIDAssigner.assignID(vertex, label);
        }
    }

    @Override
    public void assignIDs(Iterable<InternalRelation> addedRelations)
    {
        VertexIDAssigner currentIDAssigner = getCurrentIDAssigner();
        if (currentIDAssigner == this)
        {
            super.assignIDs(addedRelations);
        }
        else
        {
            currentIDAssigner.assignIDs(addedRelations);
        }
    }

    protected VertexIDAssigner getCurrentIDAssigner()
    {

        return getCurrentQueueId().map((id) -> queueMap.computeIfAbsent(id, newInstanceFactory)).orElse(this);
    }

    private Optional<String> getCurrentQueueId()
    {
        return Optional.ofNullable(idBlockQueueSupplier.get());
    }

    /**
     * This wrapper class needed to avoid
     * {@link IDAuthority#setIDBlockSizer(IDBlockSizer)} call within
     * {@link VertexIDAssigner} constructor which fails for additional
     * {@link VertexIDAssigner} instances attached to `idBlockQueue`s.
     *
     * @author sbespalov
     */
    private class ReadOnlyIDAuthority implements IDAuthority
    {

        final IDAuthority target;

        public ReadOnlyIDAuthority(IDAuthority target)
        {
            this.target = target;
        }

        public IDBlock getIDBlock(int partition,
                                  int idNamespace,
                                  Duration timeout)
            throws BackendException
        {
            return target.getIDBlock(partition, idNamespace, timeout);
        }

        public List<KeyRange> getLocalIDPartition()
            throws BackendException
        {
            return target.getLocalIDPartition();
        }

        public void setIDBlockSizer(IDBlockSizer sizer)
        {

        }

        public void close()
            throws BackendException
        {
        }

        public String getUniqueID()
        {
            return target.getUniqueID();
        }

        public boolean supportsInterruption()
        {
            return target.supportsInterruption();
        }

    }
}
