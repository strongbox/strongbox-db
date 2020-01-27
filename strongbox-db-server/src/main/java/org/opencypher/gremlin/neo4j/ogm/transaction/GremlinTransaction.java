package org.opencypher.gremlin.neo4j.ogm.transaction;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.ogm.exception.TransactionException;
import org.neo4j.ogm.transaction.AbstractTransaction;
import org.neo4j.ogm.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GremlinTransaction extends AbstractTransaction
{

    private final Logger logger = LoggerFactory.getLogger(GremlinTransaction.class);

    private final Graph nativeTransaction;

    public GremlinTransaction(TransactionManager transactionManager,
                              Graph nativeTransaction,
                              Type type)
    {
        super(transactionManager);
        this.type = type;
        this.nativeTransaction = nativeTransaction;
    }

    public Graph getNativeTransaction()
    {
        return nativeTransaction;
    }

    @Override
    public void rollback()
    {
        try
        {
            doRollback();
        }
        catch (Exception e)
        {
            throw new TransactionException(e.getLocalizedMessage(), e);
        } 
        finally
        {
            super.rollback();
        }
    }

    protected void doRollback() throws Exception
    {
        if (!transactionManager.canRollback())
        {
            logger.debug("Skip rolback.");

            return;
        }

        logger.debug("Rolling back native transaction: {}", nativeTransaction);

        if (!nativeTransaction.tx().isOpen())
        {
            logger.warn("Transaction is already closed");

            return;
        }

        try
        {
            nativeTransaction.tx().rollback();
            nativeTransaction.tx().close();
        }
        finally
        {
            nativeTransaction.close();
        }
    }

    @Override
    public void commit()
    {
        try
        {
            doCommit();
        }
        catch (Exception e)
        {
            super.rollback();
            throw new TransactionException(e.getLocalizedMessage(), e);
        }
        
        super.commit();
    }

    protected void doCommit() throws Exception
    {
        if (!transactionManager.canCommit())
        {
            logger.debug("Skip commit.");

            return;
        }

        if (!nativeTransaction.tx().isOpen())
        {
            throw new IllegalStateException("Transaction is already closed");
        }

        logger.debug("Committing native transaction: {}", nativeTransaction);

        try
        {
            nativeTransaction.tx().commit();
            nativeTransaction.tx().close();
        }
        finally
        {
            nativeTransaction.close();
        }
    }

}
