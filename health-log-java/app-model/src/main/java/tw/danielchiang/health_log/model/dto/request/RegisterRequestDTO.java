package tw.danielchiang.health_log.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 註冊請求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "電子郵件不能為空")
    @Email(message = "電子郵件格式不正確")
    private String email;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, message = "密碼長度至少為 8 個字符")
    private String password;

    @NotBlank(message = "確認密碼不能為空")
    private String confirmPassword;
}

