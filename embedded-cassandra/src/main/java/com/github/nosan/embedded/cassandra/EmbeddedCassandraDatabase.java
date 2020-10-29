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

package com.github.nosan.embedded.cassandra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.nosan.embedded.cassandra.annotations.Nullable;
import com.github.nosan.embedded.cassandra.api.Version;
import com.github.nosan.embedded.cassandra.commons.CacheConsumer;
import com.github.nosan.embedded.cassandra.commons.CompositeConsumer;
import com.github.nosan.embedded.cassandra.commons.io.Resource;
import com.github.nosan.embedded.cassandra.commons.util.FileUtils;
import com.github.nosan.embedded.cassandra.commons.util.StringUtils;

/**
 * Embedded {@link CassandraDatabase}.
 *
 * @author Dmytro Nosan
 */
class EmbeddedCassandraDatabase implements CassandraDatabase {

	private static final Logger log = LoggerFactory.getLogger(EmbeddedCassandraDatabase.class);

	private final String name;

	private final Version version;

	private final Path directory;

	private final Path workingDirectory;

	private final boolean daemon;

	private final Logger logger;

	private final Duration timeout;

	private final CassandraNode node;

	@Nullable
	private final Resource config;

	@Nullable
	private final Resource rackConfig;

	@Nullable
	private final Resource topologyConfig;

	@Nullable
	private volatile InetAddress address;

	private volatile int port = -1;

	private volatile int sslPort = -1;

	private volatile int rpcPort = -1;

	EmbeddedCassandraDatabase(String name, Version version, Path directory, Path workingDirectory, boolean daemon,
			Logger logger, Duration timeout, @Nullable Resource config, @Nullable Resource rackConfig,
			@Nullable Resource topologyConfig, CassandraNode node) {
		this.name = name;
		this.version = version;
		this.directory = directory;
		this.workingDirectory = workingDirectory;
		this.daemon = daemon;
		this.logger = logger;
		this.timeout = timeout;
		this.config = config;
		this.rackConfig = rackConfig;
		this.topologyConfig = topologyConfig;
		this.node = node;
	}

	@Override
	public void start() throws InterruptedException, IOException {
		initialize();
		this.node.start();
		log.info("{} has been started", toString());
		NativeTransportReadinessConsumer nativeTransportReadiness = new NativeTransportReadinessConsumer(this.version,
				this.node.hasSslPort());
		RpcTransportReadinessConsumer rpcTransportReadiness = new RpcTransportReadinessConsumer(this.version);
		await(nativeTransportReadiness, rpcTransportReadiness);
		int sslPort = nativeTransportReadiness.getSslPort();
		int port = nativeTransportReadiness.getPort();
		this.port = (port != -1) ? port : sslPort;
		this.sslPort = sslPort;
		this.rpcPort = rpcTransportReadiness.getRpcPort();
		InetAddress address = nativeTransportReadiness.getAddress();
		this.address = (address != null) ? address : rpcTransportReadiness.getAddress();
	}

	@Override
	public void stop() throws InterruptedException, IOException {
		if (this.node.isAlive()) {
			this.node.stop();
			log.info("{} has been stopped", toString());
		}
		try {
			FileUtils.delete(this.workingDirectory);
		}
		catch (IOException ex) {
			log.error("Working Directory '" + this.workingDirectory + "' has not been deleted", ex);
		}
	}

	@Override
	@Nullable
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public int getSslPort() {
		return this.sslPort;
	}

	@Override
	public int getRpcPort() {
		return this.rpcPort;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", EmbeddedCassandraDatabase.class.getSimpleName() + "[", "]")
				.add("name='" + this.name + "'").add("version='" + this.version + "'").add("node=" + this.node)
				.toString();
	}

	private void initialize() throws IOException {
		Files.createDirectories(this.workingDirectory);
		FileUtils.copy(this.directory, this.workingDirectory, (path, attributes) -> {
			if (attributes.isDirectory()) {
				String name = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
				return !name.equals("javadoc") && !name.equals("doc");
			}
			return true;
		});
		if (this.config != null) {
			try (InputStream is = this.config.getInputStream()) {
				Files.copy(is, this.workingDirectory.resolve("conf/cassandra.yaml"),
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
		if (this.topologyConfig != null) {
			try (InputStream is = this.topologyConfig.getInputStream()) {
				Files.copy(is, this.workingDirectory.resolve("conf/cassandra-topology.properties"),
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
		if (this.rackConfig != null) {
			try (InputStream is = this.rackConfig.getInputStream()) {
				Files.copy(is, this.workingDirectory.resolve("conf/cassandra-rackdc.properties"),
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private void await(ReadinessConsumer... readinessConsumers) throws IOException, InterruptedException {
		CompositeConsumer<String> compositeConsumer = new CompositeConsumer<>();
		CacheConsumer<String> cacheConsumer = new CacheConsumer<>(30);
		compositeConsumer.add(this.logger::info);
		compositeConsumer.add(cacheConsumer);
		for (ReadinessConsumer readinessConsumer : readinessConsumers) {
			compositeConsumer.add(readinessConsumer);
		}
		Map<String, String> context = MDC.getCopyOfContextMap();
		Thread thread = new Thread(() -> {
			Optional.ofNullable(context).ifPresent(MDC::setContextMap);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(this.node.getInputStream(), StandardCharsets.UTF_8))) {
				try {
					reader.lines().filter(StringUtils::hasText).forEach(compositeConsumer);
				}
				catch (UncheckedIOException ex) {
					if (!ex.getMessage().contains("Stream closed")) {
						throw ex;
					}
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Stream cannot be closed", ex);
			}
		});
		thread.setName(this.name);
		thread.setDaemon(this.daemon);
		thread.setUncaughtExceptionHandler((t, ex) -> log.error("Exception in thread " + t, ex));
		thread.start();
		long start = System.nanoTime();
		long rem = this.timeout.toNanos();
		while (rem > 0 && this.node.isAlive() && !isReady(readinessConsumers)) {
			Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
			rem = this.timeout.toNanos() - (System.nanoTime() - start);
		}
		if (!this.node.isAlive()) {
			thread.join(100);
			List<String> lines = new ArrayList<>(cacheConsumer.get());
			Collections.reverse(lines);
			throw new IOException(String.format("'%s' is not alive. Please see logs for more details%n\t%s", this.node,
					String.join(String.format("%n\t"), lines)));
		}
		if (rem <= 0) {
			throw new IllegalStateException(
					toString() + " couldn't be started within " + this.timeout.toMillis() + "ms");
		}
		for (ReadinessConsumer readinessConsumer : readinessConsumers) {
			compositeConsumer.remove(readinessConsumer);
		}
		compositeConsumer.remove(cacheConsumer);
	}

	private static boolean isReady(Readiness... readinesses) {
		for (Readiness readiness : readinesses) {
			if (!readiness.isReady()) {
				return false;
			}
		}
		return true;
	}

}
