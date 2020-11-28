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

package com.github.nosan.embedded.cassandra.cql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link CqlDataSet}.
 *
 * @author Dmytro Nosan
 * @since 4.0.1
 */
public class DefaultCqlDataSet implements CqlDataSet {

	private final List<CqlScript> scripts;

	/**
	 * Creates a new {@link DefaultCqlDataSet} with the specified CQL scripts.
	 *
	 * @param scripts the CQL scripts
	 */
	public DefaultCqlDataSet(Collection<? extends CqlScript> scripts) {
		this.scripts = Collections.unmodifiableList(new ArrayList<>(scripts));
	}

	@Override
	public List<CqlScript> getScripts() {
		return this.scripts;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		DefaultCqlDataSet that = (DefaultCqlDataSet) other;
		return this.scripts.equals(that.scripts);
	}

	@Override
	public int hashCode() {
		return this.scripts.hashCode();
	}

	@Override
	public String toString() {
		return "DefaultCqlDataSet{" + "scripts=" + this.scripts + '}';
	}

}
