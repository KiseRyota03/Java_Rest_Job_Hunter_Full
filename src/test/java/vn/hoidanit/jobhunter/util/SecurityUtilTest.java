package vn.hoidanit.jobhunter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResLoginDTO;
import vn.hoidanit.jobhunter.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SecurityUtilTest {

    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private UserService userService;
    
    @Value("${hoidanit.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;
    
    @Value("${hoidanit.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    private User testUser;
    private ResLoginDTO testLoginDto;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test.user@example.com");
        testUser.setPassword("password123");
        testUser = userService.handleCreateUser(testUser);
        
        // Create test login DTO
        testLoginDto = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        userLogin.setId(testUser.getId());
        userLogin.setEmail(testUser.getEmail());
        userLogin.setName(testUser.getName());
        testLoginDto.setUser(userLogin);
    }

    /**
     * Test createAccessToken
     * Test ID: UT_SECURITY_01
     * Objective: Verify that a valid access token can be created
     * Input: Email and login DTO
     * Expected: Non-null token string
     */
    @Test
    public void testCreateAccessToken() {
        // When
        String token = securityUtil.createAccessToken(testUser.getEmail(), testLoginDto);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    /**
     * Test createRefreshToken
     * Test ID: UT_SECURITY_02
     * Objective: Verify that a valid refresh token can be created
     * Input: Email and login DTO
     * Expected: Non-null token string
     */
    @Test
    public void testCreateRefreshToken() {
        // When
        String token = securityUtil.createRefreshToken(testUser.getEmail(), testLoginDto);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    /**
     * Test checkValidRefreshToken with valid token
     * Test ID: UT_SECURITY_03
     * Objective: Verify that a valid refresh token can be validated
     * Input: Valid refresh token
     * Expected: Jwt object with correct claims
     */
    @Test
    public void testCheckValidRefreshToken() {
        // Given
        String refreshToken = securityUtil.createRefreshToken(testUser.getEmail(), testLoginDto);
        
        // When
        Jwt jwt = securityUtil.checkValidRefreshToken(refreshToken);
        
        // Then
        assertNotNull(jwt);
        assertEquals(testUser.getEmail(), jwt.getSubject());
        
        // Check user claim
        Object userClaim = jwt.getClaim("user");
        assertNotNull(userClaim);
    }

    /**
     * Test getCurrentUserLogin with authenticated user
     * Test ID: UT_SECURITY_04
     * Objective: Verify that the current user can be retrieved from security context
     * Input: Authentication in SecurityContext
     * Expected: Optional with correct username
     */
    @Test
    public void testGetCurrentUserLogin() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser.getEmail(), "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<String> currentUserLogin = SecurityUtil.getCurrentUserLogin();
        
        // Then
        assertTrue(currentUserLogin.isPresent());
        assertEquals(testUser.getEmail(), currentUserLogin.get());
        
        // Clean up
        SecurityContextHolder.clearContext();
    }

    /**
     * Test getCurrentUserLogin with no authentication
     * Test ID: UT_SECURITY_05
     * Objective: Verify handling of no authentication in security context
     * Input: Empty SecurityContext
     * Expected: Empty Optional
     */
    @Test
    public void testGetCurrentUserLoginNoAuth() {
        // Given
        SecurityContextHolder.clearContext();
        
        // When
        Optional<String> currentUserLogin = SecurityUtil.getCurrentUserLogin();
        
        // Then
        assertFalse(currentUserLogin.isPresent());
    }

    /**
     * Test getCurrentUserJWT
     * Test ID: UT_SECURITY_06
     * Objective: Verify JWT retrieval from security context
     * Input: Authentication with JWT credential
     * Expected: Optional with correct JWT string
     */
    @Test
    public void testGetCurrentUserJWT() {
        // Given
        String testJwt = "test-jwt-token";
        Authentication auth = new UsernamePasswordAuthenticationToken("principal", testJwt);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<String> jwt = SecurityUtil.getCurrentUserJWT();
        
        // Then
        assertTrue(jwt.isPresent());
        assertEquals(testJwt, jwt.get());
        
        // Clean up
        SecurityContextHolder.clearContext();
    }

    /**
     * Test getCurrentUserJWT with no JWT
     * Test ID: UT_SECURITY_07
     * Objective: Verify handling of no JWT in security context
     * Input: Authentication without JWT credential
     * Expected: Empty Optional
     */
    @Test
    public void testGetCurrentUserJWTNoJwt() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("principal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<String> jwt = SecurityUtil.getCurrentUserJWT();
        
        // Then
        assertFalse(jwt.isPresent());
        
        // Clean up
        SecurityContextHolder.clearContext();
    }
}
