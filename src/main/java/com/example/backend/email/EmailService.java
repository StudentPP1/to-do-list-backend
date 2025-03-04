package com.example.backend.email;

import com.example.backend.enums.TokenType;
import com.example.backend.token.Token;
import com.example.backend.token.TokenRepository;
import com.example.backend.users.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final SpringTemplateEngine springTemplateEngine;
    private final TokenRepository tokenRepository;

    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private Integer port;
    @Value("${spring.mail.username}")
    private String sender;
    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.properties.smtp.auth}")
    private String auth;
    @Value("${spring.mail.properties.smtp.starttls.enable}")
    private String starttls;

    public String sendEmail(User user, TokenType type) throws MessagingException {
        String activationToken = generateActivationCode();
        var token = buildToken(activationToken, user, type);
        tokenRepository.save(token);
        sendEmailToUser(
                user.getEmail(),
                user.getUsername(),
                type.getName(),
                activationToken,
                type.getName()
        );
        return activationToken;
    }
    private String generateActivationCode() {
        String numbers = "0123456789";
        StringBuilder newCode = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 6; i++) {
            newCode.append(numbers.charAt(random.nextInt(numbers.length())));
        }

        return newCode.toString();
    }
    private Token buildToken(String tokenContent, User user, TokenType type) {
        return Token.builder()
                .token(tokenContent)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .tokenType(type)
                .build();
    }
    @Async
    protected void sendEmailToUser(
            String to,
            String username,
            String templateName,
            String activationCode,
            String subject
    ) throws MessagingException {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(sender);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.debug", "true");

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                StandardCharsets.UTF_8.name()
        );
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("activationCode", activationCode);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject(subject);

        String htmlTemplate = springTemplateEngine.process(templateName, context);
        helper.setText(htmlTemplate, true);

        mailSender.send(mimeMessage);
    }
}
