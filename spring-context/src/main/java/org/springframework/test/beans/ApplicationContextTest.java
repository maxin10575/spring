package org.springframework.test.beans;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Eddie
 */
public class ApplicationContextTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("org.springframework.test.beans");
		Object student = context.getBean("student");
		System.out.println(student);
	}
}
