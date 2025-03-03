package com.example.backend.email;

import com.example.backend.enums.TokenType;
import com.example.backend.token.Token;
import com.example.backend.token.TokenRepository;
import com.example.backend.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
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

    public String sendForgotPasswordEmail(User user) throws MessagingException {
        System.out.println("forgot password service: working");
        String generatedCode = generateActivationCode();

        Token token = Token.builder()
                .token(generatedCode)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .tokenType(TokenType.FORGOT_PASSWORD)
                .build();

        tokenRepository.save(token);

        this.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.FORGOT_PASSWORD,
                generatedCode,
                "Forgot password"
        );
        return generatedCode;
    }

    public String sendValidationEmail(User user) throws MessagingException {
        String activationToken = generateActivationCode();
        var token = Token.builder()
                .token(activationToken)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .tokenType(TokenType.ACTIVATION_ACCOUNT)
                .build();

        tokenRepository.save(token);

        sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationToken,
                "Activation account"
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

    @Async
    protected void sendEmail(
            String to,
            String username,
            EmailTemplateName template,
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

        String templateName = template.getName();

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
