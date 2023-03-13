package org.springframework.test.beans;

import org.springframework.stereotype.Component;

/**
 * @author Eddie
 */
@Component
public class Student {
	private String username = "Eddie";

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "Student{" +
				"username='" + username + '\'' +
				'}';
	}
}
