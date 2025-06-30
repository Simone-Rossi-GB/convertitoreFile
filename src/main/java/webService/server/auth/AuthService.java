package webService.server.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import webService.server.auth.entity.UserEntity;
import webService.server.auth.repository.UserRepository;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, TokenInfo> activeTokens = new ConcurrentHashMap<>();

    // Token validity duration in minutes
    private static final int TOKEN_VALIDITY_MINUTES = 60;

    /**
     * Inizializza l'admin di default se non esiste
     */
    @PostConstruct
    public void initializeDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setFullName("Administrator");
            admin.setRole("ADMIN");
            admin.setActive(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
            System.out.println("Admin di default creato: username=admin, password=admin123");
        }
    }

    public AuthResponse authenticate(LoginRequest loginRequest) throws AuthException {
        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(loginRequest.getUsername());

        if (!userEntityOpt.isPresent()) {
            throw new AuthException("Invalid username or password");
        }

        UserEntity userEntity = userEntityOpt.get();

        if (!userEntity.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword())) {
            throw new AuthException("Invalid username or password");
        }

        // Update last login
        userEntity.setLastLogin(LocalDateTime.now());
        userRepository.save(userEntity);

        // Generate token
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);

        // Store token info
        TokenInfo tokenInfo = new TokenInfo(token, userEntity.getUsername(), expiresAt);
        activeTokens.put(token, tokenInfo);

        // Create response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(userEntity.toUser());
        response.setExpiresAt(expiresAt);

        return response;
    }

    public void logout(String token) throws AuthException {
        if (!activeTokens.containsKey(token)) {
            throw new AuthException("Invalid token");
        }
        activeTokens.remove(token);
    }

    public User getCurrentUser(String token) throws AuthException {
        TokenInfo tokenInfo = activeTokens.get(token);

        if (tokenInfo == null) {
            throw new AuthException("Invalid token");
        }

        if (tokenInfo.getExpiresAt().isBefore(LocalDateTime.now())) {
            activeTokens.remove(token);
            throw new AuthException("Token expired");
        }

        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(tokenInfo.getUsername());
        if (!userEntityOpt.isPresent()) {
            activeTokens.remove(token);
            throw new AuthException("User not found");
        }

        return userEntityOpt.get().toUser();
    }

    public User registerUser(RegisterRequest registerRequest) throws AuthException {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new AuthException("Username already exists");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new AuthException("Email already exists");
        }

        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            throw new AuthException("Username is required");
        }

        if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 6) {
            throw new AuthException("Password must be at least 6 characters long");
        }

        if (registerRequest.getEmail() == null || !isValidEmail(registerRequest.getEmail())) {
            throw new AuthException("Valid email is required");
        }

        UserEntity newUserEntity = new UserEntity();
        newUserEntity.setUsername(registerRequest.getUsername());
        newUserEntity.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUserEntity.setEmail(registerRequest.getEmail());
        newUserEntity.setFullName(registerRequest.getFullName());
        newUserEntity.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : "USER");
        newUserEntity.setActive(true);
        newUserEntity.setCreatedAt(LocalDateTime.now());

        UserEntity savedUser = userRepository.save(newUserEntity);
        return savedUser.toUser();
    }

    /**
     * Verifica se l'utente esiste
     */
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Ottiene tutti gli utenti (solo per admin)
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userEntity -> new UserDTO(userEntity.toUser()))
                .sorted((u1, u2) -> u1.getCreatedAt().compareTo(u2.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Disattiva un utente
     */
    public boolean deactivateUser(String username) {
        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(username);
        if (userEntityOpt.isPresent()) {
            UserEntity userEntity = userEntityOpt.get();
            userEntity.setActive(false);
            userRepository.save(userEntity);

            // Invalida tutti i token dell'utente
            activeTokens.entrySet().removeIf(entry ->
                    entry.getValue().getUsername().equals(username));
            return true;
        }
        return false;
    }

    /**
     * Attiva un utente
     */
    public boolean activateUser(String username) {
        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(username);
        if (userEntityOpt.isPresent()) {
            UserEntity userEntity = userEntityOpt.get();
            userEntity.setActive(true);
            userRepository.save(userEntity);
            return true;
        }
        return false;
    }

    /**
     * Cambia la password di un utente
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) throws AuthException {
        Optional<UserEntity> userEntityOpt = userRepository.findByUsername(username);
        if (!userEntityOpt.isPresent()) {
            throw new AuthException("User not found");
        }

        UserEntity userEntity = userEntityOpt.get();

        if (!passwordEncoder.matches(oldPassword, userEntity.getPassword())) {
            throw new AuthException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new AuthException("New password must be at least 6 characters long");
        }

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);

        // Invalida tutti i token dell'utente per forzare un nuovo login
        activeTokens.entrySet().removeIf(entry ->
                entry.getValue().getUsername().equals(username));

        return true;
    }

    /**
     * Pulisce i token scaduti
     */
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        activeTokens.entrySet().removeIf(entry ->
                entry.getValue().getExpiresAt().isBefore(now));
    }

    /**
     * Ottiene il numero di utenti attivi
     */
    public long getActiveUsersCount() {
        return userRepository.countActiveUsers();
    }

    /**
     * Ottiene il numero di sessioni attive
     */
    public int getActiveSessionsCount() {
        cleanExpiredTokens();
        return activeTokens.size();
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Inner class for token management
    private static class TokenInfo {
        private final String token;
        private final String username;
        private final LocalDateTime expiresAt;

        public TokenInfo(String token, String username, LocalDateTime expiresAt) {
            this.token = token;
            this.username = username;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}