package org.carlspring.strongbox.db.schema;

import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_GROUP_HAS_ARTIFACTS;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_TAGS;
import static org.carlspring.strongbox.db.schema.Edges.REMOTE_ARTIFACT_INHERIT_ARTIFACT;
import static org.carlspring.strongbox.db.schema.Properties.CACHED;
import static org.carlspring.strongbox.db.schema.Properties.CHECKSUMS;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ABI;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ARTIFACT_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_BUILD;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_CLASSIFIER;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_DISTRIBUTION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_EXTENSION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_GROUP_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_NAME;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PACKAGING;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PATH;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PLATFORM;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_SCOPE;
import static org.carlspring.strongbox.db.schema.Properties.CREATED;
import static org.carlspring.strongbox.db.schema.Properties.DOWNLOAD_COUNT;
import static org.carlspring.strongbox.db.schema.Properties.ENABLED;
import static org.carlspring.strongbox.db.schema.Properties.FILE_NAMES;
import static org.carlspring.strongbox.db.schema.Properties.LAST_UPDATED;
import static org.carlspring.strongbox.db.schema.Properties.LAST_USED;
import static org.carlspring.strongbox.db.schema.Properties.NAME;
import static org.carlspring.strongbox.db.schema.Properties.PASSWORD;
import static org.carlspring.strongbox.db.schema.Properties.REPOSITORY_ID;
import static org.carlspring.strongbox.db.schema.Properties.ROLES;
import static org.carlspring.strongbox.db.schema.Properties.SECURITY_TOKEN_KEY;
import static org.carlspring.strongbox.db.schema.Properties.SIZE_IN_BYTES;
import static org.carlspring.strongbox.db.schema.Properties.SOURCE_ID;
import static org.carlspring.strongbox.db.schema.Properties.STORAGE_ID;
import static org.carlspring.strongbox.db.schema.Properties.UUID;
import static org.carlspring.strongbox.db.schema.Properties.VERSION;
import static org.carlspring.strongbox.db.schema.Properties.*;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_ID_GROUP;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_TAG;
import static org.carlspring.strongbox.db.schema.Vertices.GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.MAVEN_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.NPM_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.NUGET_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.PYPI_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.RAW_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.REMOTE_ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.USER;
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
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.schema.EdgeLabelMaker;
import org.janusgraph.core.schema.JanusGraphIndex;
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
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(REMOTE_ARTIFACT),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(RAW_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(MAVEN_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(NPM_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(NUGET_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(PYPI_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_TAG),
                              true,
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);

        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              false,
                              false,
                              jgm.getPropertyKey(STORAGE_ID),
                              jgm.getPropertyKey(REPOSITORY_ID)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              true,
                              jgm.getPropertyKey(STORAGE_ID),
                              jgm.getPropertyKey(REPOSITORY_ID),
                              jgm.getPropertyKey(NAME)).ifPresent(result::add);
        buildIndexIfNecessary(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(USER),
                              true,
                              jgm.getPropertyKey(UUID)).ifPresent(result::add);     

        return result;
    }

    private void applySchemaChanges(JanusGraphManagement jgm)
    {
        // Properties
        createProperties(jgm);

        // Vertices
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT);
        makeVertexLabelIfDoesNotExist(jgm, REMOTE_ARTIFACT);
        makeVertexLabelIfDoesNotExist(jgm, GENERIC_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, RAW_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, MAVEN_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, NPM_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, NUGET_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, PYPI_ARTIFACT_COORDINATES);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_TAG);
        makeVertexLabelIfDoesNotExist(jgm, ARTIFACT_ID_GROUP);
        makeVertexLabelIfDoesNotExist(jgm, USER);

        // Edges
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_HAS_ARTIFACT_COORDINATES, MANY2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_HAS_TAGS, MULTI);
        makeEdgeLabelIfDoesNotExist(jgm, REMOTE_ARTIFACT_INHERIT_ARTIFACT, ONE2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES, ONE2ONE);
        makeEdgeLabelIfDoesNotExist(jgm, ARTIFACT_GROUP_HAS_ARTIFACTS, ONE2MANY);

        // Add property constraints
        applyPropertyConstraints(jgm);

        // Add connection constraints
        applyConnectionConstraints(jgm);
    }

    private void applyConnectionConstraints(JanusGraphManagement jgm)
    {
        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_HAS_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(ARTIFACT),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_HAS_TAGS),
                          jgm.getVertexLabel(ARTIFACT),
                          jgm.getVertexLabel(ARTIFACT_TAG));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_GROUP_HAS_ARTIFACTS),
                          jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                          jgm.getVertexLabel(ARTIFACT));

        jgm.addConnection(jgm.getEdgeLabel(REMOTE_ARTIFACT_INHERIT_ARTIFACT),
                          jgm.getVertexLabel(REMOTE_ARTIFACT),
                          jgm.getVertexLabel(ARTIFACT));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(RAW_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(NUGET_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(NPM_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(MAVEN_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_COORDINATES_INHERIT_GENERIC_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(PYPI_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

    }

    private void applyPropertyConstraints(JanusGraphManagement jgm)
    {
        // Vertex Property Constraints
        addVertexPropertyConstraints(jgm,
                                     ARTIFACT,
                                     UUID,
                                     STORAGE_ID,
                                     REPOSITORY_ID,
                                     CREATED,
                                     LAST_UPDATED,
                                     LAST_USED,
                                     SIZE_IN_BYTES,
                                     DOWNLOAD_COUNT,
                                     FILE_NAMES,
                                     CHECKSUMS);

        addVertexPropertyConstraints(jgm,
                                     REMOTE_ARTIFACT,
                                     UUID,
                                     CACHED,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     GENERIC_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_ID,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_PATH,
                                     COORDINATES_SCOPE,
                                     COORDINATES_GROUP_ID,
                                     COORDINATES_ARTIFACT_ID,
                                     COORDINATES_CLASSIFIER,
                                     COORDINATES_DISTRIBUTION,
                                     COORDINATES_BUILD,
                                     COORDINATES_ABI,
                                     COORDINATES_PLATFORM,
                                     COORDINATES_PACKAGING,
                                     COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     RAW_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_PATH,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     MAVEN_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_GROUP_ID,
                                     COORDINATES_ARTIFACT_ID,
                                     COORDINATES_CLASSIFIER,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     NPM_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_SCOPE,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     NUGET_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_ID,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     PYPI_ARTIFACT_COORDINATES,
                                     UUID,
                                     VERSION,
                                     COORDINATES_EXTENSION,
                                     COORDINATES_NAME,
                                     COORDINATES_BUILD,
                                     COORDINATES_ABI,
                                     COORDINATES_PLATFORM,
                                     COORDINATES_PACKAGING,
                                     COORDINATES_DISTRIBUTION,
                                     COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     ARTIFACT_TAG,
                                     UUID,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     ARTIFACT_ID_GROUP,
                                     UUID,
                                     STORAGE_ID,
                                     REPOSITORY_ID,
                                     NAME,
                                     CREATED);

        addVertexPropertyConstraints(jgm,
                                     USER,
                                     UUID,
                                     PASSWORD,
                                     ENABLED,
                                     ROLES,
                                     SECURITY_TOKEN_KEY,
                                     SOURCE_ID,
                                     CREATED,
                                     LAST_UPDATED);
    }

    private void addVertexPropertyConstraints(JanusGraphManagement jgm,
                                              String vertex,
                                              String... propertykeys)
    {
        VertexLabel vertexLabel = jgm.getVertexLabel(vertex);
        for (String propertyKey : propertykeys)
        {
            jgm.addProperties(vertexLabel, jgm.getPropertyKey(propertyKey));
        }
    }

    private void createProperties(JanusGraphManagement jgm)
    {
        makePropertyKeyIfDoesNotExist(jgm, UUID,
                                      String.class).ifPresent(p -> jgm.setConsistency(p, ConsistencyModifier.LOCK));
        makePropertyKeyIfDoesNotExist(jgm, STORAGE_ID, String.class);
        makePropertyKeyIfDoesNotExist(jgm, REPOSITORY_ID, String.class);
        makePropertyKeyIfDoesNotExist(jgm, NAME, String.class);
        makePropertyKeyIfDoesNotExist(jgm, LAST_UPDATED, Long.class, Cardinality.SINGLE);

        // Artifact
        makePropertyKeyIfDoesNotExist(jgm, SIZE_IN_BYTES, Long.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, LAST_USED, Long.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, CREATED, Long.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, DOWNLOAD_COUNT, Integer.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, FILE_NAMES, String.class, Cardinality.SET);
        makePropertyKeyIfDoesNotExist(jgm, CHECKSUMS, String.class, Cardinality.SET);

        // RemoteArtifact
        makePropertyKeyIfDoesNotExist(jgm, CACHED, Boolean.class, Cardinality.SINGLE);

        // Common coordinates
        makePropertyKeyIfDoesNotExist(jgm, VERSION, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_EXTENSION, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_NAME, String.class, Cardinality.SINGLE);

        // Maven
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_GROUP_ID, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_ARTIFACT_ID, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_CLASSIFIER, String.class, Cardinality.SINGLE);

        // Npm
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_SCOPE, String.class, Cardinality.SINGLE);

        // Nuget
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_ID, String.class, Cardinality.SINGLE);

        // P2
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_FILENAME, String.class, Cardinality.SINGLE);

        // Pypi
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_BUILD, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION, String.class,
                                      Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_ABI, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_PLATFORM, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_PACKAGING, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_DISTRIBUTION, String.class, Cardinality.SINGLE);

        // Raw
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_PATH, String.class, Cardinality.SINGLE);

        // Rpm
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_BASE_NAME, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_RELEASE, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_ARCHITECTURE, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, COORDINATES_PACKAGE_TYPE, String.class, Cardinality.SINGLE);

        // User
        makePropertyKeyIfDoesNotExist(jgm, PASSWORD, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, ENABLED, Boolean.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, ROLES, String.class, Cardinality.SET);
        makePropertyKeyIfDoesNotExist(jgm, SECURITY_TOKEN_KEY, String.class, Cardinality.SINGLE);
        makePropertyKeyIfDoesNotExist(jgm, SOURCE_ID, String.class, Cardinality.SINGLE);
    }

    private Optional<String> buildIndexIfNecessary(final JanusGraphManagement jgm,
                                                   final Class<? extends Element> elementType,
                                                   final JanusGraphSchemaType schemaType,
                                                   final boolean unique,
                                                   final PropertyKey... properties){
        return buildIndexIfNecessary(jgm, elementType, schemaType, unique, true, properties);
    }
    
    private Optional<String> buildIndexIfNecessary(final JanusGraphManagement jgm,
                                                   final Class<? extends Element> elementType,
                                                   final JanusGraphSchemaType schemaType,
                                                   final boolean unique,
                                                   final boolean consistent,
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
        JanusGraphIndex intex = indexBuilder.buildCompositeIndex();
        if (consistent)
        {
            jgm.setConsistency(intex, ConsistencyModifier.LOCK);
        }

        return Optional.of(intex.name());
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

    private Optional<PropertyKey> makePropertyKeyIfDoesNotExist(final JanusGraphManagement jgm,
                                               final String name,
                                               final Class<?> dataType)
    {
        return makePropertyKeyIfDoesNotExist(jgm, name, dataType, null);
    }

    private Optional<PropertyKey> makePropertyKeyIfDoesNotExist(final JanusGraphManagement jgm,
                                               final String name,
                                               final Class<?> dataType,
                                               final Cardinality cardinality)
    {
        if (jgm.containsPropertyKey(name))
        {
            return Optional.empty();
        }

        PropertyKeyMaker propertyKeyMaker = jgm.makePropertyKey(name).dataType(dataType);
        if (cardinality != null)
        {
            propertyKeyMaker = propertyKeyMaker.cardinality(cardinality);
        }
        return Optional.of(propertyKeyMaker.make());
    }

}
