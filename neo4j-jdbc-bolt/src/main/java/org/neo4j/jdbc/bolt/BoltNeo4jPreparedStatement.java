/*
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 23/03/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.Neo4jParameterMetaData;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.Neo4jResultSetMetaData;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;
import org.neo4j.jdbc.utils.Neo4jInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static java.util.Arrays.copyOf;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jPreparedStatement extends Neo4jPreparedStatement implements Loggable {

	private BoltNeo4jPreparedStatement(BoltNeo4jConnectionImpl connection, String rawStatement, int... rsParams) {
		super(connection, rawStatement);
		this.resultSetParams = rsParams;
	}

	public static PreparedStatement newInstance(boolean debug, BoltNeo4jConnectionImpl connection, String rawStatement, int... rsParams) {
		PreparedStatement ps = new BoltNeo4jPreparedStatement(connection, rawStatement, rsParams);
		((Neo4jPreparedStatement) ps).setDebug(debug);
		return (PreparedStatement) Proxy.newProxyInstance(BoltNeo4jPreparedStatement.class.getClassLoader(), new Class[] { PreparedStatement.class },
				new Neo4jInvocationHandler(ps, debug));
	}

	@Override public ResultSet executeQuery() throws SQLException {
		StatementResult result = executeInternal();

		this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
		this.currentUpdateCount = -1;
		return currentResultSet;
	}

	@Override public int executeUpdate() throws SQLException {
		StatementResult result = executeInternal();

		SummaryCounters stats = result.consume().counters();
		this.currentUpdateCount = stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
		this.currentResultSet = null;
		return this.currentUpdateCount;
	}

	@Override public boolean execute() throws SQLException {
		StatementResult result = executeInternal();

		boolean hasResultSet = hasResultSet();
		if (hasResultSet) {
			this.currentResultSet = BoltNeo4jResultSet.newInstance(this.hasDebug(), this, result, this.resultSetParams);
			this.currentUpdateCount = -1;
		} else {
			this.currentResultSet = null;
			try {
				SummaryCounters stats = result.consume().counters();
				this.currentUpdateCount = stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return hasResultSet;
	}

	private StatementResult executeInternal() throws SQLException {
		this.checkClosed();
		try {
			Transaction transaction = ((BoltNeo4jConnection) this.getConnection()).getTransaction();
			StatementResult result = transaction.run(this.statement, this.parameters);
			if (this.getConnection().getAutoCommit()) {
				((BoltNeo4jConnection) this.getConnection()).doCommit();
			}
			return result;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	private boolean hasResultSet() {
		return this.statement != null && this.statement.toLowerCase().contains("return");
	}

	@Override public Neo4jParameterMetaData getParameterMetaData() throws SQLException {
		this.checkClosed();
		return new BoltNeo4jParameterMetaData(this);
	}

	@Override public Neo4jResultSetMetaData getMetaData() throws SQLException {
		return this.currentResultSet == null ? null : (Neo4jResultSetMetaData) this.currentResultSet.getMetaData();
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();
		int[] result = new int[0];

		try {
			for (Map<String, Object> parameter : this.batchParameters) {
                StatementResult res = ((BoltNeo4jConnection) this.connection).getTransaction().run(this.statement, parameter);
				SummaryCounters count = res.consume().counters();
				result = copyOf(result, result.length + 1);
				result[result.length - 1] = count.nodesCreated() + count.nodesDeleted();
				if (this.connection.getAutoCommit()) {
					((BoltNeo4jConnection) this.connection).doCommit();
				}
			}
		} catch (Exception e) {
			throw new BatchUpdateException(result, e);
		}

		return result;
	}
}
