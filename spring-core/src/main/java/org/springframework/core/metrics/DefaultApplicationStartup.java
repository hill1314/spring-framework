/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.metrics;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Default "no op" {@code ApplicationStartup} implementation.
 *
 * <p>This variant is designed for minimal overhead and does not record events.
 *
 * DefaultStartupStep 是Spring Framework 5.2引入的新特性，它是Spring的启动步骤的默认实现。主要作用如下：
 * 1. 应用程序启动跟踪：
 * 		DefaultStartupStep用于跟踪Spring应用程序的启动过程。它能够记录应用程序启动时的各个关键步骤，包括初始化、加载配置、实例化Bean等。
 * 2. 性能分析：
 * 		通过记录启动步骤，DefaultStartupStep可以用于性能分析和优化。它能够帮助开发者识别应用程序启动过程中的瓶颈和性能瓶颈，从而更好地优化应用程序的启动性能。
 * 3. 可扩展性：
 * 		DefaultStartupStep提供了扩展点，允许开发者自定义启动步骤，并将自定义的步骤集成到Spring的启动过程中。这样可以更好地监控和跟踪应用程序的启动过程。
 *
 * 总之，DefaultStartupStep在Spring框架中扮演着重要的角色，它 通过记录和跟踪应用程序的启动过程，帮助开发者进行性能分析和优化，
 * 同时也提供了扩展点，允许开发者定制化应用程序的启动跟踪。
 *
 * @author Brian Clozel
 */
class DefaultApplicationStartup implements ApplicationStartup {

	private static final DefaultStartupStep DEFAULT_STARTUP_STEP = new DefaultStartupStep();

	@Override
	public DefaultStartupStep start(String name) {
		return DEFAULT_STARTUP_STEP;
	}


	static class DefaultStartupStep implements StartupStep {

		private final DefaultTags TAGS = new DefaultTags();

		@Override
		public String getName() {
			return "default";
		}

		@Override
		public long getId() {
			return 0L;
		}

		@Override
		public Long getParentId() {
			return null;
		}

		@Override
		public Tags getTags() {
			return this.TAGS;
		}

		@Override
		public StartupStep tag(String key, String value) {
			return this;
		}

		@Override
		public StartupStep tag(String key, Supplier<String> value) {
			return this;
		}

		@Override
		public void end() {

		}


		static class DefaultTags implements StartupStep.Tags {

			@Override
			public Iterator<StartupStep.Tag> iterator() {
				return Collections.emptyIterator();
			}
		}
	}

}
