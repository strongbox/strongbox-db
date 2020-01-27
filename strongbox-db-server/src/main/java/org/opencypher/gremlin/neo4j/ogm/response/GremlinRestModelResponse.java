package org.opencypher.gremlin.neo4j.ogm.response;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.ogm.config.ObjectMapperFactory;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.model.RestModel;
import org.neo4j.ogm.response.model.DefaultRestModel;
import org.neo4j.ogm.response.model.QueryStatisticsModel;
import org.neo4j.ogm.result.adapter.RestModelAdapter;
import org.neo4j.ogm.result.adapter.ResultAdapter;
import org.opencypher.gremlin.neo4j.driver.Neo4jDriverEntityAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GremlinRestModelResponse extends GremlinResponse<RestModel>
{

    private final RestModelAdapter restModelAdapter;
    private final QueryStatisticsModel statisticsModel;
    private final Iterator<Record> resultProjection;

    public GremlinRestModelResponse(StatementResult result,
                                    Neo4jDriverEntityAdapter entityAdapter)
    {

        super(result);

        this.restModelAdapter = new GremlinRestModelAdapter(entityAdapter);
        this.resultProjection = result.list().iterator();
        this.statisticsModel = new StatisticsModelAdapter().adapt(result);
    }

    @Override
    public RestModel fetchNext()
    {
        return DefaultRestModel.basedOn(buildModel())
                               .orElse(null);
    }

    private Map<String, Object> buildModel()
    {
        Map<String, Object> row = new LinkedHashMap<>();
        if (resultProjection.hasNext())
        {
            row = restModelAdapter.adapt(resultProjection.next().asMap());
        }

        return row;
    }

    @Override
    public Optional<QueryStatistics> getStatistics()
    {
        return Optional.of(statisticsModel);
    }

    static class GremlinRestModelAdapter extends RestModelAdapter
    {

        private final Neo4jDriverEntityAdapter entityAdapter;

        public GremlinRestModelAdapter(Neo4jDriverEntityAdapter entityAdapter)
        {
            this.entityAdapter = entityAdapter;
        }

        @Override
        public boolean isNode(Object value)
        {
            return entityAdapter.isNode(value);
        }

        @Override
        public boolean isRelationship(Object value)
        {
            return entityAdapter.isRelationship(value);
        }

        @Override
        public long nodeId(Object node)
        {
            return entityAdapter.nodeId(node);
        }

        @Override
        public List<String> labels(Object value)
        {
            return entityAdapter.labels(value);
        }

        @Override
        public long relationshipId(Object relationship)
        {
            return entityAdapter.relationshipId(relationship);
        }

        @Override
        public String relationshipType(Object relationship)
        {
            return entityAdapter.relationshipType(relationship);
        }

        @Override
        public Long startNodeId(Object relationship)
        {
            return entityAdapter.startNodeId(relationship);
        }

        @Override
        public Long endNodeId(Object relationship)
        {
            return entityAdapter.endNodeId(relationship);
        }

        @Override
        public Map<String, Object> properties(Object container)
        {
            return entityAdapter.properties(container);
        }
    }

    static class StatisticsModelAdapter
            implements ResultAdapter<org.neo4j.driver.v1.StatementResult, QueryStatisticsModel>
    {

        protected static final ObjectMapper mapper = ObjectMapperFactory.objectMapper();

        @Override
        public QueryStatisticsModel adapt(org.neo4j.driver.v1.StatementResult result)
        {
            QueryStatisticsModel queryStatisticsModel = new QueryStatisticsModel();
            org.neo4j.driver.v1.summary.SummaryCounters stats = result.consume().counters();
            queryStatisticsModel.setContains_updates(stats.containsUpdates());
            queryStatisticsModel.setNodes_created(stats.nodesCreated());
            queryStatisticsModel.setNodes_deleted(stats.nodesDeleted());
            queryStatisticsModel.setProperties_set(stats.propertiesSet());
            queryStatisticsModel.setRelationships_created(stats.relationshipsCreated());
            queryStatisticsModel.setRelationship_deleted(stats.relationshipsDeleted());
            queryStatisticsModel.setLabels_added(stats.labelsAdded());
            queryStatisticsModel.setLabels_removed(stats.labelsRemoved());
            queryStatisticsModel.setIndexes_added(stats.indexesAdded());
            queryStatisticsModel.setIndexes_removed(stats.indexesRemoved());
            queryStatisticsModel.setConstraints_added(stats.constraintsAdded());
            queryStatisticsModel.setConstraints_removed(stats.constraintsRemoved());
            return queryStatisticsModel;
        }
    }
}
