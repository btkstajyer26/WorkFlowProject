package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CustomUserDetailsService için birim testleri.
 * UserRepository'den dönen sonuca göre AuthenticatedUser üretilip
 * üretilmediğini ve kullanıcı bulunamadığında doğru exception'ın
 * fırlatıldığını doğrular.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        customUserDetailsService = new CustomUserDetailsService(userRepository);
    }

   @Test
    void loadUserByUsername_kullaniciVarsa_authenticatedUserDondurmeli() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(user.getId()).thenReturn(userId);

    UserDetails result = customUserDetailsService.loadUserByUsername("test@example.com");

    assertNotNull(result);
    assertInstanceOf(AuthenticatedUser.class, result);
    assertEquals(userId, ((AuthenticatedUser) result).getId());
    verify(userRepository, times(1)).findByEmail("test@example.com");
}

    @Test
    void loadUserByUsername_kullaniciBulunamazsa_usernameNotFoundExceptionFirlatmali() {
        when(userRepository.findByEmail("olmayan@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("olmayan@example.com"));

        assertEquals("Kullanıcı bulunamadı: olmayan@example.com", ex.getMessage());
        verify(userRepository, times(1)).findByEmail("olmayan@example.com");
    }
}