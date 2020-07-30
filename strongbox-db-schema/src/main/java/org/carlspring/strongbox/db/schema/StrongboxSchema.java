package org.carlspring.strongbox.db.schema;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.carlspring.strongbox.db.schema.changelog.Changelog;
import org.carlspring.strongbox.db.schema.migration.Changeset;
import org.carlspring.strongbox.db.schema.migration.ChangesetVersion;
import org.carlspring.strongbox.db.schema.migration.JanusgraphChangelogStorage;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.RelationType;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexJobFuture;
import org.janusgraph.core.schema.SchemaAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 */
public class StrongboxSchema
{

    private static final Logger logger = LoggerFactory.getLogger(StrongboxSchema.class);

    private final SortedSet<Changeset> changeSets;

    public StrongboxSchema()
    {
        this(Changelog.changeSets);
    }

    public StrongboxSchema(SortedSet<Changeset> changeSets)
    {
        this.changeSets = changeSets;
    }

    public JanusGraph createSchema(JanusGraph jg)
        throws InterruptedException
    {
        logger.info("Apply schema changes.");
        applyChangesets(jg);
        enableIndexes(jg);
        printSchema(jg);
        logger.info("Schema changes applied.");

        return jg;
    }

    private void printSchema(JanusGraph jg)
    {
        JanusGraphManagement jgm = jg.openManagement();
        try
        {
            logger.info(String.format("Schema: %n%s", jgm.printSchema()));
        }
        finally
        {
            jgm.rollback();
        }
    }

    private void enableIndexes(JanusGraph jg)
    {
        JanusGraphManagement jgm = jg.openManagement();
        try
        {
            Set<String> vertexIndexes = fetchVertexIndexes(jgm);
            Map<String, String> relationIndexes = fetchRelationIndexes(jgm);

            enableVertexIndexes(jgm, vertexIndexes);
            enableRelationIndexes(jgm, relationIndexes);

            jgm.commit();
        }
        catch (Exception e)
        {
            logger.error("Failed to enable indexes.", e);
            jgm.rollback();
            throw new RuntimeException(e);
        }
    }

    private void applyChangesets(JanusGraph jg)
    {
        JanusgraphChangelogStorage changelogStorage = new JanusgraphChangelogStorage(jg);
        changelogStorage.init();

        ChangesetVersion schemaVersion = changelogStorage.getSchemaVersion();
        logger.info("Current schema version {}", schemaVersion.getVersion());
        SchemaTemplate schemaTemplate = new SchemaTemplate(jg);
        for (Changeset changeset : changeSets)
        {
            if (schemaVersion.compareTo(changeset) >= 0)
            {
                continue;
            }

            Changeset storedChangeset = changelogStorage.prepare(changeset);
            try
            {
                schemaTemplate.applyChangeset(storedChangeset);
            }
            catch (Exception e)
            {
                logger.error(String.format("Failed to apply changeset [%s]-[%s]", changeset.getVersion(), changeset.getName()), e);
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, String> fetchRelationIndexes(JanusGraphManagement jgm)
    {
        Iterable<RelationType> relationTypes = jgm.getRelationTypes(RelationType.class);

        return StreamSupport.stream(relationTypes.spliterator(), false)
                            .flatMap(r -> StreamSupport.stream(jgm.getRelationIndexes(r).spliterator(), false))
                            .collect(Collectors.toMap(e -> e.name(), e -> e.getType().name()));
    }

    private Set<String> fetchVertexIndexes(JanusGraphManagement jgm)
    {
        Iterable<JanusGraphIndex> vertexIndexes = jgm.getGraphIndexes(Vertex.class);
        return StreamSupport.stream(vertexIndexes.spliterator(), false)
                            .map(JanusGraphIndex::name)
                            .collect(Collectors.toSet());
    }

    protected void enableVertexIndexes(JanusGraphManagement jgm,
                                       Set<String> indexes)
        throws InterruptedException,
        ExecutionException
    {
        for (String janusGraphIndex : indexes)
        {
            logger.info(String.format("Enabling index [%s].", janusGraphIndex));
            Optional.ofNullable(jgm.updateIndex(jgm.getGraphIndex(janusGraphIndex), SchemaAction.ENABLE_INDEX))
                    .ifPresent(this::waitIndexUpdate);
        }
    }

    private void waitIndexUpdate(IndexJobFuture future)
    {
        try
        {
            future.get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void enableRelationIndexes(JanusGraphManagement jgm,
                                         Map<String, String> indexes)
        throws InterruptedException,
        ExecutionException
    {
        Set<Entry<String, String>> entrySet = indexes.entrySet();
        for (Entry<String, String> e : entrySet)
        {
            logger.info(String.format("Enabling index [%s].", e.getKey()));
            Optional.ofNullable(jgm.updateIndex(jgm.getRelationIndex(jgm.getRelationType(e.getValue()), e.getKey()),
                                                SchemaAction.ENABLE_INDEX))
                    .ifPresent(this::waitIndexUpdate);
        }
    }

}
