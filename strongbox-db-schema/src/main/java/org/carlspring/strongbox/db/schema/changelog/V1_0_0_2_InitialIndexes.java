package org.carlspring.strongbox.db.schema.changelog;

import static org.carlspring.strongbox.db.schema.Edges.ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS;
import static org.carlspring.strongbox.db.schema.Properties.REPOSITORY_ID;
import static org.carlspring.strongbox.db.schema.Properties.STORAGE_ID;
import static org.carlspring.strongbox.db.schema.Properties.TAG_NAME;
import static org.carlspring.strongbox.db.schema.Properties.UUID;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_ID_GROUP;
import static org.carlspring.strongbox.db.schema.Vertices.ARTIFACT_TAG;
import static org.carlspring.strongbox.db.schema.Vertices.GENERIC_ARTIFACT_COORDINATES;
import static org.carlspring.strongbox.db.schema.Vertices.SECURITY_ROLE;
import static org.carlspring.strongbox.db.schema.Vertices.USER;
import static org.carlspring.strongbox.db.schema.util.SchemaUtils.buildIndex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.carlspring.strongbox.db.schema.migration.Changeset;
import org.janusgraph.core.schema.JanusGraphManagement;

public class V1_0_0_2_InitialIndexes implements Changeset
{

    @Override
    public String getVersion()
    {
        return "1.0.0.2";
    }

    @Override
    public String getAuthor()
    {
        return "serge.bespalov@gmail.com";
    }
    
    @Override
    public Set<String> createVertexIndexes(JanusGraphManagement jgm)
    {
        Set<String> result = new HashSet<>();

        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT),
                              true,
                              jgm.getPropertyKey(UUID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(GENERIC_ARTIFACT_COORDINATES),
                              true,
                              jgm.getPropertyKey(UUID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_TAG),
                              true,
                              true,
                              jgm.getPropertyKey(UUID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              true,
                              jgm.getPropertyKey(UUID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(ARTIFACT_ID_GROUP),
                              false,
                              false,
                              jgm.getPropertyKey(STORAGE_ID),
                              jgm.getPropertyKey(REPOSITORY_ID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(USER),
                              true,
                              jgm.getPropertyKey(UUID)));
        result.add(buildIndex(jgm,
                              Vertex.class,
                              jgm.getVertexLabel(SECURITY_ROLE),
                              true,
                              jgm.getPropertyKey(UUID)));

        return result;
    }

    @Override
    public Map<String, String> createRelationIndexes(JanusGraphManagement jgm)
    {
        Map<String, String> result = new HashMap<>();
        String name = ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS + "By" + StringUtils.capitalize(TAG_NAME);
        jgm.buildEdgeIndex(jgm.getEdgeLabel(ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS),
                           name,
                           Direction.OUT,
                           jgm.getPropertyKey(TAG_NAME));
        result.put(name, ARTIFACT_GROUP_HAS_TAGGED_ARTIFACTS);

        return result;
    }

}
