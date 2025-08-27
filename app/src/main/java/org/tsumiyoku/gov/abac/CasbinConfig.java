package org.tsumiyoku.gov.abac;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasbinConfig {
    @Bean
    public Enforcer enforcer() {
        // Charge model + policy depuis resources
        return new Enforcer("casbin/model.conf", "casbin/policy.csv");
    }
}