package org.opencypher.gremlin.neo4j.ogm;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.driver.AbstractConfigurableDriver;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.transaction.Transaction;
import org.neo4j.ogm.transaction.Transaction.Type;
import org.neo4j.ogm.transaction.TransactionManager;
import org.opencypher.gremlin.neo4j.driver.CypherGremlinStatementRunner;
import org.opencypher.gremlin.neo4j.ogm.request.GremlinRequest;
import org.opencypher.gremlin.neo4j.ogm.transaction.GremlinTransaction;

/**
 * @author sbespalov
 */
public class GremlinGraphDriver extends AbstractConfigurableDriver
{

    private org.apache.tinkerpop.gremlin.structure.Transaction transactionContext;

    public GremlinGraphDriver(org.apache.tinkerpop.gremlin.structure.Transaction transactionContext)
    {
        this.transactionContext = transactionContext;
    }

    @Override
    public void configure(Configuration config)
    {

    }

    @Override
    public Function<TransactionManager, BiFunction<Type, Iterable<String>, Transaction>> getTransactionFactorySupplier()
    {
        return transactionManager -> (type,
                                      bookmarks) -> {
            return createTransaction(transactionManager, type);
        };

    }

    private Transaction createTransaction(TransactionManager transactionManager,
                                          Type type)
    {
        // TODO: Type.READ_ONLY
        Graph tx = transactionContext.createThreadedTx();

        return new GremlinTransaction(transactionManager, tx, type);
    }

    @Override
    public void close()
    {
        transactionContext.close();
    }

    @Override
    public Request request(Transaction transaction)
    {
        return new GremlinRequest(new CypherGremlinStatementRunner((GremlinTransaction) transaction));
    }

    @Override
    public Configuration getConfiguration()
    {
        return null;
    }

    @Override
    protected String getTypeSystemName()
    {
        return GremlinGraphDriver.class.getName() + ".types";
    }

}
