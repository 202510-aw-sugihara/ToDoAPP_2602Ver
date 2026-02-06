package com.example.todo;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  @Async
  public void sendAsync(String message) {
    log.info("async notification start: {}", message);
    simulateWork(200);
    log.info("async notification end: {}", message);
  }

  @Async
  public CompletableFuture<String> sendWithResult(String message) {
    log.info("async notification (future) start: {}", message);
    simulateWork(300);
    log.info("async notification (future) end: {}", message);
    return CompletableFuture.completedFuture("sent:" + message);
  }

  public void notifyException(String summary, Throwable ex) {
    log.warn("async exception notification: {} - {}", summary, ex.toString());
  }

  private void simulateWork(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
