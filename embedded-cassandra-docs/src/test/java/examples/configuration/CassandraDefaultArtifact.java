/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.nosan.embedded.cassandra.EmbeddedCassandraFactory;
import com.github.nosan.embedded.cassandra.api.Cassandra;
import com.github.nosan.embedded.cassandra.api.Version;
import com.github.nosan.embedded.cassandra.artifact.DefaultArtifact;

public class CassandraDefaultArtifact {

	void source() {
		// tag::source[]
		EmbeddedCassandraFactory cassandraFactory = new EmbeddedCassandraFactory();
		Path directory = Paths.get("<path to Cassandra's home>");
		Version version = Version.of("3.11.9");
		cassandraFactory.setArtifact(new DefaultArtifact(version, directory));
		Cassandra cassandra = cassandraFactory.create();
		cassandra.start();
		try {
			//..
		}
		finally {
			cassandra.stop();
		}
		// end::source[]
	}

}
