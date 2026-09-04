package btk.staj.WorkFlowProject.auth.service;

import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUserFactory;
import org.springframework.transaction.annotation.Transactional;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthenticatedUserFactory principalFactory;

    public CustomUserDetailsService(UserRepository userRepository, AuthenticatedUserFactory principalFactory) {
        this.userRepository = userRepository;
        this.principalFactory = principalFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        return principalFactory.create(user);
    }
}
