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
 * Created on 19/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.neo4j.jdbc.InstanceFactory;
import org.neo4j.jdbc.Loggable;
import org.neo4j.jdbc.Neo4jStatement;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jStatement extends Neo4jStatement implements Loggable {

	private int[]        rsParams;

	private boolean loggable = false;

	/**
	 * Default Constructor
	 *
	 * @param connection The connection used for sharing the transaction between statements
	 * @param rsParams   The params (type, concurrency and holdability) used to create a new ResultSet
	 */
	public BoltNeo4jStatement(BoltNeo4jConnection connection, int... rsParams) {
		super(connection);
		this.rsParams = rsParams;
		this.batchStatements = new ArrayList<>();
	}

	@Override public ResultSet executeQuery(String sql) throws SQLException {
			StatementResult result = executeInternal(sql);

			this.currentResultSet = InstanceFactory.debug(BoltNeo4jResultSet.class, new BoltNeo4jResultSet(this, result, this.rsParams), this.isLoggable());
			this.currentUpdateCount = -1;
			return this.currentResultSet;
	}

	@Override public int executeUpdate(String sql) throws SQLException {
			StatementResult result = executeInternal(sql);

			SummaryCounters stats = result.consume().counters();
			this.currentUpdateCount = stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
			this.currentResultSet = null;
			return this.currentUpdateCount;
	}

	@Override public boolean execute(String sql) throws SQLException {
		StatementResult result = executeInternal(sql);

		boolean hasResultSet = false;
		if (result != null) {
			hasResultSet = hasResultSet(sql);
			if (hasResultSet) {
				this.currentResultSet = InstanceFactory.debug(BoltNeo4jResultSet.class, new BoltNeo4jResultSet(this,result, this.rsParams), this.isLoggable());
				this.currentUpdateCount = -1;
			}
			else {
				this.currentResultSet = null;
				try {
					SummaryCounters stats = result.consume().counters();
					this.currentUpdateCount = stats.nodesCreated() + stats.nodesDeleted() + stats.relationshipsCreated() + stats.relationshipsDeleted();
				}
				catch (Exception e) {
					throw new SQLException(e);
				}
			}
		}
		return hasResultSet;
	}

	private StatementResult executeInternal(String sql) throws SQLException {
		this.checkClosed();

		StatementResult result;
		if (this.getConnection().getAutoCommit()) {
			try (Transaction t = ((BoltNeo4jConnection) this.getConnection()).getSession().beginTransaction()) {
				result = t.run(sql);
				t.success();
			}
			catch (Exception e) {
				throw new SQLException(e);
			}
		} else {
			try {
				result = ((BoltNeo4jConnection) this.getConnection()).getTransaction().run(sql);
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return result;
	}

	private boolean hasResultSet(String sql) {
		return sql != null && sql.toLowerCase().contains("return");
	}

	@Override public int getResultSetConcurrency() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getConcurrency();
		}
		if (this.rsParams.length > 1) {
			return this.rsParams[1];
		}
		return BoltNeo4jResultSet.DEFAULT_CONCURRENCY;
	}

	@Override public int getResultSetType() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getType();
		}
		if (this.rsParams.length > 0) {
			return this.rsParams[0];
		}
		return BoltNeo4jResultSet.DEFAULT_TYPE;
	}

	@Override public int getResultSetHoldability() throws SQLException {
		this.checkClosed();
		if (currentResultSet != null) {
			return currentResultSet.getHoldability();
		}
		if (this.rsParams.length > 2) {
			return this.rsParams[2];
		}
		return BoltNeo4jResultSet.DEFAULT_HOLDABILITY;
	}

	/*-------------------*/
	/*       Batch       */
	/*-------------------*/

	@Override public int[] executeBatch() throws SQLException {
		this.checkClosed();

		int[] result = new int[0];

		try {
			for (String query : this.batchStatements) {
				StatementResult res;
				if (this.connection.getAutoCommit()) {
					res = ((BoltNeo4jConnection) connection).getSession().run(query);
				} else {
					res = ((BoltNeo4jConnection) connection).getTransaction().run(query);
				}
				SummaryCounters count = res.consume().counters();
				result = Arrays.copyOf(result, result.length + 1);
				result[result.length - 1] = count.nodesCreated() + count.nodesDeleted();
			}
		} catch (Exception e) {
			throw new BatchUpdateException(result, e);
		}

		return result;
	}

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}

}
