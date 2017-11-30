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
 * Created on 24/03/16
 */
package org.neo4j.jdbc.bolt.utils;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.jdbc.bolt.BoltNeo4jConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a factory to create all the mocks that are used in multiple tests
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Mocker {

	public static Session mockSessionOpen() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		return session;
	}

	public static Session mockSessionClosed() {
		return mock(Session.class);
	}

	public static Session mockSessionOpenSlow() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		when(session.run(anyString())).thenAnswer(new Answer<ResultSet>() {
			@Override public ResultSet answer(InvocationOnMock invocation) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
				}
				return null;
			}
		});
		return session;
	}

	public static Session mockSessionException() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		when(session.run(anyString())).thenThrow(new RuntimeException("RuntimeException THROWN"));
		return session;
	}

	public static BoltNeo4jConnection mockConnectionOpen() throws SQLException {
		BoltNeo4jConnection mockConnection = mock(BoltNeo4jConnection.class);
		when(mockConnection.isClosed()).thenReturn(false);
		return mockConnection;
	}

	public static BoltNeo4jConnection mockConnectionClosed() throws SQLException {
		BoltNeo4jConnection mockConnection = mock(BoltNeo4jConnection.class);
		when(mockConnection.isClosed()).thenReturn(true);
		return mockConnection;
	}

	public static BoltNeo4jConnection mockConnectionOpenWithTransactionThatReturns(StatementResult cur) throws SQLException {
		Transaction mockTransaction = mock(Transaction.class);
		when(mockTransaction.run(anyString())).thenReturn(cur);
		when(mockTransaction.run(anyString(), any(HashMap.class))).thenReturn(cur);

		BoltNeo4jConnection mockConnection = mockConnectionOpen();
		when(mockConnection.getTransaction()).thenReturn(mockTransaction);
		return mockConnection;
	}
}
