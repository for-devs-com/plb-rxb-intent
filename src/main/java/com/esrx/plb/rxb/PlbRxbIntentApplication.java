package com.esrx.plb.rxb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.Arrays;


@Slf4j
@Configuration
@ComponentScan(basePackages = {"com.esrx.plb.rxb","com.esrx.plb.commons"}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {com.esrx.plb.rxb.config.WebSecurityConfiguration.class}))
@SpringBootApplication(exclude = {KafkaAutoConfiguration.class, DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class})
public class PlbRxbIntentApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(PlbRxbIntentApplication.class, args);
        String[] beans = ctx.getBeanDefinitionNames();
        Arrays.sort(beans);
        for (String bean : beans) {
            Object beanClass = ctx.getBean(bean);
            log.info("Loaded Bean - " + bean + ", Class - " + ((beanClass != null) ? beanClass.toString() : "Null Class - Class not loaded"));
        }
        log.info("***** PLB Rxb Application  *****");
    }

}
