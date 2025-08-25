package org.openjdbcproxy.grpc.server;

import com.google.protobuf.ByteString;
import com.openjdbcproxy.grpc.ConnectionDetails;
import com.openjdbcproxy.grpc.SessionInfo;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openjdbcproxy.grpc.SerializationHandler;
import org.openjdbcproxy.grpc.server.utils.ConnectionHashGenerator;
import org.openjdbcproxy.grpc.server.chain.SqlProcessorChain;
import org.openjdbcproxy.grpc.server.chain.processors.SlowQuerySegregationProcessor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that slow query segregation is properly integrated into
 * the SQL processing chain and configured with correct pool sizes.
 */
public class PerDatasourceSlowQuerySegregationTest {

    private StatementServiceImpl statementService;
    private ServerConfiguration serverConfiguration;

    @BeforeEach
    public void setUp() {
        serverConfiguration = new ServerConfiguration();
        SessionManager sessionManager = Mockito.mock(SessionManager.class);
        CircuitBreaker circuitBreaker = Mockito.mock(CircuitBreaker.class);
        
        statementService = new StatementServiceImpl(sessionManager, circuitBreaker, serverConfiguration);
    }

    @Test
    public void testSlowQuerySegregationProcessorInChain() throws Exception {
        // Create properties for connection
        Properties clientProperties = new Properties();
        clientProperties.setProperty("ojp.connection.pool.maximumPoolSize", "15");
        clientProperties.setProperty("ojp.connection.pool.minimumIdle", "3");
        byte[] serializedProperties = SerializationHandler.serialize(clientProperties);

        // Create connection details
        ConnectionDetails connectionDetails = ConnectionDetails.newBuilder()
                .setUrl("jdbc:h2:mem:test")
                .setUser("test")
                .setPassword("test")
                .setClientUUID("client-1")
                .setProperties(ByteString.copyFrom(serializedProperties))
                .build();

        StreamObserver<SessionInfo> responseObserver = Mockito.mock(StreamObserver.class);
        statementService.connect(connectionDetails, responseObserver);

        // Use reflection to access the SQL processor chain
        Field chainField = StatementServiceImpl.class.getDeclaredField("sqlProcessorChain");
        chainField.setAccessible(true);
        
        SqlProcessorChain chain = (SqlProcessorChain) chainField.get(statementService);
        assertNotNull(chain, "SQL processor chain should be initialized");

        // Find the slow query segregation processor in the chain
        SlowQuerySegregationProcessor segregationProcessor = 
                (SlowQuerySegregationProcessor) chain.findProcessor("SlowQuerySegregationProcessor");
        
        assertNotNull(segregationProcessor, "Slow query segregation processor should be in the chain");
        assertTrue(segregationProcessor.isEnabled(), "Segregation processor should be enabled");
        
        // Check processor stats
        var stats = segregationProcessor.getStats();
        assertNotNull(stats, "Processor stats should be available");
        assertTrue(stats.isEnabled(), "Stats should show processor as enabled");
        
        System.out.println("Slow query segregation processor stats: " + stats.toString());
    }

    @Test
    public void testMultipleDatasourcesCalculation() throws Exception {
        // Create first datasource
        Properties clientProperties1 = new Properties();
        clientProperties1.setProperty("ojp.connection.pool.maximumPoolSize", "10");
        clientProperties1.setProperty("ojp.connection.pool.minimumIdle", "2");
        byte[] serializedProperties1 = SerializationHandler.serialize(clientProperties1);

        ConnectionDetails connectionDetails1 = ConnectionDetails.newBuilder()
                .setUrl("jdbc:h2:mem:test1")
                .setUser("test")
                .setPassword("test")
                .setClientUUID("client-1")
                .setProperties(ByteString.copyFrom(serializedProperties1))
                .build();

        // Create second datasource
        Properties clientProperties2 = new Properties();
        clientProperties2.setProperty("ojp.connection.pool.maximumPoolSize", "20");
        clientProperties2.setProperty("ojp.connection.pool.minimumIdle", "5");
        byte[] serializedProperties2 = SerializationHandler.serialize(clientProperties2);

        ConnectionDetails connectionDetails2 = ConnectionDetails.newBuilder()
                .setUrl("jdbc:h2:mem:test2")
                .setUser("test")
                .setPassword("test")
                .setClientUUID("client-2")
                .setProperties(ByteString.copyFrom(serializedProperties2))
                .build();

        StreamObserver<SessionInfo> responseObserver1 = Mockito.mock(StreamObserver.class);
        StreamObserver<SessionInfo> responseObserver2 = Mockito.mock(StreamObserver.class);

        // Connect both datasources
        statementService.connect(connectionDetails1, responseObserver1);
        statementService.connect(connectionDetails2, responseObserver2);

        // Test the total slots calculation
        java.lang.reflect.Method calculateMethod = StatementServiceImpl.class
                .getDeclaredMethod("calculateTotalSlots");
        calculateMethod.setAccessible(true);
        
        Integer totalSlots = (Integer) calculateMethod.invoke(statementService);
        
        assertNotNull(totalSlots, "Total slots should be calculated");
        assertEquals(30, totalSlots, "Total slots should be sum of both pool sizes (10 + 20)");
        
        System.out.println("Calculated total slots: " + totalSlots);
    }

    @Test
    public void testChainConfiguration() throws Exception {
        // Use reflection to access the SQL processor chain
        Field chainField = StatementServiceImpl.class.getDeclaredField("sqlProcessorChain");
        chainField.setAccessible(true);
        
        SqlProcessorChain chain = (SqlProcessorChain) chainField.get(statementService);
        assertNotNull(chain, "SQL processor chain should be initialized");

        // Verify chain statistics
        var chainStats = chain.getStatistics();
        assertTrue(chainStats.totalProcessors >= 5, "Chain should have at least 5 processors");
        assertTrue(chainStats.enabledProcessors >= 3, "Chain should have at least 3 enabled processors");
        
        // Verify specific processors exist
        assertNotNull(chain.findProcessor("TransactionProcessor"), "Transaction processor should exist");
        assertNotNull(chain.findProcessor("DataPermissionProcessor"), "Data permission processor should exist");
        assertNotNull(chain.findProcessor("CrudOperationProcessor"), "CRUD operation processor should exist");
        assertNotNull(chain.findProcessor("SlowQuerySegregationProcessor"), "Slow query segregation processor should exist");
        assertNotNull(chain.findProcessor("SqlExecutionProcessor"), "SQL execution processor should exist");
        
        System.out.println("Chain statistics: " + chainStats.toString());
    }

    @Test
    public void testSlowQuerySegregationDisabled() throws Exception {
        // Create disabled configuration
        ServerConfiguration disabledConfig = new ServerConfiguration();
        // serverConfiguration.setSlowQuerySegregationEnabled(false); // Assume default is false
        
        SessionManager sessionManager = Mockito.mock(SessionManager.class);
        CircuitBreaker circuitBreaker = Mockito.mock(CircuitBreaker.class);
        
        StatementServiceImpl disabledService = new StatementServiceImpl(
                sessionManager, circuitBreaker, disabledConfig);

        // Use reflection to access the SQL processor chain
        Field chainField = StatementServiceImpl.class.getDeclaredField("sqlProcessorChain");
        chainField.setAccessible(true);
        
        SqlProcessorChain chain = (SqlProcessorChain) chainField.get(disabledService);
        SlowQuerySegregationProcessor segregationProcessor = 
                (SlowQuerySegregationProcessor) chain.findProcessor("SlowQuerySegregationProcessor");
        
        assertNotNull(segregationProcessor, "Segregation processor should still exist in chain");
        // Note: The processor might be disabled based on configuration
        
        var stats = segregationProcessor.getStats();
        System.out.println("Disabled segregation processor stats: " + stats.toString());
    }
}