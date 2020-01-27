package org.opencypher.gremlin.neo4j.ogm.response;

import java.util.Arrays;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.ogm.model.GraphRowListModel;
import org.neo4j.ogm.response.model.DefaultGraphRowListModel;
import org.neo4j.ogm.result.adapter.GraphRowModelAdapter;
import org.opencypher.gremlin.neo4j.driver.Neo4jDriverEntityAdapter;

public class GremlinGraphRowModelResponse extends GremlinResponse<GraphRowListModel>
{

    private final GraphRowModelAdapter adapter;

    public GremlinGraphRowModelResponse(StatementResult result,
                                        Neo4jDriverEntityAdapter entityAdapter)
    {

        super(result);

        this.adapter = new GraphRowModelAdapter(new GremlinModelResponse.GremlinGraphModelAdapter(entityAdapter));
        this.adapter.setColumns(Arrays.asList(columns()));
    }

    @Override
    public GraphRowListModel fetchNext()
    {
        if (result.hasNext())
        {
            DefaultGraphRowListModel model = new DefaultGraphRowListModel();
            model.add(adapter.adapt(result.next().asMap()));

            while (result.hasNext())
            {
                model.add(adapter.adapt(result.next().asMap()));
            }
            return model;
        }
        return null;
    }
}
