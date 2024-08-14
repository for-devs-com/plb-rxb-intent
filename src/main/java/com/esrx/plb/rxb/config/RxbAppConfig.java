package com.esrx.plb.rxb.config;

import com.esrx.plb.commons.process.PlbIntentFlow;
import com.esrx.plb.rxb.impl.RxbIntentObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@Slf4j
public class RxbAppConfig {

    @Bean
    @Primary
    public RxbIntentObject createRxbIntentObject() {
        return new RxbIntentObject();
    }

    @Bean
    public PlbIntentFlow<RxbIntentObject> createRxbIntentFlow() {
        return new PlbIntentFlow<>(createRxbIntentObject());
    }
}
