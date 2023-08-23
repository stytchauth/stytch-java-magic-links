package com.stytch.javamagiclinks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class StytchAuthenticationIntercepterAppConfig implements WebMvcConfigurer {
    @Autowired
    StytchAuthenticationIntercepter stytchAuthenticationIntercepter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(stytchAuthenticationIntercepter);
    }
}
