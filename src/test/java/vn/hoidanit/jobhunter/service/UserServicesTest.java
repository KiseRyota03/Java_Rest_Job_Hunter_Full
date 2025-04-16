package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUpdateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserServicesTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private RoleService roleService;

    private User testUser;
    private Company testCompany;
    private Role testRole;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test company
        testCompany = new Company();
        testCompany.setName("Test Company");
        testCompany.setAddress("Test Address");
        testCompany = companyService.handleCreateCompany(testCompany);

        // Create test role
        testRole = new Role();
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Test Role Description");
        testRole.setActive(true);
        testRole = roleService.create(testRole);

        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test.user@example.com");
        testUser.setPassword("password123");
        testUser.setAge(30);
        testUser.setGender(GenderEnum.MALE);
        testUser.setAddress("Test Address");
    }

    /**
     * Test handleCreateUser with a valid user
     * Test ID: UT_USER_01
     * Objective: Verify that a valid user can be created successfully
     * Input: Valid User object
     * Expected: User saved with generated ID
     */
    @Test
    public void testHandleCreateUser() {
        // When
        User createdUser = userService.handleCreateUser(testUser);

        // Then
        assertNotNull(createdUser);
        assertThat(createdUser.getId()).isGreaterThan(0);
        assertEquals("Test User", createdUser.getName());
        assertEquals("test.user@example.com", createdUser.getEmail());

        // Check database - verify the user exists
        Optional<User> dbUser = userRepository.findById(createdUser.getId());
        assertTrue(dbUser.isPresent());
    }

    /**
     * Test handleCreateUser with company association
     * Test ID: UT_USER_02
     * Objective: Verify that a user can be associated with a company
     * Input: User with company ID
     * Expected: User saved with correct company association
     */
    @Test
    public void testHandleCreateUserWithCompany() {
        // Given
        testUser.setCompany(testCompany);

        // When
        User createdUser = userService.handleCreateUser(testUser);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getCompany());
        assertEquals(testCompany.getId(), createdUser.getCompany().getId());
        assertEquals(testCompany.getName(), createdUser.getCompany().getName());
    }

    /**
     * Test handleCreateUser with role association
     * Test ID: UT_USER_03
     * Objective: Verify that a user can be associated with a role
     * Input: User with role ID
     * Expected: User saved with correct role association
     */
    @Test
    public void testHandleCreateUserWithRole() {
        // Given
        testUser.setRole(testRole);

        // When
        User createdUser = userService.handleCreateUser(testUser);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getRole());
        assertEquals(testRole.getId(), createdUser.getRole().getId());
        assertEquals(testRole.getName(), createdUser.getRole().getName());
    }

    /**
     * Test handleDeleteUser with existing user
     * Test ID: UT_USER_04
     * Objective: Verify that an existing user can be deleted
     * Input: Valid user ID
     * Expected: User deleted from database
     */
    @Test
    public void testHandleDeleteUser() {
        // Given
        User createdUser = userService.handleCreateUser(testUser);
        long userId = createdUser.getId();

        // When
        userService.handleDeleteUser(userId);

        // Then
        Optional<User> dbUser = userRepository.findById(userId);
        assertFalse(dbUser.isPresent());
    }

    /**
     * Test handleDeleteUser with non-existing user
     * Test ID: UT_USER_05
     * Objective: Verify graceful handling of deletion for non-existing user
     * Input: Invalid user ID
     * Expected: No exception thrown
     */
    @Test
    public void testHandleDeleteUserNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When/Then
        assertDoesNotThrow(() -> userService.handleDeleteUser(nonExistingId));
    }

    /**
     * Test fetchUserById with existing user
     * Test ID: UT_USER_06
     * Objective: Verify that an existing user can be fetched by ID
     * Input: Valid user ID
     * Expected: Correct user returned
     */
    @Test
    public void testFetchUserById() {
        // Given
        User createdUser = userService.handleCreateUser(testUser);
        long userId = createdUser.getId();

        // When
        User fetchedUser = userService.fetchUserById(userId);

        // Then
        assertNotNull(fetchedUser);
        assertEquals(userId, fetchedUser.getId());
        assertEquals(testUser.getName(), fetchedUser.getName());
        assertEquals(testUser.getEmail(), fetchedUser.getEmail());
    }

    /**
     * Test fetchUserById with non-existing user
     * Test ID: UT_USER_07
     * Objective: Verify handling of non-existent user
     * Input: Invalid user ID
     * Expected: null returned
     */
    @Test
    public void testFetchUserByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        User fetchedUser = userService.fetchUserById(nonExistingId);

        // Then
        assertNull(fetchedUser);
    }

    /**
     * Test fetchAllUser with pagination
     * Test ID: UT_USER_08
     * Objective: Verify that users can be fetched with pagination
     * Input: Specification, Pageable
     * Expected: Correctly paginated users
     */
    @Test
    public void testFetchAllUser() {
        // Given
        userService.handleCreateUser(testUser);

        User user2 = new User();
        user2.setName("Second User");
        user2.setEmail("second.user@example.com");
        user2.setPassword("password123");
        userService.handleCreateUser(user2);

        Pageable pageable = PageRequest.of(0, 10);
        Specification<User> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = userService.fetchAllUser(spec, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertNotNull(result.getResult());

        // Check pagination metadata
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
        assertTrue(result.getMeta().getTotal() >= 2);
    }

    /**
     * Test handleUpdateUser with valid user
     * Test ID: UT_USER_09
     * Objective: Verify that a user can be updated
     * Input: Valid User object
     * Expected: Updated user returned
     */
    @Test
    public void testHandleUpdateUser() {
        // Given
        User createdUser = userService.handleCreateUser(testUser);
        long userId = createdUser.getId();

        // Create update request
        User updateRequest = new User();
        updateRequest.setId(userId);
        updateRequest.setName("Updated Name");
        updateRequest.setAddress("Updated Address");
        updateRequest.setAge(35);
        updateRequest.setGender(GenderEnum.FEMALE);

        // When
        User updatedUser = userService.handleUpdateUser(updateRequest);

        // Then
        assertNotNull(updatedUser);
        assertEquals(userId, updatedUser.getId());
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("Updated Address", updatedUser.getAddress());
        assertEquals(35, updatedUser.getAge());
        assertEquals(GenderEnum.FEMALE, updatedUser.getGender());

        // Check email hasn't changed (not in update request)
        assertEquals(testUser.getEmail(), updatedUser.getEmail());
    }

    /**
     * Test handleUpdateUser with non-existing user
     * Test ID: UT_USER_10
     * Objective: Verify handling of update for non-existent user
     * Input: Invalid user ID
     * Expected: null returned
     */
    @Test
    public void testHandleUpdateUserNonExisting() {
        // Given
        User updateRequest = new User();
        updateRequest.setId(99999L);
        updateRequest.setName("Updated Name");

        // When
        User updatedUser = userService.handleUpdateUser(updateRequest);

        // Then
        assertNull(updatedUser);
    }

    /**
     * Test handleGetUserByUsername with valid email
     * Test ID: UT_USER_11
     * Objective: Verify that a user can be retrieved by email
     * Input: Valid email
     * Expected: Correct user returned
     */
    @Test
    public void testHandleGetUserByUsername() {
        // Given
        userService.handleCreateUser(testUser);
        String email = testUser.getEmail();

        // When
        User fetchedUser = userService.handleGetUserByUsername(email);

        // Then
        assertNotNull(fetchedUser);
        assertEquals(email, fetchedUser.getEmail());
        assertEquals(testUser.getName(), fetchedUser.getName());
    }

    /**
     * Test handleGetUserByUsername with non-existing email
     * Test ID: UT_USER_12
     * Objective: Verify handling of non-existent username
     * Input: Invalid email
     * Expected: null returned
     */
    @Test
    public void testHandleGetUserByUsernameNonExisting() {
        // Given
        String nonExistingEmail = "nonexisting@example.com";

        // When
        User fetchedUser = userService.handleGetUserByUsername(nonExistingEmail);

        // Then
        assertNull(fetchedUser);
    }

    /**
     * Test isEmailExist with existing email
     * Test ID: UT_USER_13
     * Objective: Verify email existence check
     * Input: Existing email
     * Expected: true
     */
    @Test
    public void testIsEmailExistWithExistingEmail() {
        // Given
        userService.handleCreateUser(testUser);
        String email = testUser.getEmail();

        // When
        boolean exists = userService.isEmailExist(email);

        // Then
        assertTrue(exists);
    }

    /**
     * Test isEmailExist with non-existing email
     * Test ID: UT_USER_14
     * Objective: Verify email non-existence check
     * Input: Non-existing email
     * Expected: false
     */
    @Test
    public void testIsEmailExistWithNonExistingEmail() {
        // Given
        String nonExistingEmail = "nonexisting@example.com";

        // When
        boolean exists = userService.isEmailExist(nonExistingEmail);

        // Then
        assertFalse(exists);
    }

    /**
     * Test updateUserToken with valid user
     * Test ID: UT_USER_15
     * Objective: Verify that a user's refresh token can be updated
     * Input: Valid token, email
     * Expected: User updated with token
     */
    @Test
    public void testUpdateUserToken() {
        // Given
        userService.handleCreateUser(testUser);
        String email = testUser.getEmail();
        String token = "test-refresh-token";

        // When
        userService.updateUserToken(token, email);

        // Then
        User updatedUser = userService.handleGetUserByUsername(email);
        assertNotNull(updatedUser);
        assertEquals(token, updatedUser.getRefreshToken());
    }

    /**
     * Test updateUserToken with non-existing user
     * Test ID: UT_USER_16
     * Objective: Verify graceful handling of token update for non-existing user
     * Input: Valid token, invalid email
     * Expected: No action
     */
    @Test
    public void testUpdateUserTokenNonExisting() {
        // Given
        String nonExistingEmail = "nonexisting@example.com";
        String token = "test-refresh-token";

        // When/Then
        assertDoesNotThrow(() -> userService.updateUserToken(token, nonExistingEmail));
    }

    /**
     * Test getUserByRefreshTokenAndEmail with valid data
     * Test ID: UT_USER_17
     * Objective: Verify that a user can be retrieved by refresh token and email
     * Input: Valid token, email
     * Expected: Correct user returned
     */
    @Test
    public void testGetUserByRefreshTokenAndEmail() {
        // Given
        User createdUser = userService.handleCreateUser(testUser);
        String email = createdUser.getEmail();
        String token = "test-refresh-token";

        // Update the user with the token
        userService.updateUserToken(token, email);

        // When
        User fetchedUser = userService.getUserByRefreshTokenAndEmail(token, email);

        // Then
        assertNotNull(fetchedUser);
        assertEquals(email, fetchedUser.getEmail());
        assertEquals(token, fetchedUser.getRefreshToken());
    }

    /**
     * Test getUserByRefreshTokenAndEmail with invalid data
     * Test ID: UT_USER_18
     * Objective: Verify handling of invalid token
     * Input: Invalid token, email
     * Expected: null returned
     */
    @Test
    public void testGetUserByRefreshTokenAndEmailInvalid() {
        // Given
        userService.handleCreateUser(testUser);
        String email = testUser.getEmail();
        String invalidToken = "invalid-token";

        // When
        User fetchedUser = userService.getUserByRefreshTokenAndEmail(invalidToken, email);

        // Then
        assertNull(fetchedUser);
    }

    /**
     * Test DTO conversion methods
     * Test ID: UT_USER_19
     * Objective: Verify all DTO conversion methods work correctly
     * Input: User object
     * Expected: Correctly converted DTOs
     */
    @Test
    public void testDtoConversionMethods() {
        // Given
        testUser.setCompany(testCompany);
        testUser.setRole(testRole);
        User createdUser = userService.handleCreateUser(testUser);

        // When
        ResCreateUserDTO createDto = userService.convertToResCreateUserDTO(createdUser);
        ResUpdateUserDTO updateDto = userService.convertToResUpdateUserDTO(createdUser);
        ResUserDTO userDto = userService.convertToResUserDTO(createdUser);

        // Then - Check CreateDTO
        assertNotNull(createDto);
        assertEquals(createdUser.getId(), createDto.getId());
        assertEquals(createdUser.getName(), createDto.getName());
        assertEquals(createdUser.getEmail(), createDto.getEmail());
        assertEquals(createdUser.getGender(), createDto.getGender());
        assertEquals(createdUser.getAddress(), createDto.getAddress());
        assertEquals(createdUser.getAge(), createDto.getAge());
        assertNotNull(createDto.getCompany());
        assertEquals(testCompany.getId(), createDto.getCompany().getId());

        // Check UpdateDTO
        assertNotNull(updateDto);
        assertEquals(createdUser.getId(), updateDto.getId());
        assertEquals(createdUser.getName(), updateDto.getName());
        assertEquals(createdUser.getGender(), updateDto.getGender());
        assertEquals(createdUser.getAddress(), updateDto.getAddress());
        assertEquals(createdUser.getAge(), updateDto.getAge());
        assertNotNull(updateDto.getCompany());
        assertEquals(testCompany.getId(), updateDto.getCompany().getId());

        // Check UserDTO
        assertNotNull(userDto);
        assertEquals(createdUser.getId(), userDto.getId());
        assertEquals(createdUser.getName(), userDto.getName());
        assertEquals(createdUser.getEmail(), userDto.getEmail());
        assertEquals(createdUser.getGender(), userDto.getGender());
        assertEquals(createdUser.getAddress(), userDto.getAddress());
        assertEquals(createdUser.getAge(), userDto.getAge());
        assertNotNull(userDto.getCompany());
        assertEquals(testCompany.getId(), userDto.getCompany().getId());
        assertNotNull(userDto.getRole());
        assertEquals(testRole.getId(), userDto.getRole().getId());
    }
}