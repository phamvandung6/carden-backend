package com.loopy.carden.security;

import com.loopy.carden.entity.User;
import com.loopy.carden.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation that loads user details from the database.
 * This service is used by Spring Security for authentication and authorization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByEmailOrUsername(username, username)
                .orElseThrow(() -> {
                    log.warn("User not found with username/email: {}", username);
                    return new UsernameNotFoundException("User not found with username/email: " + username);
                });

        log.debug("Successfully loaded user: {} with role: {}", user.getEmail(), user.getRole());
        return user;
    }

    /**
     * Loads user by ID for internal use
     * @param id the user ID
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new UsernameNotFoundException("User not found with ID: " + id);
                });

        log.debug("Successfully loaded user by ID: {} - {}", id, user.getEmail());
        return user;
    }
}
