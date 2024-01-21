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

package org.springframework.context.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Standard implementation of the
 * {@link org.springframework.beans.factory.config.BeanExpressionResolver}
 * interface, parsing and evaluating Spring EL using Spring's expression module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * 主要用于解析Bean表达式（Bean expressions）。其作用包括：
 * 1. 解析Bean表达式：StandardBeanExpressionResolver用于解析Spring中的SpEL（Spring Expression Language）表达式。
 * 	这些表达式可以用于在XML配置文件、注解中或者其他地方引用Bean，执行运行时计算，或者进行动态属性设置等。
 * 2. 支持动态计算：通过SpEL表达式，StandardBeanExpressionResolver可以支持动态计算属性值，动态引用Bean，或者执行一些复杂的逻辑操作。
 * 	这使得在配置文件中能够更加灵活地定义Bean之间的关系和属性值。
 * 3. 与注解驱动的开发集成：在基于注解的开发中，StandardBeanExpressionResolver可以与@Value注解一起使用，允许在注解中使用SpEL表达式来动态注入属性值。
 *
 * 通过支持SpEL表达式，它提供了一种灵活的方式来处理动态属性设置、Bean引用和运行时计算，从而增强了Spring框架的灵活性和可扩展性。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see BeanExpressionContext#getBeanFactory()
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/** Default expression prefix: "#{". */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** Default expression suffix: "}". */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

	/**
	 * 表达式解析器
	 */
	private ExpressionParser expressionParser;

	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}
		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}
		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * Create a new {@code StandardBeanExpressionResolver} with default settings.
	 */
	public StandardBeanExpressionResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	/**
	 * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
	 * using it as the basis for expression compilation.
	 * @param beanClassLoader the factory's bean class loader
	 */
	public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
		this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
	}


	/**
	 * Set the prefix that an expression string starts with.
	 * The default is "#{".
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * Set the suffix that an expression string ends with.
	 * The default is "}".
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * Specify the EL parser to use for expression parsing.
	 * <p>Default is a {@link org.springframework.expression.spel.standard.SpelExpressionParser},
	 * compatible with standard Unified EL style expression syntax.
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	@Nullable
	public Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException {
		if (!StringUtils.hasLength(value)) {
			return value;
		}
		try {
			Expression expr = this.expressionCache.get(value);
			if (expr == null) {
				expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
				this.expressionCache.put(value, expr);
			}
			StandardEvaluationContext sec = this.evaluationCache.get(beanExpressionContext);
			if (sec == null) {
				sec = new StandardEvaluationContext(beanExpressionContext);
				sec.addPropertyAccessor(new BeanExpressionContextAccessor());
				sec.addPropertyAccessor(new BeanFactoryAccessor());
				sec.addPropertyAccessor(new MapAccessor());
				sec.addPropertyAccessor(new EnvironmentAccessor());
				sec.setBeanResolver(new BeanFactoryResolver(beanExpressionContext.getBeanFactory()));
				sec.setTypeLocator(new StandardTypeLocator(beanExpressionContext.getBeanFactory().getBeanClassLoader()));
				sec.setTypeConverter(new StandardTypeConverter(() -> {
					ConversionService cs = beanExpressionContext.getBeanFactory().getConversionService();
					return (cs != null ? cs : DefaultConversionService.getSharedInstance());
				}));
				customizeEvaluationContext(sec);
				this.evaluationCache.put(beanExpressionContext, sec);
			}
			return expr.getValue(sec);
		}
		catch (Throwable ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * Template method for customizing the expression evaluation context.
	 * <p>The default implementation is empty.
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
