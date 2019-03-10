/*
 * Copyright 2018-2019 the original author or authors.
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

package com.github.nosan.embedded.cassandra.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.local.artifact.Artifact;
import com.github.nosan.embedded.cassandra.local.artifact.ArtifactFactory;
import com.github.nosan.embedded.cassandra.util.ArchiveUtils;
import com.github.nosan.embedded.cassandra.util.FileUtils;

/**
 * {@link Initializer} to initialize a {@code directory} with an {@link Artifact}.
 *
 * @author Dmytro Nosan
 * @since 1.3.0
 */
class WorkingDirectoryInitializer implements Initializer {

	private static final Logger log = LoggerFactory.getLogger(WorkingDirectoryInitializer.class);

	private final ArtifactFactory artifactFactory;

	private final Path artifactDirectory;

	/**
	 * Creates an {@link WorkingDirectoryInitializer}.
	 *
	 * @param artifactFactory a factory to create {@link Artifact}
	 * @param artifactDirectory a directory to extract an {@link Artifact} (must be writable)
	 */
	WorkingDirectoryInitializer(ArtifactFactory artifactFactory, Path artifactDirectory) {
		this.artifactFactory = artifactFactory;
		this.artifactDirectory = artifactDirectory;
	}

	@Override
	public void initialize(Path workingDirectory, Version version) throws IOException {
		Path artifactDirectory = this.artifactDirectory;
		Artifact artifact = this.artifactFactory.create(version);
		Objects.requireNonNull(artifact, "Artifact must not be null");
		extractArtifact(artifact, artifactDirectory);
		copyArtifact(workingDirectory, artifactDirectory);
	}

	private static void extractArtifact(Artifact artifact, Path artifactDirectory) throws IOException {
		Path archive = artifact.get();
		log.info("Extract ({}) into ({}).", archive, artifactDirectory);
		try {
			ArchiveUtils.extract(archive, artifactDirectory);
		}
		catch (IOException ex) {
			throw new IOException(String.format("Artifact (%s) could not be extracted into (%s)",
					archive, artifactDirectory), ex);
		}
		log.info("({}) Archive has been extracted into ({})", archive, artifactDirectory);
	}

	private static void copyArtifact(Path workingDirectory, Path artifactDirectory) throws IOException {
		Path directory = getDirectory(artifactDirectory);
		log.info("Copy ({}) folder into ({}).", directory, workingDirectory);
		try {
			FileUtils.copy(directory, workingDirectory, path -> shouldCopy(directory, workingDirectory, path));
		}
		catch (IOException ex) {
			throw new IOException(String.format("Could not copy folder (%s) into (%s)", directory, workingDirectory),
					ex);
		}
		log.info("({}) Folder has been copied into ({})", directory, workingDirectory);
	}

	private static boolean shouldCopy(Path src, Path dest, Path srcPath) {
		Path relativize = src.relativize(srcPath);
		Path destPath = dest.resolve(relativize);
		if (Files.isDirectory(srcPath) && !Files.exists(destPath)) {
			String name = relativize.getName(0).toString().toLowerCase(Locale.ENGLISH);
			return !name.equals("javadoc") && !name.equals("doc");
		}
		try {
			return !Files.exists(dest) || Files.size(destPath) < Files.size(srcPath);
		}
		catch (IOException ex) {
			return true;
		}
	}

	private static Path getDirectory(Path directory) throws IOException {
		Set<Path> directories = Files.find(directory, 1, (path, attributes) -> {
			Path bin = path.resolve("bin");
			Path lib = path.resolve("lib");
			Path conf = path.resolve("conf/cassandra.yaml");
			return Files.exists(bin) && Files.exists(conf) && Files.exists(lib);
		}).collect(Collectors.toSet());

		if (directories.isEmpty()) {
			throw new IllegalStateException(
					String.format("(%s) must have at least 'bin', lib' folders and 'conf/cassandra.yaml' file",
							directory));
		}
		if (directories.size() > 1) {
			throw new IllegalStateException(String.format(
					"Impossible to determine a base directory. There are (%s) candidates : (%s)",
					directories.size(), directories));

		}
		return directories.iterator().next();
	}

}
