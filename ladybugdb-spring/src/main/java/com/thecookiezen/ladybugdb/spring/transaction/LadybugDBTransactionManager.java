package com.thecookiezen.ladybugdb.spring.transaction;

import com.ladybugdb.Connection;
import com.thecookiezen.ladybugdb.spring.connection.LadybugDBConnectionFactory;
import com.thecookiezen.ladybugdb.spring.core.ConnectionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring PlatformTransactionManager implementation for LadybugDB.
 * <p>
 * This transaction manager binds a LadybugDB connection to the current thread
 * for the duration of a transaction. All operations performed through
 * {@link com.thecookiezen.ladybugdb.spring.core.LadybugDBTemplate} within the
 * transaction will use the same connection.
 * <p>
 * This implementation provides connection binding for logical transaction
 * boundaries, ensuring consistent connection usage within a transaction scope.
 */
public class LadybugDBTransactionManager extends AbstractPlatformTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(LadybugDBTransactionManager.class);

    private final LadybugDBConnectionFactory connectionFactory;

    public LadybugDBTransactionManager(LadybugDBConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        LadybugDBTransactionObject txObject = new LadybugDBTransactionObject();
        ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(connectionFactory);
        txObject.setConnectionHolder(holder);
        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) transaction;
        return txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) transaction;

        try {
            if (!txObject.hasConnectionHolder()) {
                Connection connection = connectionFactory.getConnection();
                logger.debug("Acquired connection for new transaction");

                ConnectionHolder holder = new ConnectionHolder(connection);
                holder.setTransactionActive(true);
                holder.setSynchronizedWithTransaction(true);
                txObject.setConnectionHolder(holder);
                txObject.setNewConnectionHolder(true);

                TransactionSynchronizationManager.bindResource(connectionFactory, holder);
            } else {
                // Existing connection holder - mark as active
                txObject.getConnectionHolder().setTransactionActive(true);
            }

            // NOTE: LadybugDB auto-commits each command. Explicit transaction management
            // (BEGIN TRANSACTION/COMMIT/ROLLBACK) is not used because LadybugDB only allows
            // one write transaction at a time, which can cause deadlocks in multi-threaded
            // apps.
            logger.debug("Started LadybugDB transaction (auto-commit mode)");
        } catch (Exception e) {
            throw new LadybugDBTransactionException("Could not begin transaction", e);
        }
    }

    @Override
    @SuppressWarnings("unused") // txObject kept for API consistency
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) status.getTransaction();
        logger.debug("Committing LadybugDB transaction (auto-commit mode - no explicit commit needed)");
        // LadybugDB auto-commits each command, so no explicit COMMIT is needed.
    }

    @Override
    @SuppressWarnings("unused") // txObject kept for API consistency
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) status.getTransaction();
        logger.debug("Rolling back LadybugDB transaction");
        // LadybugDB auto-commits each command - rollback is not supported.
        // Once a command executes, it is committed. This transaction manager provides
        // connection binding only.
        logger.warn("LadybugDB auto-commits each command. Rollback not supported.");
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) transaction;

        if (txObject.isNewConnectionHolder()) {
            TransactionSynchronizationManager.unbindResource(connectionFactory);
        }

        ConnectionHolder holder = txObject.getConnectionHolder();
        if (holder != null) {
            holder.setTransactionActive(false);
            if (txObject.isNewConnectionHolder()) {
                logger.debug("Releasing connection after transaction");
                connectionFactory.releaseConnection(holder.getConnection());
            }
        }

        txObject.clear();
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        LadybugDBTransactionObject txObject = (LadybugDBTransactionObject) status.getTransaction();
        logger.debug("Setting transaction rollback-only");
        txObject.setRollbackOnly(true);
    }

    public LadybugDBConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
