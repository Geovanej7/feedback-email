package com.br.email.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.br.email.model.dto.FeedbackDto;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

@Service
public class EmailService {

    private final ConcurrentMap<String, LocalDateTime> ultimoEnvioPorEmail = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String starttls;

    public String enviarEmailFeedback(FeedbackDto dto) {
        String emailUsuario = dto.getEmail();
        LocalDateTime agora = LocalDateTime.now();

        // Verifica se já foi enviado e se passou menos de 5 minutos
        if (ultimoEnvioPorEmail.containsKey(emailUsuario)) {
            LocalDateTime ultimoEnvio = ultimoEnvioPorEmail.get(emailUsuario);
            Duration duracao = Duration.between(ultimoEnvio, agora);
            if (duracao.toMinutes() < 5) {
                long segundosRestantes = 300 - duracao.getSeconds();
                return "Aguarde " + segundosRestantes + " segundos para enviar outro feedback.";
            }
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", smtpAuth);
            props.put("mail.smtp.starttls.enable", starttls);
            props.put("mail.debug", "false");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            helper.setFrom(new InternetAddress(username));
            helper.setTo(username);
            helper.setReplyTo(dto.getEmail());
            helper.setSubject(dto.getAssunto());

            String corpoHtml = String.format(
                "<p><strong>Email do cliente:</strong> %s</p>" +
                "<p><strong>Mensagem:</strong><br>%s</p>",
                dto.getEmail(), dto.getMensagem()
            );

            helper.setText(new String(corpoHtml.getBytes(), StandardCharsets.ISO_8859_1), true);
            helper.setEncodeFilenames(true);

            mailSender.send(message);

            // Atualiza o tempo do último envio
            ultimoEnvioPorEmail.put(emailUsuario, agora);

            return "Feedback enviado com sucesso!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao enviar email: " + e.getMessage();
        }
    }
}
