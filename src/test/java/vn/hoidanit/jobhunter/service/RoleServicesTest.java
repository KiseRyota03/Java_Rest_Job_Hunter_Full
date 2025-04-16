package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RoleServicesTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    private Role testRole;
    private List<Permission> testPermissions;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test permissions
        testPermissions = new ArrayList<>();

        Permission permission1 = new Permission();
        permission1.setName("Test Permission 1");
        permission1.setApiPath("/api/v1/test1");
        permission1.setMethod("GET");
        permission1.setModule("TEST");
        permission1 = permissionRepository.save(permission1);
        testPermissions.add(permission1);

        Permission permission2 = new Permission();
        permission2.setName("Test Permission 2");
        permission2.setApiPath("/api/v1/test2");
        permission2.setMethod("POST");
        permission2.setModule("TEST");
        permission2 = permissionRepository.save(permission2);
        testPermissions.add(permission2);

        // Create test role
        testRole = new Role();
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Role for testing");
        testRole.setActive(true);
        testRole.setPermissions(testPermissions);
    }

    /**
     * Test existByName with new role name
     * Test ID: UT_ROLE_01
     * Objective: Verify that new role name doesn't exist
     * Input: New role name
     * Expected: false
     */
    @Test
    public void testExistByNameNew() {
        // When
        boolean exists = roleService.existByName("NEW_ROLE_NAME");

        // Then
        assertFalse(exists);
    }

    /**
     * Test existByName with existing role name
     * Test ID: UT_ROLE_02
     * Objective: Verify that existing role name is detected
     * Input: Existing role name
     * Expected: true
     */
    @Test
    public void testExistByNameExisting() {
        // Given
        roleRepository.save(testRole);

        // When
        boolean exists = roleService.existByName("TEST_ROLE");

        // Then
        assertTrue(exists);
    }

    /**
     * Test create with valid role
     * Test ID: UT_ROLE_03
     * Objective: Verify that a valid role can be created
     * Input: Valid Role object
     * Expected: Role saved with generated ID and permissions
     */
    @Test
    public void testCreate() {
        // When
        Role createdRole = roleService.create(testRole);

        // Then
        assertNotNull(createdRole);
        assertThat(createdRole.getId()).isGreaterThan(0);
        assertEquals("TEST_ROLE", createdRole.getName());
        assertEquals("Role for testing", createdRole.getDescription());
        assertTrue(createdRole.isActive());

        // Check permissions are associated
        assertNotNull(createdRole.getPermissions());
        assertEquals(2, createdRole.getPermissions().size());

        // Check database - verify role exists
        Role dbRole = roleRepository.findById(createdRole.getId()).orElse(null);
        assertNotNull(dbRole);
        assertEquals(2, dbRole.getPermissions().size());
    }

    /**
     * Test create with invalid permissions
     * Test ID: UT_ROLE_04
     * Objective: Verify handling of invalid permission IDs
     * Input: Role with non-existent permission IDs
     * Expected: Role created with only valid permissions
     */
    @Test
    public void testCreateWithInvalidPermissions() {
        // Given
        List<Permission> mixedPermissions = new ArrayList<>(testPermissions);

        // Add a permission with non-existent ID
        Permission invalidPermission = new Permission();
        invalidPermission.setId(99999L);
        invalidPermission.setName("Invalid Permission");
        invalidPermission.setApiPath("/invalid");
        invalidPermission.setMethod("GET");
        invalidPermission.setModule("INVALID");
        mixedPermissions.add(invalidPermission);

        testRole.setPermissions(mixedPermissions);

        // When
        Role createdRole = roleService.create(testRole);

        // Then
        assertNotNull(createdRole);
        assertNotNull(createdRole.getPermissions());
        assertEquals(2, createdRole.getPermissions().size()); // Only valid permissions should be associated
    }

    /**
     * Test fetchById with existing role
     * Test ID: UT_ROLE_05
     * Objective: Verify that an existing role can be fetched by ID
     * Input: Valid role ID
     * Expected: Correct role returned
     */
    @Test
    public void testFetchById() {
        // Given
        Role savedRole = roleRepository.save(testRole);
        long roleId = savedRole.getId();

        // When
        Role fetchedRole = roleService.fetchById(roleId);

        // Then
        assertNotNull(fetchedRole);
        assertEquals(roleId, fetchedRole.getId());
        assertEquals("TEST_ROLE", fetchedRole.getName());
        assertEquals("Role for testing", fetchedRole.getDescription());
        assertTrue(fetchedRole.isActive());
        assertNotNull(fetchedRole.getPermissions());
        assertEquals(2, fetchedRole.getPermissions().size());
    }

    /**
     * Test fetchById with non-existing role
     * Test ID: UT_ROLE_06
     * Objective: Verify handling of non-existent role ID
     * Input: Invalid role ID
     * Expected: null returned
     */
    @Test
    public void testFetchByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        Role fetchedRole = roleService.fetchById(nonExistingId);

        // Then
        assertNull(fetchedRole);
    }

    /**
     * Test update with valid role
     * Test ID: UT_ROLE_07
     * Objective: Verify that a role can be updated
     * Input: Valid Role object
     * Expected: Updated role returned
     */
    @Test
    public void testUpdate() {
        // Given
        Role savedRole = roleRepository.save(testRole);
        long roleId = savedRole.getId();

        // Create a new permission for the update
        Permission newPermission = new Permission();
        newPermission.setName("New Permission");
        newPermission.setApiPath("/api/v1/new");
        newPermission.setMethod("PUT");
        newPermission.setModule("NEW");
        newPermission = permissionRepository.save(newPermission);

        List<Permission> updatedPermissions = new ArrayList<>();
        updatedPermissions.add(newPermission);

        // Create update request
        Role updateRequest = new Role();
        updateRequest.setId(roleId);
        updateRequest.setName("UPDATED_ROLE");
        updateRequest.setDescription("Updated description");
        updateRequest.setActive(false);
        updateRequest.setPermissions(updatedPermissions);

        // When
        Role updatedRole = roleService.update(updateRequest);

        // Then
        assertNotNull(updatedRole);
        assertEquals(roleId, updatedRole.getId());
        assertEquals("UPDATED_ROLE", updatedRole.getName());
        assertEquals("Updated description", updatedRole.getDescription());
        assertFalse(updatedRole.isActive());

        // Check permissions are updated
        assertNotNull(updatedRole.getPermissions());
        assertEquals(1, updatedRole.getPermissions().size());
        assertEquals("New Permission", updatedRole.getPermissions().get(0).getName());

        // Check database - verify update persisted
        Role dbRole = roleRepository.findById(roleId).orElse(null);
        assertNotNull(dbRole);
        assertEquals("UPDATED_ROLE", dbRole.getName());
        assertEquals(1, dbRole.getPermissions().size());
    }

    /**
     * Test delete with existing role
     * Test ID: UT_ROLE_08
     * Objective: Verify that an existing role can be deleted
     * Input: Valid role ID
     * Expected: Role deleted
     */
    @Test
    public void testDelete() {
        // Given
        Role savedRole = roleRepository.save(testRole);
        long roleId = savedRole.getId();

        // When
        roleService.delete(roleId);

        // Then
        Role dbRole = roleRepository.findById(roleId).orElse(null);
        assertNull(dbRole);
    }

    /**
     * Test delete with role assigned to users
     * Test ID: UT_ROLE_09
     * Objective: Verify cascade delete behavior
     * Input: Role ID that is assigned to users
     * Expected: Role deleted (but potentially orphaned users)
     * Note: This behavior depends on the database constraints
     */
    @Test
    public void testDeleteWithUsers() {
        // Given
        Role savedRole = roleRepository.save(testRole);
        long roleId = savedRole.getId();

        // Create a user with this role
        User user = new User();
        user.setName("Test User");
        user.setEmail("test.user@example.com");
        user.setPassword("password123");
        user.setRole(savedRole);
        user = userRepository.save(user);

        long userId = user.getId();

        // Verify user has the role
        User dbUser = userRepository.findById(userId).orElse(null);
        assertNotNull(dbUser);
        assertNotNull(dbUser.getRole());
        assertEquals(roleId, dbUser.getRole().getId());

        // When
        roleService.delete(roleId);

        // Then
        // Check role is deleted
        Role dbRole = roleRepository.findById(roleId).orElse(null);
        assertNull(dbRole);

        // Note: The user might still exist but with null role depending on database
        // constraints
        // This test just verifies the role deletion completes without errors
    }

    /**
     * Test getRoles with pagination
     * Test ID: UT_ROLE_10
     * Objective: Verify that roles can be fetched with pagination
     * Input: Specification, Pageable
     * Expected: Correctly paginated roles
     */
    @Test
    public void testGetRoles() {
        // Given
        roleRepository.save(testRole);

        Role role2 = new Role();
        role2.setName("SECOND_ROLE");
        role2.setDescription("Second role for testing");
        role2.setActive(true);
        roleRepository.save(role2);

        Pageable pageable = PageRequest.of(0, 10);
        Specification<Role> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = roleService.getRoles(spec, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertNotNull(result.getResult());

        // Check pagination metadata
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
        assertTrue(result.getMeta().getTotal() >= 2);
    }
}
