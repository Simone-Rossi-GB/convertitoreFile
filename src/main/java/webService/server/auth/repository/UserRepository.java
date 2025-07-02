package webService.server.auth.repository;

import webService.server.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Trova un utente per username
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Trova un utente per email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Verifica se esiste un utente con questo username
     */
    boolean existsByUsername(String username);

    /**
     * Verifica se esiste un utente con questa email
     */
    boolean existsByEmail(String email);

    /**
     * Trova tutti gli utenti attivi
     */
    List<UserEntity> findByIsActiveTrue();

    /**
     * Trova utenti per ruolo
     */
    List<UserEntity> findByRole(String role);

    /**
     * Conta gli utenti attivi
     */
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * Trova utenti il cui username contiene la stringa specificata
     */
    List<UserEntity> findByUsernameContainingIgnoreCase(String username);
}