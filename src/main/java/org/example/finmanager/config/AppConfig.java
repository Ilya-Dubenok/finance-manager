package org.example.finmanager.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Конфигурация контекста Spring: сканирование компонентов и общие бины. */
@Configuration
@ComponentScan(AppConfig.BASE_PACKAGE)
public class AppConfig {

  /** Корневой пакет, который сканирует Spring. */
  public static final String BASE_PACKAGE = "org.example.finmanager";

  /** Jackson для файлов кошельков: неизвестные поля игнорируются, чтобы новые файлы читались. */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /** Источник текущего времени; в тестах подменяется на фиксированный. */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
