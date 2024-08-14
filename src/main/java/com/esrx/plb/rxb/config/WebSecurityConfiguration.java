package com.esrx.plb.rxb.config;


import com.esrx.inf.spring.boot.autoconfigure.security.HttpServiceSecurityAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.stereotype.Component;

@Component
public class WebSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    @Profile("local")
    @EnableWebSecurity
    public class LocalSecurityConfig extends HttpServiceSecurityAutoConfiguration {
        @Override
        public void configure(HttpSecurity httpSecurity) throws Exception {
            log.info("Inside WebSecurityConfiguration.LocalSecurityConfig");

            httpSecurity.authorizeRequests().antMatchers("/**").permitAll();
            super.configure(httpSecurity);
        }
    }
}