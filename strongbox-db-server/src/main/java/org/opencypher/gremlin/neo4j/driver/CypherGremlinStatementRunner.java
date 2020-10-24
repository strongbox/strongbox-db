package org.opencypher.gremlin.neo4j.driver;

import static org.opencypher.gremlin.translation.ReturnProperties.ID;
import static org.opencypher.gremlin.translation.ReturnProperties.INV;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;
import static org.opencypher.gremlin.translation.ReturnProperties.OUTV;
import static org.opencypher.gremlin.translation.ReturnProperties.RELATIONSHIP_TYPE;
import static org.opencypher.gremlin.translation.ReturnProperties.TYPE;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.graphdb.relations.RelationIdentifier;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.StatementResultCursor;
import org.neo4j.driver.v1.StatementRunner;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.TypeSystem;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.neo4j.ogm.transaction.GremlinTransaction;
import org.strongbox.util.Commons;

/**
 * @author sbespalov
 */
public class CypherGremlinStatementRunner implements StatementRunner
{

    private final Session session;
    private final GremlinTransaction gremlinTransaction;

    public CypherGremlinStatementRunner(GremlinTransaction gremlinTransaction)
    {
        this.gremlinTransaction = gremlinTransaction;
        this.session = new GremlinServerSession(null,
                CypherGremlinClient.inMemory(gremlinTransaction.getNativeTransaction().traversal()),
                new JanusGraphValueConverter(false));
    }

    public StatementResult run(String statementTemplate,
                               Value parameters)
    {
        return session.run(statementTemplate, parameters);
    }

    public StatementResult run(String statementTemplate,
                               Map<String, Object> statementParameters)
    {
        return session.run(statementTemplate, statementParameters);
    }

    public StatementResult run(String statementTemplate,
                               Record statementParameters)
    {
        return session.run(statementTemplate, statementParameters);
    }

    public StatementResult run(String statementTemplate)
    {
        return session.run(statementTemplate);
    }

    public StatementResult run(Statement statement)
    {
        return session.run(statement);
    }

    public TypeSystem typeSystem()
    {
        return session.typeSystem();
    }

    class JanusGraphValueConverter extends GremlinCypherValueConverter
    {

        public JanusGraphValueConverter(boolean ignoreIds)
        {
            super(ignoreIds);
        }

        @Override
        Record toRecord(Map<String, Object> map)
        {
            map.entrySet().forEach(this::normalizeValue);

            return super.toRecord(map);
        }

        private void normalizeValue(Entry<String, Object> e)
        {
            Object value = e.getValue();
            if (value instanceof Map)
            {
                Map<String, Object> nodeMap = (Map<String, Object>) value;
                nodeMap.entrySet().forEach(this::normalizeValue);
            }
            else if (e.getValue() instanceof RelationIdentifier)
            {
                JanusGraphTransaction tx = (JanusGraphTransaction) gremlinTransaction.getNativeTransaction();
                RelationIdentifier relationIdentifier = (RelationIdentifier) e.getValue();
                if (ID.equals(e.getKey()))
                {
                    e.setValue(relationIdentifier.getRelationId());

                    return;
                }

                Map<String, Object> expectedValue = new HashMap<>();
                expectedValue.put(TYPE, RELATIONSHIP_TYPE);
                expectedValue.put(ID, relationIdentifier.getRelationId());
                expectedValue.put(OUTV, relationIdentifier.getOutVertexId());
                expectedValue.put(INV, relationIdentifier.getInVertexId());

                JanusGraphVertex typeVertex = tx.getVertex(relationIdentifier.getTypeId());
                expectedValue.put(LABEL, typeVertex.label());

                e.setValue(expectedValue);
            }
            else if (value instanceof Date)
            {
                e.setValue(Commons.toLocalDateTime((Date) value));
            }
        }

    }

    @Override
    public CompletionStage<StatementResultCursor> runAsync(String statementTemplate,
                                                           Value parameters)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<StatementResultCursor> runAsync(String statementTemplate,
                                                           Map<String, Object> statementParameters)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<StatementResultCursor> runAsync(String statementTemplate,
                                                           Record statementParameters)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<StatementResultCursor> runAsync(String statementTemplate)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<StatementResultCursor> runAsync(Statement statement)
    {
        throw new UnsupportedOperationException();
    }

}
