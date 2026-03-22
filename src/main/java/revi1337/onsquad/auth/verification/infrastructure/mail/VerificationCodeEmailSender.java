package revi1337.onsquad.auth.verification.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import revi1337.onsquad.auth.verification.domain.VerificationCode;
import revi1337.onsquad.common.application.mail.EmailSender;
import revi1337.onsquad.infrastructure.network.mail.EmailException;

@Slf4j
@Component
public class VerificationCodeEmailSender implements EmailSender<VerificationCode> {

    private static final String CLASSPATH_MAIL_TEMPLATE = "template/mail/verification-code.html";
    private static final String MIME_SETTING_ERROR_LOG = "MimeMessage 설정 중 예외 발생 - 메일 발송 중단";
    private static final String SEND_VERIFICATION_CODE_ERROR_LOG = "이메일 인증코드 발송 중 예외 발생";
    private static final String ONSQUAD_MAIL_BACKGROUND = "/onsquad/mail/background.jpg";
    private static final String ONSQUAD_PRIMARY_LOGO = "/onsquad/mail/logo-row-primary.png";
    private static final String ONSQUAD_SQUARE_LOGO = "/onsquad/mail/logo-square-primary.png";
    private static final DateTimeFormatter VERIFICATION_CODE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender javaMailSender;
    private final String cloudfrontBaseDomain;
    private final String emailTemplate;

    public VerificationCodeEmailSender(JavaMailSender javaMailSender, @Value("${onsquad.aws.cloud-front.base-domain}") String cloudfrontBaseDomain) {
        this.javaMailSender = javaMailSender;
        this.cloudfrontBaseDomain = cloudfrontBaseDomain;
        this.emailTemplate = loadTemplate();
    }

    @Override
    public void sendEmail(String subject, VerificationCode verificationCode, String to) {
        try {
            MimeMessage mimeMessage = createMimeMessage(subject, verificationCode, to);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error(MIME_SETTING_ERROR_LOG, e);
            throw new EmailException.MimeSettingError(MIME_SETTING_ERROR_LOG, e);
        } catch (MailException e) {
            log.error(SEND_VERIFICATION_CODE_ERROR_LOG, e);
            throw new EmailException.SendError(SEND_VERIFICATION_CODE_ERROR_LOG, e);
        }
    }

    private MimeMessage createMimeMessage(String subject, VerificationCode verificationCode, String to) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

        String baseHyperText = buildEmailBody(verificationCode);
        mimeMessageHelper.setText(baseHyperText, true);
        mimeMessageHelper.setTo(to);
        mimeMessageHelper.setSubject(subject);

        return mimeMessage;
    }

    private String buildEmailBody(VerificationCode verificationCode) {
        return MessageFormat.format(
                emailTemplate,
                cloudfrontBaseDomain + ONSQUAD_MAIL_BACKGROUND,
                cloudfrontBaseDomain + ONSQUAD_PRIMARY_LOGO,
                verificationCode.getCode(),
                verificationCode.getExpiredAt().format(VERIFICATION_CODE_DATETIME_FORMATTER),
                cloudfrontBaseDomain + ONSQUAD_SQUARE_LOGO
        );
    }

    private String loadTemplate() {
        try {
            Resource resource = new ClassPathResource(CLASSPATH_MAIL_TEMPLATE);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[EmailSender] 메일 템플릿 로드 실패", e);
            throw new UncheckedIOException(e);
        }
    }
}
