package revi1337.onsquad.inrastructure.mail.application;

public interface EmailSender { // TODO PATTERN 이거 Naver 는 잘되는데, GOOGLE 은 깨짐. 다 Table 로 바꿔야 가능할듯..?

    String PATTERN = """ 
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Document</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f5f5f5; font-family: 'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif;">
            <link rel="stylesheet" as="style" crossorigin href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css"/>
            <div style="display: flex; justify-content: center; align-items: center; width: 100%; height: 100%;">
              <div style="background: url({0}) no-repeat center; background-size: cover; width: 100%; max-width: 600px; height: 760px; gap: 28px; padding-bottom: 160px; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #181600;">
                <div style="display: flex; flex-direction: column; gap: 8px;">
                  <div>
                    <img src="{1}" alt="onsquad-logo"/>
                  </div>
                  <h1 style="text-align: center; padding: 0; margin: 0;">온스쿼드</h1>
                </div>
                <div>
                  <h2 style="text-align: center; margin: 0; font-size: 20px; font-weight: 700;">이메일 인증코드</h2>
                  <p style="text-align: center; font-weight: 500;">인증코드가 발급되었습니다. <br/>아래의 인증코드를 입력해 주세요.</p>
                </div>
                <div style="display: flex; flex-direction: column; align-items: center; gap: 12px;">
                  <div style="background-color: #e6e6e6; padding: 8px 12px;">
                    <span style="font-size: 24px; font-weight: 600; letter-spacing: 0.25em; line-height: 1.5;">{2}</span>
                  </div>
                  <span style="color: #909090;">유효기간: {3} 까지</span>
                </div>
                <div style="display: flex; flex-direction: column; align-items: center;">
                  <div style="display: flex; align-items: center; gap: 4px; flex-direction: column; width: 100%; height: auto;">
                    <img style="width: 30px; height: 28px; object-fit: contain;" src="{4}" alt="onsquad-logo-suqare"/>
                    <span style="color: #ff6b00; font-size: 12px; font-weight: 700;">온스쿼드</span>
                  </div>
                  <p style="font-size: 12px; font-weight: 500; color: #909090; text-align: center;">취미생활의 아지트, 온스쿼드! <br/>지금 바로 합류하세요!</p>
                </div>
              </div>
            </div>
            </body>
            </html>
            """;

    void sendEmail(String subject, EmailContent content, String to);

    String buildEmailBody(EmailContent content);

}
