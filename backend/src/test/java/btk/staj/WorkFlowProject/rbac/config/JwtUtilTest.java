package btk.staj.WorkFlowProject.rbac.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-tests-must-be-long-enough-1234567890");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 86400000L);
    }

    @Test
    void ayniKullaniciIcinPesPeseIkiRefreshTokenFarkliOlmali() {
        UUID userId = UUID.randomUUID();

        String token1 = jwtUtil.generateRefreshToken(userId);
        String token2 = jwtUtil.generateRefreshToken(userId);

        assertNotEquals(token1, token2);
    }

    @Test
    void uretilenRefreshTokenGecerliOlmali() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateRefreshToken(userId);

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void uretilenRefreshTokendanSubjectDogruCozulmeli() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateRefreshToken(userId);

        assertEquals(userId.toString(), jwtUtil.extractEmail(token));
    }
}