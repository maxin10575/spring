/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.transaction.config;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.*;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Chris Beams
 * @since 2.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String METHOD_ELEMENT = "method";

	private static final String METHOD_NAME_ATTRIBUTE = "name";

	private static final String ATTRIBUTES_ELEMENT = "attributes";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String READ_ONLY_ATTRIBUTE = "read-only";

	private static final String PROPAGATION_ATTRIBUTE = "propagation";

	private static final String ISOLATION_ATTRIBUTE = "isolation";

	private static final String ROLLBACK_FOR_ATTRIBUTE = "rollback-for";

	private static final String NO_ROLLBACK_FOR_ATTRIBUTE = "no-rollback-for";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return TransactionInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));
		// 获取子标签 <tx:attributes>，这个子标签只能有一个。
		List<Element> txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES_ELEMENT);
		if (txAttributes.size() > 1) {
			parserContext.getReaderContext().error(
					"Element <attributes> is allowed at most once inside element <advice>", element);
		}
		else if (txAttributes.size() == 1) {
			// Using attributes source.
			Element attributeSourceElement = txAttributes.get(0);
			// TODO 解析子标签，将子标签封装为RootBeanDefinition
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
		}
		else {
			// Assume annotations source.
			builder.addPropertyValue("transactionAttributeSource",
					new RootBeanDefinition("org.springframework.transaction.annotation.AnnotationTransactionAttributeSource"));
		}
	}

	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
		// 获取子标签<tx:method />
		List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, METHOD_ELEMENT);
		ManagedMap<TypedStringValue, RuleBasedTransactionAttribute> transactionAttributeMap =
				new ManagedMap<>(methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));
		// 解析子标签中的属性
		for (Element methodEle : methods) {
			// 解析属性 name
			String name = methodEle.getAttribute(METHOD_NAME_ATTRIBUTE);
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(methodEle));
			// 解析属性 propagation、isolation、timeout、read-only
			RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
			String propagation = methodEle.getAttribute(PROPAGATION_ATTRIBUTE);
			String isolation = methodEle.getAttribute(ISOLATION_ATTRIBUTE);
			String timeout = methodEle.getAttribute(TIMEOUT_ATTRIBUTE);
			String readOnly = methodEle.getAttribute(READ_ONLY_ATTRIBUTE);
			// 将属性值设置到 attribute
			if (StringUtils.hasText(propagation)) {
				attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
			}
			if (StringUtils.hasText(isolation)) {
				attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
			}
			if (StringUtils.hasText(timeout)) {
				try {
					attribute.setTimeout(Integer.parseInt(timeout));
				}
				catch (NumberFormatException ex) {
					parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", methodEle);
				}
			}
			if (StringUtils.hasText(readOnly)) {
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY_ATTRIBUTE)));
			}
			// 解析属性，回滚规则 rollback-for
			List<RollbackRuleAttribute> rollbackRules = new LinkedList<>();
			if (methodEle.hasAttribute(ROLLBACK_FOR_ATTRIBUTE)) {
				String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR_ATTRIBUTE);
				addRollbackRuleAttributesTo(rollbackRules,rollbackForValue);
			}
			// 解析属性，不回滚规则 no-rollback-for
			if (methodEle.hasAttribute(NO_ROLLBACK_FOR_ATTRIBUTE)) {
				String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR_ATTRIBUTE);
				addNoRollbackRuleAttributesTo(rollbackRules,noRollbackForValue);
			}
			attribute.setRollbackRules(rollbackRules);

			transactionAttributeMap.put(nameHolder, attribute);
		}
		// 与 AnnotationTransactionAttributeSource 功能类似，只不过前者是注解方式的支持。
		// AnnotationTransactionAttributeSource 是名称匹配的支持。
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
		attributeSourceDefinition.getPropertyValues().add("nameMap", transactionAttributeMap);
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

	private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

}
