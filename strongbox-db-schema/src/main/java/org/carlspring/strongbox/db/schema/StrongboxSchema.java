package org.carlspring.strongbox.db.schema;

import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_GROUP_HAS_ARTIFACTS;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_TAGS;
import static org.carlspring.strongbox.db.schema.Edges.REMOTE_ARTIFACT_INHERIT_ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_ID_GROUP;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_TAG;
import static org.carlspring.strongbox.db.schema.Vertices.GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.RAW_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.REMOTE_ARTIFACT;
import static org.janusgraph.core.Multiplicity.MANY2ONE;
import static org.janusgraph.core.Multiplicity.MULTI;
import static org.janusgraph.core.Multiplicity.ONE2MANY;
import static org.janusgraph.core.Multiplicity.ONE2ONE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.EdgeLabelMaker;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.JanusGraphSchemaType;
import org.janusgraph.core.schema.PropertyKeyMaker;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrongboxSchema
{

    private static final Logger logger = LoggerFactory.getLogger(StrongboxSchema.class);

    public void createSchema(JanusGraph jg)
        throws InterruptedException
    {
        JanusGraphManagement jgm = jg.openManagement();
        try
        {
            applySchemaChanges(jgm);
            jgm.commit();
        }
        catch (Exception e)
        {
            logger.error("Failed to apply schema changes.", e);
            jgm.rollback();
            throw new RuntimeException("Failed to apply schema changes.", e);
        }

        jgm = jg.openManagement();
        Set<String> indexes;
        try
        {
            indexes = createIndexes(jg, jgm);
            jgm.commit();
        }
        catch (Exception e)
        {
            logger.error("Failed to create indexes.", e);
            jgm.rollback();
            throw new RuntimeException("Failed to create indexes.", e);
        }

        for (String janusGraphIndex : indexes)
        {
            logger.info(String.format("Wait index [%s] to be registered.", janusGraphIndex));
            ManagementSystem.awaitGraphIndexStatus(jg, janusGraphIndex).call();
        }

        jgm = jg.openManagement();
        try
        {
            enableIndexes(jgm, indexes);
            jgm.commit();
        }
        catch (Exception e)
        {
            logger.error("Failed to enable indexes.", e);
            jgm.rollback();
            throw new RuntimeException("Failed to enable indexes.", e);
        }

        jgm = jg.openManagement();
        try
        {
            logger.info(String.format("Schema: %n%s", jgm.printSchema()));
        }
        finally
        {
            jgm.rollback();
        }
    }

    protected void enableIndexes(JanusGraphManagement jgm,
                                 Set<String> indexes)
        throws InterruptedException,
        ExecutionException
    {
        for (String janusGraphIndex : indexes)
        {
            logger.info(String.format("Enabling index [%s].", janusGraphIndex));
            jgm.updateIndex(jgm.getGraphIndex(janusGraphIndex), SchemaAction.ENABLE_INDEX).get();
        }
    }

    protected Set<String> createIndexes(JanusGraph jg,
                                        JanusGraphManagement jgm)
        throws InterruptedException
    {
        Set<String> result = new HashSet<>();

        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(REMOTE_ARTIFACT),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(RAW_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_TAG),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              true,
                              jgm.getPropertyKey("uuid")).ifPresent(result::add);
        
        // TODO: Artifact index by storageId+repositoryId+path
        // buildIndexIfNecessary(jgm,
        // Edge.class,
        // jgm.getEdgeLabel(ARTIFACT_HAS_ARTIFACT_COORDINATES),
        // true,
        // jgm.getPropertyKey("storageId"),
        // jgm.getPropertyKey("repositoryId"),
        // jgm.getPropertyKey("path")).ifPresent(result::add);

        return result;
    }

    private void applySchemaChanges(JanusGraphManagement jgm)
    {
        // Properties
        makePropertyKeyIfDoesNotExist(jgm, "uuid", String.class);
        makePropertyKeyIfDoesNotExist(jgm, "storageId", String.class);
        makePropertyKeyIfDoesNotExist(jgm, "repositoryId", String.class);
        makePropertyKeyIfDoesNotExist(jgm, "filenames", String.class, Cardinality.SET);
        makePropertyKeyIfDoesNotExist(jgm, "checksums", String.class, Cardinality.SET);

        // Vertices
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT);
        makeVertexLabelIfDoesNotExist(jgm, REMOTE_ARTIFACT);
        makeVertexLabelIfDoesNotExist(jgm, GENERIC_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, RAW_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_TAG);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_ID_GROUP);

        // Edges
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_HAS_ARTIFACT_COORDINATES, MANY2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_HAS_TAGS, MULTI);
        makeEdgeLabelIfDoesNotExist(jgm, REMOTE_ARTIFACT_INHERIT_ARTIFACT, ONE2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES, ONE2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_GROUP_HAS_ARTIFACTS, ONE2MANY);
        
    }

    private Optional<String> buildIndexIfNecessary(final JanusGraphManagement jgm,
                                                   final Class<? extends Element> elementType,
                                                   final JanusGraphSchemaType schemaType,
                                                   final boolean unique,
                                                   final PropertyKey... properties)
    {
        final String name = schemaType.name() + "By"
                + Arrays.stream(properties)
                        .map(PropertyKey::name)
                        .map(StringUtils::capitalize)
                        .reduce((p1,
                                 p2) -> p1 + "And" + p2)
                        .get();
        if (jgm.containsGraphIndex(name))
        {
            return Optional.empty();
        }

        IndexBuilder indexBuilder = jgm.buildIndex(name, elementType)
                                       .indexOnly(schemaType);
        Arrays.stream(properties).forEach(indexBuilder::addKey);

        if (unique)
        {
            indexBuilder = indexBuilder.unique();
        }

        return Optional.of(indexBuilder.buildCompositeIndex().name());
    }

    private void makeEdgeLabelIfDoesNotExist(final JanusGraphManagement jgm,
                                             final String name,
                                             final Multiplicity multiplicity)
    {
        if (jgm.containsEdgeLabel(name))
        {
            return;
        }
        EdgeLabelMaker edgeLabelMaker = jgm.makeEdgeLabel(name);
        if (multiplicity != null)
        {
            edgeLabelMaker = edgeLabelMaker.multiplicity(multiplicity);
        }

        edgeLabelMaker.make();
    }

    private void makeVertexLabelIfDoesNotExist(final JanusGraphManagement jgm,
                                               final String name)
    {
        if (jgm.containsVertexLabel(name))
        {
            return;
        }
        jgm.makeVertexLabel(name).make();
    }

    private void makePropertyKeyIfDoesNotExist(final JanusGraphManagement jgm,
                                               final String name,
                                               final Class<?> dataType)
    {
        makePropertyKeyIfDoesNotExist(jgm, name, dataType, null);
    }

    private void makePropertyKeyIfDoesNotExist(final JanusGraphManagement jgm,
                                               final String name,
                                               final Class<?> dataType,
                                               final Cardinality cardinality)
    {
        if (jgm.containsPropertyKey(name))
        {
            return;
        }

        PropertyKeyMaker propertyKeyMaker = jgm.makePropertyKey(name).dataType(dataType);
        if (cardinality != null)
        {
            propertyKeyMaker = propertyKeyMaker.cardinality(cardinality);
        }
        propertyKeyMaker.make();

    }

}
