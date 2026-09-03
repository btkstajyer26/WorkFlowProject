package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.auth.repository.PasswordResetCodeRepository;
import btk.staj.WorkFlowProject.notification.repository.MailActionTokenRepository;
import btk.staj.WorkFlowProject.user.entity.Token;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TokenCleanupJob")
class TokenCleanupJobTest {

    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final PasswordResetCodeRepository passwordResetCodeRepository =
            mock(PasswordResetCodeRepository.class);
    private final MailActionTokenRepository mailActionTokenRepository =
            mock(MailActionTokenRepository.class);

    private final TokenCleanupJob job = new TokenCleanupJob(
            tokenRepository, passwordResetCodeRepository, mailActionTokenRepository);

    @Test
    @DisplayName("suresi dolmus oturum tokenlarini expired olarak isaretler")
    void marksExpiredSessionTokens() {
        Token first = new Token();
        Token second = new Token();
        when(tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(any()))
                .thenReturn(List.of(first, second));

        job.cleanUpExpiredCredentials();

        assertThat(first.isExpired()).isTrue();
        assertThat(second.isExpired()).isTrue();
        verify(tokenRepository).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("suresi gecmis mail aksiyon anahtarlarini siler")
    void deletesExpiredMailActionTokens() {
        when(tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(any())).thenReturn(List.of());

        job.cleanUpExpiredCredentials();

        verify(mailActionTokenRepository).deleteByExpiresAtBefore(any());
    }

    /**
     * Bekletme suresi kaza eseri sifira dusmemeli: "baglanti gonderildi ama kullanilmadi"
     * durumunun bir sure gorunur kalmasi bilincli. Esik <strong>gecmiste</strong> olmali,
     * yani suresi henuz yeni dolmus anahtarlar bu turda silinmemeli.
     */
    @Test
    @DisplayName("silme esigi gecmiste; yeni suresi dolmus anahtarlar bir sure korunur")
    void keepsRecentlyExpiredRowsForOneRetentionWindow() {
        when(tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        job.cleanUpExpiredCredentials();

        ArgumentCaptor<LocalDateTime> mailThreshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mailActionTokenRepository).deleteByExpiresAtBefore(mailThreshold.capture());
        assertThat(mailThreshold.getValue())
                .as("mail anahtari silme esigi")
                .isBefore(before);

        ArgumentCaptor<LocalDateTime> resetThreshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(passwordResetCodeRepository).deleteByExpiresAtBefore(resetThreshold.capture());
        assertThat(resetThreshold.getValue())
                .as("parola kodu silme esigi")
                .isBefore(before);
    }

    @Test
    @DisplayName("uc temizligi de ayni kosuda yapar")
    void cleansAllThreeStoresInOneRun() {
        when(tokenRepository.findAllByExpiredFalseAndExpiresAtBefore(any())).thenReturn(List.of());

        job.cleanUpExpiredCredentials();

        verify(tokenRepository).saveAll(List.of());
        verify(passwordResetCodeRepository).deleteByExpiresAtBefore(any());
        verify(mailActionTokenRepository).deleteByExpiresAtBefore(any());
    }

    @Test
    @DisplayName("bagimlilik olmadan olusturulamaz")
    void rejectsMissingDependencies() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TokenCleanupJob(null, passwordResetCodeRepository, mailActionTokenRepository))
                .withMessageContaining("tokenRepository");
        assertThatNullPointerException()
                .isThrownBy(() -> new TokenCleanupJob(tokenRepository, null, mailActionTokenRepository))
                .withMessageContaining("passwordResetCodeRepository");
        assertThatNullPointerException()
                .isThrownBy(() -> new TokenCleanupJob(tokenRepository, passwordResetCodeRepository, null))
                .withMessageContaining("mailActionTokenRepository");
    }
}
