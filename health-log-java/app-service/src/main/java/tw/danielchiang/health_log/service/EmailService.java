package tw.danielchiang.health_log.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Email 服務
 * 處理郵件發送相關業務邏輯
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.fromName:Health Log}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * 發送 Email 驗證郵件
     * @param to 收件人 Email
     * @param token 驗證 Token
     */
    public void sendVerificationEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            
            // 設定發送者（使用顯示名稱和實際 Email 地址）
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject("請驗證您的 Email 地址");
            helper.setText(buildVerificationEmailContent(to, token), false);
            
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * 構建驗證郵件內容
     * @param email 使用者 Email
     * @param token 驗證 Token
     * @return 郵件內容
     */
    private String buildVerificationEmailContent(String email, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        
        return String.format(
            "親愛的 %s，\n\n" +
            "感謝您註冊 Health Log 應用程式！\n\n" +
            "請點擊以下連結驗證您的 Email 地址：\n" +
            "%s\n\n" +
            "如果上述連結無法點擊，請複製並貼上到瀏覽器地址欄中。\n\n" +
            "此連結將在 24 小時後過期。\n\n" +
            "如果您沒有註冊此帳號，請忽略此郵件。\n\n" +
            "祝您使用愉快！\n" +
            "Health Log 團隊",
            email,
            verificationUrl
        );
    }
}

