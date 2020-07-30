package org.carlspring.strongbox.db.schema.migration;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 */
public class JanusgraphChangelogStorage implements ChangelogStorage
{

    private static final Logger logger = LoggerFactory.getLogger(JanusgraphChangelogStorage.class);

    private final JanusGraph jg;

    public JanusgraphChangelogStorage(JanusGraph jg)
    {
        this.jg = jg;
    }

    @Override
    public void init()
    {
        JanusGraphManagement jgm = jg.openManagement();
        try
        {
            initSchema(jgm);
            jgm.commit();
        }
        catch (Throwable e)
        {
            jgm.rollback();
            throw new RuntimeException("Failed to init schema storage.", e);
        }
    }

    private void initSchema(JanusGraphManagement jgm)
    {
        logger.info("Check the changelog storage schema to be initialized.");
        VertexLabel schemaVersionLabel = Optional.ofNullable(jgm.getVertexLabel(VERTEX_SCHEMA_VERSION))
                                                 .orElseGet(() -> jgm.makeVertexLabel(VERTEX_SCHEMA_VERSION).make());
        EdgeLabel changesetLabel = Optional.ofNullable(jgm.getEdgeLabel(EDGE_CHANGESET))
                                           .orElseGet(() -> jgm.makeEdgeLabel(EDGE_CHANGESET).make());
        if (schemaVersionLabel.mappedConnections().isEmpty())
        {
            jgm.addConnection(changesetLabel,
                              schemaVersionLabel,
                              schemaVersionLabel);
        }

        if (jgm.getPropertyKey(PROPERTY_VERSION_VALUE) == null)
        {
            PropertyKey property = jgm.makePropertyKey(PROPERTY_VERSION_VALUE).dataType(String.class).make();
            jgm.setConsistency(property, ConsistencyModifier.LOCK);
            jgm.addProperties(schemaVersionLabel, property);
        }
        if (jgm.getPropertyKey(PROPERTY_APPLY_DATE) == null)
        {
            PropertyKey property = jgm.makePropertyKey(PROPERTY_APPLY_DATE).dataType(Date.class).make();
            jgm.addProperties(changesetLabel, property);
        }
        if (jgm.getPropertyKey(PROPERTY_AUTHOR) == null)
        {
            PropertyKey property = jgm.makePropertyKey(PROPERTY_AUTHOR).dataType(String.class).make();
            jgm.addProperties(changesetLabel, property);
        }
        if (jgm.getPropertyKey(PROPERTY_CHANGESET_NAME) == null)
        {
            PropertyKey property = jgm.makePropertyKey(PROPERTY_CHANGESET_NAME).dataType(String.class).make();
            jgm.addProperties(changesetLabel, property);
        }
    }

    @Override
    public ChangesetVersion getSchemaVersion()
    {
        GraphTraversalSource g = jg.traversal();
        try
        {
            ChangesetVersion version = fetchSchemaVersion(g);
            g.tx().commit();
            return version;
        }
        catch (Throwable e)
        {
            g.tx().rollback();
            throw new RuntimeException("Failed to get stored schema version.", e);
        }
    }

    private ChangesetVersion fetchSchemaVersion(GraphTraversalSource g)
    {
        GraphTraversal<Vertex, Vertex> t = g.V()
                                            .hasLabel(VERTEX_SCHEMA_VERSION)
                                            .not(__.inE(EDGE_CHANGESET))
                                            .until(__.not(__.out(EDGE_CHANGESET)))
                                            .repeat(__.out());
        if (t.hasNext())
        {
            return new ChangesetVersionValue((String) t.next().property(PROPERTY_VERSION_VALUE).value());
        }

        return g.addV(VERTEX_SCHEMA_VERSION)
                .property(PROPERTY_VERSION_VALUE, ChangesetVersionValue.ZERO.getVersion())
                .map(v -> new ChangesetVersionValue((String) v.get().property(PROPERTY_VERSION_VALUE).value()))
                .next();
    }

    public Changeset prepare(Changeset changeset)
    {
        return new StoredChangeset(changeset);
    }

    @Override
    public void upgrade(Changeset version)
    {
        ChangesetVersion currentVersion = getSchemaVersion();
        GraphTraversalSource g = jg.traversal();
        try
        {
            upgrade(g, currentVersion, version);
            g.tx().commit();
        }
        catch (Exception e)
        {
            g.tx().rollback();
            throw new RuntimeException(String.format("Failed to upgrade schema version from [%s] to [%s]", currentVersion, version),
                    e);
        }
    }

    private void upgrade(GraphTraversalSource g,
                         ChangesetVersion from,
                         Changeset to)
    {
        GraphTraversal<Vertex, Edge> updateSchemaVersion = g.V()
                                                            .hasLabel(VERTEX_SCHEMA_VERSION)
                                                            .has(PROPERTY_VERSION_VALUE, from.getVersion())
                                                            .addE(EDGE_CHANGESET)
                                                            .property(PROPERTY_APPLY_DATE, new Date())
                                                            .property(PROPERTY_CHANGESET_NAME, to.getName())
                                                            .property(PROPERTY_AUTHOR, to.getAuthor())
                                                            .to(__.addV(VERTEX_SCHEMA_VERSION)
                                                                  .property(PROPERTY_VERSION_VALUE, to.getVersion()));
        if (updateSchemaVersion.hasNext())
        {
            updateSchemaVersion.next();
        }
        else
        {
            g.addV(VERTEX_SCHEMA_VERSION).property(PROPERTY_VERSION_VALUE, to.getVersion()).next();
        }
    }

    /**
     * @author sbespalov
     */
    public class StoredChangeset implements Changeset
    {

        private final Changeset target;

        public StoredChangeset(Changeset target)
        {
            this.target = target;
        }

        public String getName()
        {
            return target.getName();
        }

        public String getAuthor()
        {
            return target.getAuthor();
        }

        public String getVersion()
        {
            return target.getVersion();
        }

        public void applySchemaChanges(JanusGraphManagement jgm)
        {
            target.applySchemaChanges(jgm);
        }

        public int compareTo(ChangesetVersion other)
        {
            return target.compareTo(other);
        }

        public Set<String> createVertexIndexes(JanusGraphManagement jgm)
        {
            return target.createVertexIndexes(jgm);
        }

        public Map<String, String> createRelationIndexes(JanusGraphManagement jgm)
        {
            return target.createRelationIndexes(jgm);
        }

        @Override
        public void applyGraphChanges(GraphTraversalSource g)
        {
            target.applyGraphChanges(g);
            upgrade(g, fetchSchemaVersion(g), target);
        }

    }

}
