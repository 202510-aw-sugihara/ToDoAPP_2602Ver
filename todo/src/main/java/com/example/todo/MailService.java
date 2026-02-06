package com.example.todo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {

  private static final Logger log = LoggerFactory.getLogger(MailService.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final String fromAddress;

  public MailService(JavaMailSender mailSender, TemplateEngine templateEngine,
      @Value("${app.mail.from}") String fromAddress) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.fromAddress = fromAddress;
  }

  public void sendTodoCreated(AppUser user, Todo todo) {
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      log.warn("skip todo created mail: user email missing");
      return;
    }
    if (fromAddress == null || fromAddress.isBlank()) {
      log.warn("skip todo created mail: from address missing");
      return;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(user.getEmail());
    message.setSubject("ToDo created");
    message.setText(buildTodoCreatedText(user, todo));
    mailSender.send(message);
  }

  public void sendDueSoonReminder(AppUser user, List<Todo> todos) {
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      log.warn("skip reminder mail: user email missing");
      return;
    }
    if (fromAddress == null || fromAddress.isBlank()) {
      log.warn("skip reminder mail: from address missing");
      return;
    }
    if (todos == null || todos.isEmpty()) {
      return;
    }
    Context context = new Context();
    context.setVariable("user", user);
    context.setVariable("todos", todos);
    context.setVariable("dateFormatter", DATE_FORMATTER);
    String html = templateEngine.process("mail/todo_reminder", context);
    sendHtml(user.getEmail(), "ToDo reminder (due soon)", html);
  }

  private String buildTodoCreatedText(AppUser user, Todo todo) {
    String due = todo.getDueDate() == null ? "-" : todo.getDueDate().format(DATE_FORMATTER);
    return "Hello " + user.getUsername() + "\n"
        + "Your ToDo was created.\n"
        + "Title: " + todo.getTitle() + "\n"
        + "Due: " + due + "\n";
  }

  private void sendHtml(String to, String subject, String html) {
    if (fromAddress == null || fromAddress.isBlank()) {
      log.warn("skip html mail: from address missing");
      return;
    }
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
    } catch (MessagingException ex) {
      throw new IllegalStateException("Failed to send html mail", ex);
    }
  }
}
