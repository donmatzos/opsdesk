package at.donmatzos.opsdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled (e.g. the active-incident gauge refresh). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfig {
}
