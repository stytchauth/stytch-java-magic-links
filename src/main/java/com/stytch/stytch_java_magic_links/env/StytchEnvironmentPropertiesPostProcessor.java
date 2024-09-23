package com.stytch.stytch_java_magic_links.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;

final class StytchEnvironmentPropertiesPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File localProperties = new File(System.getProperty("user.dir"), "local.properties");
        if (localProperties.exists()) {
            try {
                FileSystemResource localPropertiesResource = new FileSystemResource(localProperties);
                PropertySource<?> source = new PropertiesPropertySourceLoader()
                    .load("local.properties", localPropertiesResource)
                    .getFirst();
                environment.getPropertySources().addLast(source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
