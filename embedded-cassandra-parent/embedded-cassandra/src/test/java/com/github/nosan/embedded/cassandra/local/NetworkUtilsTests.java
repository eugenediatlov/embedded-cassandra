/*
 * Copyright 2018-2019 the original author or authors.
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

package com.github.nosan.embedded.cassandra.local;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NetworkUtils}.
 *
 * @author Dmytro Nosan
 */
class NetworkUtilsTests {

	@Test
	void isListen() throws IOException {
		int port;
		InetAddress address;
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			port = serverSocket.getLocalPort();
			address = serverSocket.getInetAddress();
			assertThat(NetworkUtils.isListen(address, port)).isTrue();
		}
		assertThat(NetworkUtils.isListen(address, port)).isFalse();
	}

	@Test
	void getLocalhost() throws UnknownHostException {
		InetAddress localhost = NetworkUtils.getLocalhost();
		assertThat(localhost).isEqualTo(InetAddress.getByName("localhost"));
	}

}
