/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.AfterAdvice;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Spring AOP advice wrapping an AspectJ after advice method.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAfterAdvice extends AbstractAspectJAdvice
		implements MethodInterceptor, AfterAdvice, Serializable {

	public AspectJAfterAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
			// 先执行执行链，当所有执行链中的通知方法，及被代理方法执行完毕，会继续往下走。
			// 最终，在finally语句块中，调用后置通知方法。卸载finally语句块里，是让在执行链中及被代理方法执行完毕，但还未返回时，执行后置通知方法。
			// 这里有点绕。 A(around) -> B(after) -> C(around)，假设有这三个切面组成执行链，他们执行顺序按照前面的排序。
			// 执行顺序是这样的：A环绕前置通知 -> B invoke中通知调用执行链下一个节点（这里没有实际的执行内容，只是做一个火炬传递） -> C环绕前置通知 -> C环绕后置通知 -> B后置通知（这时候B中调用完，返回前掉finally语句块） -> A环绕后置通知。
			return mi.proceed();
		}
		finally {
			invokeAdviceMethod(getJoinPointMatch(), null, null);
		}
	}

	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

}
