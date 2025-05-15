package com.leun.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.leun.user.entity.User;
import com.leun.user.entity.User.UserRole;
import com.leun.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername - 사용자를 찾으면 UserDetails 객체 반환")
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        // Given
        String email = "test@example.com";
        String password = "encodedPassword123";
        UserRole userRole = UserRole.ROLE_USER;

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setPassword(password);
        mockUser.setUserRole(userRole);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals(new SimpleGrantedAuthority(String.valueOf(userRole)), authority);

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("loadUserByUsername - 사용자를 찾을 수 없으면 UsernameNotFoundException 발생")
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";

        given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(nonExistentEmail);
        });

        assertEquals("User not found with email: " + nonExistentEmail, thrown.getMessage());

        verify(userRepository, times(1)).findByEmail(nonExistentEmail);
    }
}
