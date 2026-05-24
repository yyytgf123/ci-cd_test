package com.groom.common.infrastructure.config.jpa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.sql.DataSource;

@Configuration
@EnableJpaAuditing
@ConditionalOnBean(DataSource.class)
public class JpaAuditConfig { //time
}
