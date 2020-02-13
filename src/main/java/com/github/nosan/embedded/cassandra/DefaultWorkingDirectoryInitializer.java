/*
 * Copyright 2020 the original author or authors.
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

package com.github.nosan.embedded.cassandra;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.github.nosan.embedded.cassandra.commons.FileUtils;

/**
 * The default implementation of {@link WorkingDirectoryInitializer} which gets Cassandra directory from the {@link
 * CassandraDirectoryProvider} and copies all files from a gotten directory into the working directory except
 * <b>javadoc, doc and licenses</b> directories.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
public class DefaultWorkingDirectoryInitializer implements WorkingDirectoryInitializer {

	protected static final Set<String> SKIP_DIRECTORIES = Collections.unmodifiableSet(
			new LinkedHashSet<>(Arrays.asList("javadoc", "doc", "licenses")));

	private final CassandraDirectoryProvider cassandraDirectoryProvider;

	/**
	 * Creates a new {@link DefaultWorkingDirectoryInitializer}.
	 *
	 * @param cassandraDirectoryProvider the Cassandra directory provider. This provider is used to get a path to
	 * Cassandra directory.
	 */
	public DefaultWorkingDirectoryInitializer(CassandraDirectoryProvider cassandraDirectoryProvider) {
		Objects.requireNonNull(cassandraDirectoryProvider, "Cassandra Directory Provider must not be null");
		this.cassandraDirectoryProvider = cassandraDirectoryProvider;
	}

	@Override
	public final void init(Path workingDirectory, Version version) throws IOException {
		Objects.requireNonNull(workingDirectory, "Working Directory must not be null");
		Objects.requireNonNull(version, "Version must not be null");
		Path cassandraDirectory = this.cassandraDirectoryProvider.getDirectory(version);
		Objects.requireNonNull(cassandraDirectory, "Cassandra Directory must not be null");
		copy(cassandraDirectory, workingDirectory, version);
	}

	protected void copy(Path cassandraDirectory, Path workingDirectory, Version version) throws IOException {
		FileUtils.copy(cassandraDirectory, workingDirectory, this::include, StandardCopyOption.REPLACE_EXISTING);
	}

	protected boolean include(Path path, BasicFileAttributes attributes) {
		if (attributes.isDirectory()) {
			return !SKIP_DIRECTORIES.contains(path.getFileName().toString());
		}
		return true;
	}

}
