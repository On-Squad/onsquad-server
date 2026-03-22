package revi1337.onsquad.common.application.mail;

import revi1337.onsquad.common.domain.EmailContent;

public interface EmailSender<T extends EmailContent> {

    void sendEmail(String subject, T content, String to);

}
