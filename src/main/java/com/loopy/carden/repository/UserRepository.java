package com.loopy.carden.repository;

import com.loopy.carden.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides methods for user authentication and user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by username
     * @param username the username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email or username (used for login)
     * @param email the email address
     * @param username the username
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);

    /**
     * Checks if a user exists by email
     * @param email the email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists by username
     * @param username the username
     * @return true if user exists
     */
    boolean existsByUsername(String username);

    /**
     * Finds active users by email
     * @param email the email address
     * @return Optional containing the active user if found
     */
    Optional<User> findByEmailAndIsActiveTrue(String email);

    /**
     * Finds active users by username
     * @param username the username
     * @return Optional containing the active user if found
     */
    Optional<User> findByUsernameAndIsActiveTrue(String username);
}
