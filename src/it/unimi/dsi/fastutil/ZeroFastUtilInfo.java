/*
 * Copyright (C) 2026 LionBlazer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.unimi.dsi.fastutil;

/** Marker info for the ZeroFastUtil fork release.
 *
 * <p>This class exists so consumers can quickly confirm that they pulled
 * the forked artifact from Maven Central, not upstream fastutil.
 */
public final class ZeroFastUtilInfo {
	/** Maven group id for this fork. */
	public static final String GROUP_ID = "io.github.lionblazer";
	/** Maven artifact id for this fork. */
	public static final String ARTIFACT_ID = "zerofastutil";
	/** Source repository URL for this fork. */
	public static final String REPOSITORY_URL = "https://github.com/LionBlazer/zerofastutil";
	/** Human-readable marker string. */
	public static final String MARKER = "ZeroFastUtil fork by LionBlazer";

	private ZeroFastUtilInfo() {}

	/** Returns Maven coordinates without version.
	 *
	 * @return {@code <groupId>:<artifactId>}.
	 */
	public static String coordinates() {
		return GROUP_ID + ":" + ARTIFACT_ID;
	}

	/** Returns a marker string suitable for quick runtime verification.
	 *
	 * @return marker with coordinates.
	 */
	public static String marker() {
		return MARKER + " [" + coordinates() + "]";
	}

	/** Returns implementation version from JAR manifest, if available.
	 *
	 * @return implementation version or {@code null} when unavailable.
	 */
	public static String implementationVersion() {
		final Package p = ZeroFastUtilInfo.class.getPackage();
		return p == null ? null : p.getImplementationVersion();
	}
}
