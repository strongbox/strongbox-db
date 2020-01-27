package org.opencypher.gremlin.neo4j.ogm.response;

import java.util.Set;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.ogm.response.Response;

public abstract class GremlinResponse<T> implements Response<T>
{

    final StatementResult result;

    GremlinResponse(StatementResult result)
    {
        this.result = result;
    }

    @Override
    public T next()
    {
        return fetchNext();
    }

    protected abstract T fetchNext();

    @Override
    public void close()
    {
        result.consume();
    }

    @Override
    public String[] columns()
    {
        if (result.hasNext())
        {
            Record record = result.peek();
            if (record != null)
            {
                Set<String> columns = result.peek().asMap().keySet();
                return columns.toArray(new String[columns.size()]);
            }
        }
        return new String[0];
    }
}