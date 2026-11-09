package io.github.andi005.qram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Starts the application: Spring scans this package and everything below it. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class QrAssetManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(QrAssetManagerApplication.class, args);
  }
}
