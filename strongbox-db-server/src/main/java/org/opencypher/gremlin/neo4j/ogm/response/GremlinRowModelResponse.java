package org.opencypher.gremlin.neo4j.ogm.response;

import java.util.Arrays;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.ogm.model.RowModel;
import org.neo4j.ogm.result.adapter.RowModelAdapter;
import org.opencypher.gremlin.neo4j.driver.Neo4jDriverEntityAdapter;

public class GremlinRowModelResponse extends GremlinResponse<RowModel>
{

    private final GremlinRowModelAdapter adapter;

    public GremlinRowModelResponse(StatementResult result,
                                   Neo4jDriverEntityAdapter entityAdapter)
    {

        super(result);

        this.adapter = new GremlinRowModelAdapter(entityAdapter);
        this.adapter.setColumns(Arrays.asList(columns()));
    }

    @Override
    public RowModel fetchNext()
    {
        if (result.hasNext())
        {
            return adapter.adapt(result.next().asMap());
        }
        return null;
    }

    class GremlinRowModelAdapter extends RowModelAdapter

    {

        private final Neo4jDriverEntityAdapter entityAdapter;

        public GremlinRowModelAdapter(Neo4jDriverEntityAdapter entityAdapter)
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

    }
}
