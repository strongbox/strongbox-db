package org.carlspring.strongbox.db.schema.changelog;

import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_GROUP_HAS_ARTIFACTS;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_HAS_TAGS;
import static org.carlspring.strongbox.db.schema.Edges.EXTENDS;
import static org.carlspring.strongbox.db.schema.Edges.USER_HAS_SECURITY_ROLES;
import static org.carlspring.strongbox.db.schema.Properties.ARTIFACT_FILE_EXISTS;
import static org.carlspring.strongbox.db.schema.Properties.CHECKSUMS;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ABI;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ARCHITECTURE;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ARTIFACT_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_BASE_NAME;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_BUILD;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_CLASSIFIER;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_DISTRIBUTION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_EXTENSION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_FILENAME;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_GROUP_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_ID;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_NAME;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PACKAGE_TYPE;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PACKAGING;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PATH;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_PLATFORM;
import static org.carlspring.strongbox.db.schema.Properties.COORDINATES_RELEASE;
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
import static org.carlspring.strongbox.db.schema.Properties.SECURITY_TOKEN_KEY;
import static org.carlspring.strongbox.db.schema.Properties.SIZE_IN_BYTES;
import static org.carlspring.strongbox.db.schema.Properties.SOURCE_ID;
import static org.carlspring.strongbox.db.schema.Properties.STORAGE_ID;
import static org.carlspring.strongbox.db.schema.Properties.TAG_NAME;
import static org.carlspring.strongbox.db.schema.Properties.UUID;
import static org.carlspring.strongbox.db.schema.Properties.VERSION;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_ID_GROUP;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_TAG;
import static org.carlspring.strongbox.db.schema.Vertices.GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.MAVEN_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.NPM_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.NUGET_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.PYPI_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.RAW_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.SECURITY_ROLE;
import static org.carlspring.strongbox.db.schema.Vertices.USER;
import static org.carlspring.strongbox.db.schema.util.SchemaUtils.addEdgePropertyConstraints;
import static org.carlspring.strongbox.db.schema.util.SchemaUtils.addVertexPropertyConstraints;
import static org.janusgraph.core.Multiplicity.MANY2ONE;
import static org.janusgraph.core.Multiplicity.MULTI;
import static org.janusgraph.core.Multiplicity.ONE2MANY;
import static org.janusgraph.core.Multiplicity.ONE2ONE;

import java.util.Optional;

import org.carlspring.strongbox.db.schema.migration.Changeset;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.schema.JanusGraphManagement;

public class V1_0_0_1_InitialSchema implements Changeset
{

    @Override
    public String getVersion()
    {
        return "1.0.0.1";
    }

    @Override
    public String getAuthor()
    {
        return "serge.bespalov@gmail.com";
    }

    @Override
    public void applySchemaChanges(JanusGraphManagement jgm)
    {
        // Properties
        makeProperties(jgm);

        // Vertices
        makeVertices(jgm);

        // Edges
        makeEdges(jgm);

        // Add property constraints
        makeVertexConstraints(jgm);

        // Add connection constraints
        makeEdgeConstraints(jgm);
    }

    private void makeProperties(JanusGraphManagement jgm)
    {
        Optional.of(jgm.makePropertyKey(UUID).dataType(String.class).make())
                .ifPresent(p -> jgm.setConsistency(p, ConsistencyModifier.LOCK));
        jgm.makePropertyKey(STORAGE_ID).dataType(String.class).make();
        jgm.makePropertyKey(REPOSITORY_ID).dataType(String.class).make();
        jgm.makePropertyKey(NAME).dataType(String.class).make();
        jgm.makePropertyKey(LAST_UPDATED).dataType(Long.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(TAG_NAME).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Artifact
        jgm.makePropertyKey(SIZE_IN_BYTES).dataType(Long.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(LAST_USED).dataType(Long.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(CREATED).dataType(Long.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(DOWNLOAD_COUNT).dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(FILE_NAMES).dataType(String.class).cardinality(Cardinality.SET).make();
        jgm.makePropertyKey(CHECKSUMS).dataType(String.class).cardinality(Cardinality.SET).make();

        // RemoteArtifact
        jgm.makePropertyKey(ARTIFACT_FILE_EXISTS).dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();

        // Common coordinates
        jgm.makePropertyKey(VERSION).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_EXTENSION).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_NAME).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Maven
        jgm.makePropertyKey(COORDINATES_GROUP_ID).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_ARTIFACT_ID).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_CLASSIFIER).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Npm
        jgm.makePropertyKey(COORDINATES_SCOPE).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Nuget
        jgm.makePropertyKey(COORDINATES_ID).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // P2
        jgm.makePropertyKey(COORDINATES_FILENAME).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Pypi
        jgm.makePropertyKey(COORDINATES_BUILD).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_LANGUAGE_IMPLEMENTATION_VERSION).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_ABI).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_PLATFORM).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_PACKAGING).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_DISTRIBUTION).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Raw
        jgm.makePropertyKey(COORDINATES_PATH).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // Rpm
        jgm.makePropertyKey(COORDINATES_BASE_NAME).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_RELEASE).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_ARCHITECTURE).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(COORDINATES_PACKAGE_TYPE).dataType(String.class).cardinality(Cardinality.SINGLE).make();

        // User
        jgm.makePropertyKey(PASSWORD).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(ENABLED).dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(SECURITY_TOKEN_KEY).dataType(String.class).cardinality(Cardinality.SINGLE).make();
        jgm.makePropertyKey(SOURCE_ID).dataType(String.class).cardinality(Cardinality.SINGLE).make();
    }

    private void makeVertices(JanusGraphManagement jgm)
    {
        jgm.makeVertexLabel(ARTIFACT).make();
        jgm.makeVertexLabel(GENERIC_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(RAW_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(MAVEN_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(NPM_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(NUGET_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(PYPI_ARTIFACT_COORDINATES).make();
        jgm.makeVertexLabel(ARTIFACT_TAG).make();
        jgm.makeVertexLabel(ARTIFACT_ID_GROUP).make();
        jgm.makeVertexLabel(USER).make();
        jgm.makeVertexLabel(SECURITY_ROLE).make();
    }

    private void makeEdges(JanusGraphManagement jgm)
    {
        jgm.makeEdgeLabel(ARTIFACT_HAS_ARTIFACT_COORDINATES).multiplicity(MANY2ONE).make();
        jgm.makeEdgeLabel(ARTIFACT_HAS_TAGS).multiplicity(MULTI).make();
        jgm.makeEdgeLabel(EXTENDS).multiplicity(ONE2ONE).make();
        jgm.makeEdgeLabel(ARTIFACT_GROUP_HAS_ARTIFACTS).multiplicity(ONE2MANY).make();
        jgm.makeEdgeLabel(ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS).multiplicity(MULTI).make();
        jgm.makeEdgeLabel(USER_HAS_SECURITY_ROLES).multiplicity(MULTI).make();
    }

    private void makeVertexConstraints(JanusGraphManagement jgm)
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
                                     CHECKSUMS,
                                     ARTIFACT_FILE_EXISTS);

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
                                     SECURITY_TOKEN_KEY,
                                     SOURCE_ID,
                                     CREATED,
                                     LAST_UPDATED);

        addVertexPropertyConstraints(jgm,
                                     SECURITY_ROLE,
                                     UUID,
                                     CREATED);

        addEdgePropertyConstraints(jgm,
                                   ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS,
                                   TAG_NAME);
    }

    private void makeEdgeConstraints(JanusGraphManagement jgm)
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

        jgm.addConnection(jgm.getEdgeLabel(ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS),
                          jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                          jgm.getVertexLabel(ARTIFACT));

        jgm.addConnection(jgm.getEdgeLabel(EXTENDS),
                          jgm.getVertexLabel(RAW_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(EXTENDS),
                          jgm.getVertexLabel(NUGET_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(EXTENDS),
                          jgm.getVertexLabel(NPM_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(EXTENDS),
                          jgm.getVertexLabel(MAVEN_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(EXTENDS),
                          jgm.getVertexLabel(PYPI_ARTIFACT_COORDINATES),
                          jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES));

        jgm.addConnection(jgm.getEdgeLabel(USER_HAS_SECURITY_ROLES),
                          jgm.getVertexLabel(USER),
                          jgm.getVertexLabel(SECURITY_ROLE));

    }

}
