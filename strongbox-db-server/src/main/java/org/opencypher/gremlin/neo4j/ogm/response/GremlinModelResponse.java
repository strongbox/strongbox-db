package org.opencypher.gremlin.neo4j.ogm.response;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.result.adapter.GraphModelAdapter;
import org.opencypher.gremlin.neo4j.driver.Neo4jDriverEntityAdapter;

public class GremlinModelResponse extends GremlinResponse<GraphModel>
{

    private final GremlinGraphModelAdapter adapter;

    public GremlinModelResponse(StatementResult result,
                                Neo4jDriverEntityAdapter entityAdapter)
    {

        super(result);

        this.adapter = new GremlinGraphModelAdapter(entityAdapter);
    }

    @Override
    public GraphModel fetchNext()
    {
        if (result.hasNext())
        {
            return adapter.adapt(result.next().asMap());
        }
        return null;
    }

    static class GremlinGraphModelAdapter extends GraphModelAdapter
    {

        private final Neo4jDriverEntityAdapter entityAdapter;

        GremlinGraphModelAdapter(Neo4jDriverEntityAdapter entityAdapter)
        {
            this.entityAdapter = entityAdapter;
        }

        @Override
        public boolean isPath(Object value)
        {
            return entityAdapter.isPath(value);
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
        public List<String> labels(Object entity)
        {
            return entityAdapter.labels(entity);
        }

        @Override
        public long relationshipId(Object relationship)
        {
            return entityAdapter.relationshipId(relationship);
        }

        @Override
        public String relationshipType(Object entity)
        {
            return entityAdapter.relationshipType(entity);
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

        @Override
        public List<Object> nodesInPath(Object pathValue)
        {
            return entityAdapter.nodesInPath(pathValue);
        }

        @Override
        public List<Object> relsInPath(Object pathValue)
        {
            return entityAdapter.relsInPath(pathValue);
        }

    }
}