package com.net128.application.sqlcommander;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@SuppressWarnings("unused")
public class Application extends SpringBootServletInitializer {
	static {
		String addLoc="spring.config.additional-location";
		if(System.getProperty(addLoc)==null) {
			String userHome=System.getProperty("user.home");
			String location=userHome+"/.springboot/"+
				Application.class.getPackage().getName()+".properties";
			System.setProperty(addLoc, location);
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}
}
