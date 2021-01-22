package org.carlspring.strongbox.db.schema.migration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.schema.JanusGraphManagement;

/**
 * @author sbespalov
 */
public interface Changeset extends ChangesetVersion
{
    default String getName()
    {
        return getClass().getSimpleName();
    }
 
    String getAuthor();
    
    default void applySchemaChanges(JanusGraphManagement jgm)
    {

    }

    default Set<String> createVertexIndexes(JanusGraphManagement jgm)
    {
        return Collections.emptySet();
    }

    default Map<String, String> createRelationIndexes(JanusGraphManagement jgm)
    {
        return Collections.emptyMap();
    }

    default void applyGraphChanges(GraphTraversalSource g)
    {

    }

}
