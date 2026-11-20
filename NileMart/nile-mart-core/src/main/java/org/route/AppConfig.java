package org.route;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("org.route")
@PropertySource("classpath:application.properties")
public class AppConfig {
}
