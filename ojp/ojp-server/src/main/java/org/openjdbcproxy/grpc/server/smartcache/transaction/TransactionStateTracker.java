package org.openjdbcproxy.grpc.server.smartcache.transaction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks transaction state for each session to ensure cache safety.
 * This is critical for maintaining ACID properties when caching is enabled.
 */
@Slf4j
public class TransactionStateTracker {
    
    private final ConcurrentMap<String, TransactionState> sessionTransactions = new ConcurrentHashMap<>();
    
    /**
     * Represents the transaction state for a session
     */
    @Data
    public static class TransactionState {
        private boolean inTransaction = false;
        private boolean hasWrites = false;
        private long transactionStartTime = 0;
        
        public void startTransaction() {
            this.inTransaction = true;
            this.hasWrites = false;
            this.transactionStartTime = System.currentTimeMillis();
        }
        
        public void markWrite() {
            this.hasWrites = true;
        }
        
        public void endTransaction() {
            this.inTransaction = false;
            this.hasWrites = false;
            this.transactionStartTime = 0;
        }
        
        public long getTransactionDuration() {
            if (!inTransaction) return 0;
            return System.currentTimeMillis() - transactionStartTime;
        }
    }
    
    /**
     * Starts a transaction for the given session
     */
    public void startTransaction(String sessionId) {
        TransactionState state = sessionTransactions.computeIfAbsent(sessionId, k -> new TransactionState());
        state.startTransaction();
        log.debug("Transaction started for session: {}", sessionId);
    }
    
    /**
     * Marks that a write operation has occurred in the current transaction
     */
    public void markWrite(String sessionId) {
        TransactionState state = sessionTransactions.get(sessionId);
        if (state != null && state.isInTransaction()) {
            state.markWrite();
            log.debug("Write operation marked for session: {}", sessionId);
        }
    }
    
    /**
     * Ends the transaction for the given session
     */
    public void endTransaction(String sessionId) {
        TransactionState state = sessionTransactions.get(sessionId);
        if (state != null) {
            state.endTransaction();
            log.debug("Transaction ended for session: {}", sessionId);
        }
    }
    
    /**
     * Checks if the session is in a transaction
     */
    public boolean isInTransaction(String sessionId) {
        TransactionState state = sessionTransactions.get(sessionId);
        return state != null && state.isInTransaction();
    }
    
    /**
     * Checks if the current transaction has performed any writes
     */
    public boolean hasWrites(String sessionId) {
        TransactionState state = sessionTransactions.get(sessionId);
        return state != null && state.isHasWrites();
    }
    
    /**
     * Gets the complete transaction state for a session
     */
    public TransactionState getTransactionState(String sessionId) {
        return sessionTransactions.get(sessionId);
    }
    
    /**
     * Removes transaction state for a session (called when session is closed)
     */
    public void removeSession(String sessionId) {
        sessionTransactions.remove(sessionId);
        log.debug("Transaction state removed for session: {}", sessionId);
    }
    
    /**
     * Gets the number of active transactions
     */
    public long getActiveTransactionCount() {
        return sessionTransactions.values().stream()
                .mapToLong(state -> state.isInTransaction() ? 1 : 0)
                .sum();
    }
    
    /**
     * Gets the number of transactions with writes
     */
    public long getTransactionsWithWritesCount() {
        return sessionTransactions.values().stream()
                .mapToLong(state -> state.isInTransaction() && state.isHasWrites() ? 1 : 0)
                .sum();
    }
    
    /**
     * Clears all transaction states (for testing or reset)
     */
    public void clear() {
        sessionTransactions.clear();
        log.debug("All transaction states cleared");
    }
}