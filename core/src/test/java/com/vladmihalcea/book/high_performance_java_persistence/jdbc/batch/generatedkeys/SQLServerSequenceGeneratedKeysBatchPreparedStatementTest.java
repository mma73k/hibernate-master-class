package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.generatedkeys;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.SequenceBatchEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractSQLServerIntegrationTest;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * GeneratedKeysBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement generated keys
 *
 * @author Vlad Mihalcea
 */
public class SQLServerSequenceGeneratedKeysBatchPreparedStatementTest extends AbstractSQLServerIntegrationTest {

    private SequenceBatchEntityProvider entityProvider = new SequenceBatchEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testBatch() {
        doInConnection(this::batchInsert);
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getBatchSize() {
        return 5;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        AtomicInteger postStatementCount = new AtomicInteger();

        try(PreparedStatement postStatement = connection.prepareStatement("insert into Post (id, title, version) values (NEXT VALUE FOR hibernate_sequence, ?, ?)", new int[]{1})) {
            int postCount = getPostCount();

            int index;

            for (int i = 0; i < postCount; i++) {
                index = 0;

                postStatement.setString(++index, String.format("Post no. %1$d", i));
                postStatement.setInt(++index, 0);
                postStatement.addBatch();
                int count = postStatementCount.incrementAndGet();
                if(count % getBatchSize() == 0) {
                    postStatement.executeBatch();
                    try(ResultSet resultSet = postStatement.getGeneratedKeys()) {
                        while (resultSet.next()) {
                            LOGGER.info("Generated identifier: {}", resultSet.getLong(1));
                        }
                    }
                }
            }
        }
    }
}