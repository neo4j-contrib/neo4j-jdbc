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
 * Created on 09/03/16
 */
package org.neo4j.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Neo4jResultSetTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(resultSet.isWrapperFor(Neo4jResultSet.class));
		assertTrue(resultSet.isWrapperFor(java.sql.ResultSet.class));
		assertTrue(resultSet.isWrapperFor(java.sql.Wrapper.class));
		assertTrue(resultSet.isWrapperFor(java.lang.AutoCloseable.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(resultSet.isWrapperFor(Neo4jStatement.class));
		assertFalse(resultSet.isWrapperFor(java.sql.Driver.class));
		assertFalse(resultSet.isWrapperFor(java.sql.Statement.class));
		assertFalse(resultSet.isWrapperFor(java.sql.ResultSetMetaData.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(resultSet.unwrap(Neo4jResultSet.class));
		assertNotNull(resultSet.unwrap(java.sql.ResultSet.class));
		assertNotNull(resultSet.unwrap(java.sql.Wrapper.class));
		assertNotNull(resultSet.unwrap(java.lang.AutoCloseable.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);

		resultSet.unwrap(Neo4jStatement.class);
	}

	/*-----------------------------*/
	/*            warning          */
	/*-----------------------------*/
	@Test public void getWarningsShouldReturnNull() throws SQLException {
		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);
		assertNull(resultSet.getWarnings());
	}

	@Test public void clearWarningsShouldNotThrowException() throws SQLException {
		Neo4jResultSet resultSet = mock(Neo4jResultSet.class, Mockito.CALLS_REAL_METHODS);
		resultSet.clearWarnings();
	}
}
