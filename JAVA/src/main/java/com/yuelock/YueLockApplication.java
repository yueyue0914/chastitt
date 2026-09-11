package com.yuelock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YueLockApplication {
  public static void main(String[] args) {
    SpringApplication.run(YueLockApplication.class, args);
  }
}
