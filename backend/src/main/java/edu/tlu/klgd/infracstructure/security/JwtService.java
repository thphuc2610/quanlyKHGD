package edu.tlu.klgd.infracstructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tlu.klgd.domain.common.ApiMessage;
import edu.tlu.klgd.domain.entity.AppUser;
import edu.tlu.klgd.infracstructure.config.AppPropertyConstant;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
        ObjectMapper objectMapper,
        @Value(AppPropertyConstant.JWT_SECRET) String secret,
        @Value(AppPropertyConstant.JWT_EXPIRATION_MINUTES) long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * JwtConstant.SECONDS_PER_MINUTE;
    }

    public String generate(AppUser user) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = Map.of(
                JwtConstant.HEADER_ALGORITHM_KEY,
                JwtConstant.HMAC_SHA256_ALGORITHM,
                JwtConstant.HEADER_TYPE_KEY,
                JwtConstant.JWT_TYPE
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put(JwtConstant.SUBJECT_CLAIM, user.getUsername());
            payload.put(JwtConstant.USER_ID_CLAIM, user.getId());
            payload.put(JwtConstant.NAME_CLAIM, user.getFullName());
            payload.put(JwtConstant.ROLES_CLAIM, user.getRoles().stream().map(Enum::name).toList());
            payload.put(JwtConstant.ISSUED_AT_CLAIM, now.getEpochSecond());
            payload.put(JwtConstant.EXPIRES_AT_CLAIM, now.plusSeconds(expirationSeconds).getEpochSecond());

            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signaturePart = sign(headerPart + JwtConstant.JWT_PART_SEPARATOR + payloadPart);
            return headerPart + JwtConstant.JWT_PART_SEPARATOR + payloadPart + JwtConstant.JWT_PART_SEPARATOR + signaturePart;
        } catch (Exception exception) {
            throw new IllegalStateException(ApiMessage.JWT_GENERATION_FAILED, exception);
        }
    }

    public Optional<JwtPrincipal> parse(String token) {
        try {
            String[] parts = token.split(JwtConstant.JWT_PART_SEPARATOR_REGEX);
            if (parts.length != JwtConstant.JWT_PART_COUNT) {
                return Optional.empty();
            }
            String expectedSignature = sign(parts[JwtConstant.JWT_HEADER_INDEX] + JwtConstant.JWT_PART_SEPARATOR + parts[JwtConstant.JWT_PAYLOAD_INDEX]);
            if (!MessageDigestSupport.constantTimeEquals(expectedSignature, parts[JwtConstant.JWT_SIGNATURE_INDEX])) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(
                URL_DECODER.decode(parts[JwtConstant.JWT_PAYLOAD_INDEX]),
                new TypeReference<>() {}
            );
            long exp = ((Number) payload.get(JwtConstant.EXPIRES_AT_CLAIM)).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            String username = String.valueOf(payload.get(JwtConstant.SUBJECT_CLAIM));
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.getOrDefault(JwtConstant.ROLES_CLAIM, List.of());
            return Optional.of(new JwtPrincipal(username, roles));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Object value) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance(JwtConstant.HMAC_SHA256_JCA_NAME);
        mac.init(new SecretKeySpec(secret, JwtConstant.HMAC_SHA256_JCA_NAME));
        return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    public record JwtPrincipal(String username, List<String> roles) {
    }

    private static final class MessageDigestSupport {
        private MessageDigestSupport() {
        }

        static boolean constantTimeEquals(String left, String right) {
            return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
