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
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PermissionServiceTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    private Permission testPermission;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test permission
        testPermission = new Permission();
        testPermission.setName("Test Permission");
        testPermission.setApiPath("/api/v1/test");
        testPermission.setMethod("GET");
        testPermission.setModule("TEST");
    }

    /**
     * Test isPermissionExist with new permission
     * Test ID: UT_PERMISSION_01
     * Objective: Verify that new permission doesn't exist
     * Input: New Permission object
     * Expected: false
     */
    @Test
    public void testIsPermissionExistNew() {
        // When
        boolean exists = permissionService.isPermissionExist(testPermission);

        // Then
        assertFalse(exists);
    }

    /**
     * Test isPermissionExist with existing permission
     * Test ID: UT_PERMISSION_02
     * Objective: Verify that existing permission is detected
     * Input: Existing Permission object
     * Expected: true
     */
    @Test
    public void testIsPermissionExistExisting() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        
        // Create a new permission with same path/method/module
        Permission duplicatePermission = new Permission();
        duplicatePermission.setName("Different Name");
        duplicatePermission.setApiPath(savedPermission.getApiPath());
        duplicatePermission.setMethod(savedPermission.getMethod());
        duplicatePermission.setModule(savedPermission.getModule());

        // When
        boolean exists = permissionService.isPermissionExist(duplicatePermission);

        // Then
        assertTrue(exists);
    }

    /**
     * Test fetchById with existing permission
     * Test ID: UT_PERMISSION_03
     * Objective: Verify that an existing permission can be fetched by ID
     * Input: Valid permission ID
     * Expected: Correct permission returned
     */
    @Test
    public void testFetchById() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        long permissionId = savedPermission.getId();

        // When
        Permission fetchedPermission = permissionService.fetchById(permissionId);

        // Then
        assertNotNull(fetchedPermission);
        assertEquals(permissionId, fetchedPermission.getId());
        assertEquals(testPermission.getName(), fetchedPermission.getName());
        assertEquals(testPermission.getApiPath(), fetchedPermission.getApiPath());
        assertEquals(testPermission.getMethod(), fetchedPermission.getMethod());
        assertEquals(testPermission.getModule(), fetchedPermission.getModule());
    }

    /**
     * Test fetchById with non-existing permission
     * Test ID: UT_PERMISSION_04
     * Objective: Verify handling of non-existent permission ID
     * Input: Invalid permission ID
     * Expected: null returned
     */
    @Test
    public void testFetchByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        Permission fetchedPermission = permissionService.fetchById(nonExistingId);

        // Then
        assertNull(fetchedPermission);
    }

    /**
     * Test create with valid permission
     * Test ID: UT_PERMISSION_05
     * Objective: Verify that a valid permission can be created
     * Input: Valid Permission object
     * Expected: Permission saved with generated ID
     */
    @Test
    public void testCreate() {
        // When
        Permission createdPermission = permissionService.create(testPermission);

        // Then
        assertNotNull(createdPermission);
        assertThat(createdPermission.getId()).isGreaterThan(0);
        assertEquals(testPermission.getName(), createdPermission.getName());
        assertEquals(testPermission.getApiPath(), createdPermission.getApiPath());
        assertEquals(testPermission.getMethod(), createdPermission.getMethod());
        assertEquals(testPermission.getModule(), createdPermission.getModule());
        
        // Check database - verify permission exists
        Optional<Permission> dbPermission = permissionRepository.findById(createdPermission.getId());
        assertTrue(dbPermission.isPresent());
    }

    /**
     * Test update with valid permission
     * Test ID: UT_PERMISSION_06
     * Objective: Verify that a permission can be updated
     * Input: Valid Permission object
     * Expected: Updated permission returned
     */
    @Test
    public void testUpdate() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        long permissionId = savedPermission.getId();
        
        // Create update request
        Permission updateRequest = new Permission();
        updateRequest.setId(permissionId);
        updateRequest.setName("Updated Permission");
        updateRequest.setApiPath("/api/v1/updated");
        updateRequest.setMethod("POST");
        updateRequest.setModule("UPDATED");

        // When
        Permission updatedPermission = permissionService.update(updateRequest);

        // Then
        assertNotNull(updatedPermission);
        assertEquals(permissionId, updatedPermission.getId());
        assertEquals("Updated Permission", updatedPermission.getName());
        assertEquals("/api/v1/updated", updatedPermission.getApiPath());
        assertEquals("POST", updatedPermission.getMethod());
        assertEquals("UPDATED", updatedPermission.getModule());
        
        // Check database - verify update persisted
        Optional<Permission> dbPermission = permissionRepository.findById(permissionId);
        assertTrue(dbPermission.isPresent());
        assertEquals("Updated Permission", dbPermission.get().getName());
    }

    /**
     * Test update with non-existing permission
     * Test ID: UT_PERMISSION_07
     * Objective: Verify handling of update for non-existent permission
     * Input: Invalid permission ID
     * Expected: null returned
     */
    @Test
    public void testUpdateNonExisting() {
        // Given
        Permission updateRequest = new Permission();
        updateRequest.setId(99999L);
        updateRequest.setName("Updated Permission");
        updateRequest.setApiPath("/api/v1/updated");
        updateRequest.setMethod("POST");
        updateRequest.setModule("UPDATED");

        // When
        Permission updatedPermission = permissionService.update(updateRequest);

        // Then
        assertNull(updatedPermission);
    }

    /**
     * Test delete with existing permission
     * Test ID: UT_PERMISSION_08
     * Objective: Verify that an existing permission can be deleted
     * Input: Valid permission ID
     * Expected: Permission deleted
     */
    @Test
    public void testDelete() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        long permissionId = savedPermission.getId();

        // When
        permissionService.delete(permissionId);

        // Then
        Optional<Permission> dbPermission = permissionRepository.findById(permissionId);
        assertFalse(dbPermission.isPresent());
    }

    /**
     * Test delete with permission assigned to role
     * Test ID: UT_PERMISSION_09
     * Objective: Verify that a permission can be deleted even when assigned to a role
     * Input: Permission ID that is assigned to a role
     * Expected: Permission deleted and removed from role
     */
    @Test
    public void testDeleteWithRoleAssociation() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        long permissionId = savedPermission.getId();
        
        // Create a role and assign the permission
        Role role = new Role();
        role.setName("TEST_ROLE");
        role.setDescription("Role for testing");
        role.setActive(true);
        
        List<Permission> permissions = new ArrayList<>();
        permissions.add(savedPermission);
        role.setPermissions(permissions);
        
        role = roleRepository.save(role);
        
        // Verify role has the permission
        Role savedRole = roleRepository.findById(role.getId()).orElse(null);
        assertNotNull(savedRole);
        assertEquals(1, savedRole.getPermissions().size());
        assertEquals(permissionId, savedRole.getPermissions().get(0).getId());

        // When
        permissionService.delete(permissionId);

        // Then
        // Check permission is deleted
        Optional<Permission> dbPermission = permissionRepository.findById(permissionId);
        assertFalse(dbPermission.isPresent());
        
        // Check role doesn't have the permission anymore
        Role updatedRole = roleRepository.findById(role.getId()).orElse(null);
        assertNotNull(updatedRole);
        assertTrue(updatedRole.getPermissions() == null || updatedRole.getPermissions().isEmpty());
    }

    /**
     * Test getPermissions with pagination
     * Test ID: UT_PERMISSION_10
     * Objective: Verify that permissions can be fetched with pagination
     * Input: Specification, Pageable
     * Expected: Correctly paginated permissions
     */
    @Test
    public void testGetPermissions() {
        // Given
        permissionRepository.save(testPermission);
        
        Permission permission2 = new Permission();
        permission2.setName("Second Permission");
        permission2.setApiPath("/api/v1/second");
        permission2.setMethod("POST");
        permission2.setModule("SECOND");
        permissionRepository.save(permission2);
        
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Permission> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = permissionService.getPermissions(spec, pageable);

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
     * Test isSameName with same name
     * Test ID: UT_PERMISSION_11
     * Objective: Verify detection of duplicate permission name
     * Input: Permission with same name as existing one
     * Expected: true
     */
    @Test
    public void testIsSameNameTrue() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        
        // Create permission with same ID but different properties
        Permission checkPermission = new Permission();
        checkPermission.setId(savedPermission.getId());
        checkPermission.setName(savedPermission.getName()); // Same name
        checkPermission.setApiPath("/different/path");
        checkPermission.setMethod("PUT");
        checkPermission.setModule("DIFFERENT");

        // When
        boolean result = permissionService.isSameName(checkPermission);

        // Then
        assertTrue(result);
    }

    /**
     * Test isSameName with different name
     * Test ID: UT_PERMISSION_12
     * Objective: Verify handling of different permission name
     * Input: Permission with different name
     * Expected: false
     */
    @Test
    public void testIsSameNameFalse() {
        // Given
        Permission savedPermission = permissionRepository.save(testPermission);
        
        // Create permission with same ID but different name
        Permission checkPermission = new Permission();
        checkPermission.setId(savedPermission.getId());
        checkPermission.setName("Different Name");
        checkPermission.setApiPath(savedPermission.getApiPath());
        checkPermission.setMethod(savedPermission.getMethod());
        checkPermission.setModule(savedPermission.getModule());

        // When
        boolean result = permissionService.isSameName(checkPermission);

        // Then
        assertFalse(result);
    }
}
