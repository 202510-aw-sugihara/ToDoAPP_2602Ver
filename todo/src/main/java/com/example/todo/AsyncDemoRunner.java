package com.example.todo;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AsyncDemoRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AsyncDemoRunner.class);
  private final NotificationService notificationService;

  public AsyncDemoRunner(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public void run(String... args) throws Exception {
    notificationService.sendAsync("fire-and-forget");

    CompletableFuture<String> future1 = notificationService.sendWithResult("demo-1");
    CompletableFuture<String> future2 = notificationService.sendWithResult("demo-2");

    CompletableFuture.allOf(future1, future2).get();
    String result1 = future1.get();
    String result2 = future2.get();

    log.info("async results: {}, {}", result1, result2);
  }
}
