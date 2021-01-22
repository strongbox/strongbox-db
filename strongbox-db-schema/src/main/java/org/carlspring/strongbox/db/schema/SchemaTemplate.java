package org.carlspring.strongbox.db.schema;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.carlspring.strongbox.db.schema.migration.Changeset;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 */
public class SchemaTemplate
{
    private static final Logger logger = LoggerFactory.getLogger(SchemaTemplate.class);

    private final JanusGraph jg;

    public SchemaTemplate(JanusGraph jg)
    {
        this.jg = jg;
    }

    public void applyChangeset(Changeset changeset)
    {
        logger.info(String.format("Apply changeset [%s]-[%s].", changeset.getVersion(), changeset.getName()));

        JanusGraphManagement jgm = jg.openManagement();
        Set<String> compositeIndexes;
        Map<String, String> relationIndexes;
        try
        {
            changeset.applySchemaChanges(jgm);

            logger.info(String.format("Create indexes [%s]-[%s].", changeset.getVersion(), changeset.getName()));
            compositeIndexes = changeset.createVertexIndexes(jgm);
            relationIndexes = changeset.createRelationIndexes(jgm);

            jgm.commit();
        }
        catch (Exception e)
        {
            jgm.rollback();
            throw new RuntimeException(String.format("Failed to apply changeset [%s]-[%s].",
                                                     changeset.getVersion(),
                                                     changeset.getName()),
                    e);
        }

        GraphTraversalSource g = jg.traversal();
        try
        {
            changeset.applyGraphChanges(g);
            g.tx().commit();
        }
        catch (Exception e)
        {
            g.tx().rollback();
            throw new RuntimeException(String.format("Failed to apply changeset [%s]-[%s].",
                                                     changeset.getVersion(),
                                                     changeset.getName()),
                    e);
        }

        // Waiting index status
        try
        {
            for (String janusGraphIndex : compositeIndexes)
            {
                logger.info(String.format("Wait index [%s] to be registered.", janusGraphIndex));
                ManagementSystem.awaitGraphIndexStatus(jg, janusGraphIndex).call();
            }

            for (Entry<String, String> relationIndex : relationIndexes.entrySet())
            {
                logger.info(String.format("Wait index [%s] to be registered.", relationIndex.getKey()));
                ManagementSystem.awaitRelationIndexStatus(jg, relationIndex.getKey(), relationIndex.getValue()).call();
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Failed to enable indexes for [%s]-[%s].",
                                                     changeset.getVersion(),
                                                     changeset.getName()),
                    e);
        }
    }

}
