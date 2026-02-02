package com.mystictarot.backend.util;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * Utility class for security-related operations
 */
public class SecurityUtils {

    /**
     * Extract current user ID from SecurityContext
     * @return UUID of current authenticated user
     * @throws AuthenticationCredentialsNotFoundException if user is not authenticated
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                return UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationCredentialsNotFoundException("Invalid user ID format: " + username);
            }
        }
        
        throw new AuthenticationCredentialsNotFoundException("Unable to extract user ID from authentication");
    }
}
