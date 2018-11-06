/*
 * Copyright 2018-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nosan.embedded.cassandra.test;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.nosan.embedded.cassandra.cql.CqlScript;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestCassandra}.
 *
 * @author Dmytro Nosan
 */
public class TestCassandraTests {

	private static final TestCassandra cassandra = new TestCassandra();

	private static final String KEYSPACE_NAME = "test";

	@BeforeClass
	public void start() {
		cassandra.start();

	}

	@AfterClass
	public void stop() {
		cassandra.stop();
	}

	@AfterMethod
	public void clean() {
		cassandra.dropKeyspaces(KEYSPACE_NAME);

	}

	@BeforeMethod
	public void create() {
		cassandra.executeScripts(CqlScript.classpath("init.cql"));
	}


	@Test
	public void dropTables() {
		KeyspaceMetadata keyspace = getKeyspace(KEYSPACE_NAME);
		assertThat(keyspace).isNotNull();
		assertThat(keyspace.getTable("users")).isNotNull();
		cassandra.dropTables("test.users");
		assertThat(keyspace.getTable("users")).isNull();
	}

	@Test
	public void getCount() {
		assertThat(cassandra.getRowCount("test.users")).isEqualTo(1);
	}

	@Test
	public void deleteFromTables() {
		assertThat(cassandra.getRowCount("test.users")).isEqualTo(1);
		cassandra.deleteFromTables("test.users");
		assertThat(cassandra.getRowCount("test.users")).isEqualTo(0);
	}

	@Test
	public void executeStatement() {
		ResultSet resultSet = cassandra.executeStatement("SELECT * FROM test.users WHERE user_id = ?", "frodo");
		assertThat(resultSet.one().get("first_name", String.class)).isEqualTo("Frodo");
	}


	private KeyspaceMetadata getKeyspace(String name) {
		return cassandra.getCluster().getMetadata().getKeyspace(name);
	}
}
