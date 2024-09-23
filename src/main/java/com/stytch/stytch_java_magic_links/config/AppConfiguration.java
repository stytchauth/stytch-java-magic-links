package com.stytch.stytch_java_magic_links.config;

import com.stytch.stytch_java_magic_links.filters.JwtAuthFilter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class AppConfiguration {
	private final String[] PROTECTED_ROUTES = {
		"/profile"
	};

	@Bean
	public SecurityFilterChain filterChain(@NotNull HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize ->
				authorize
					.requestMatchers(PROTECTED_ROUTES)
						.authenticated()
					.anyRequest()
						.permitAll()
			)
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
			.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
		;
		return http.build();
	}

	@Bean
	JwtAuthFilter jwtAuthFilter() {
		return new JwtAuthFilter();
	}
}
