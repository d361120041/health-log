package tw.danielchiang.health_log.model.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 認證回應 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshTokenId;
    private String tokenType = "Bearer";
    private Long expiresIn; // Access token expiration in milliseconds
}

