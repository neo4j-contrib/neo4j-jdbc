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
 * Created on 23/02/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.*;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.neo4j.driver.Config.builder;

/**
 * @author Stefan Armbruster
 */
public class SampleIT {

	@Rule public Neo4jRule neo4j = new Neo4jRule();  // here we're firing up neo4j with bolt enabled

	@Test public void shouldSimpleServerTestSucceed() throws Exception {

		// if we want to have raw access to neo4j instance, e.g. for populating the DB upfront:
		neo4j.defaultDatabaseService().executeTransactionally("create ( )");

		//Creating config without SSL
		Config.ConfigBuilder builder = builder();
		builder.withoutEncryption();
		Config config = builder.build();

		// hitting the DB with a bolt request
		Driver driver = GraphDatabase.driver(neo4j.boltURI(), config);   // defaults to localhost:7687
		Session session = driver.session();
		long count = session.readTransaction(tx -> tx.run("match (n) RETURN count(n) AS count").single().get("count").asLong());
		assertEquals("there should be 1 node in the database", 1, count);
		session.close();
		driver.close();
	}

	@Test public void exampleTestInMemory() throws ClassNotFoundException, SQLException {
		neo4j.defaultDatabaseService().executeTransactionally("create (:User{name:\"testUser\"})");

		// Connect
		Connection con = JdbcConnectionTestUtils.getConnection(neo4j);

		// Querying
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n:User) RETURN n.name");
			assertTrue(rs.next());
			assertEquals("testUser", rs.getString("n.name"));
			assertFalse(rs.next());
		}
		con.close();
	}
}
