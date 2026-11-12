package org.example.eventpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class ReservationConfig {
    @Bean
    public Clock reservationClock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}
