package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CompanyServicesTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private Company testCompany;
    private User testUser;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test company
        testCompany = new Company();
        testCompany.setName("Test Company");
        testCompany.setAddress("Test Address");
        testCompany.setDescription("Test Description");

        // Create test user for user association tests
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test.user@example.com");
        testUser.setPassword("password123");
    }

    /**
     * Test handleCreateCompany with valid company
     * Test ID: UT_COMPANY_01
     * Objective: Verify that a valid company can be created successfully
     * Input: Valid Company object
     * Expected: Company saved with generated ID
     */
    @Test
    public void testHandleCreateCompany() {
        // When
        Company createdCompany = companyService.handleCreateCompany(testCompany);

        // Then
        assertNotNull(createdCompany);
        assertThat(createdCompany.getId()).isGreaterThan(0);
        assertEquals("Test Company", createdCompany.getName());
        assertEquals("Test Address", createdCompany.getAddress());
        assertEquals("Test Description", createdCompany.getDescription());

        // Check database - verify company exists
        Optional<Company> dbCompany = companyRepository.findById(createdCompany.getId());
        assertTrue(dbCompany.isPresent());
    }

    /**
     * Test handleGetCompany with pagination
     * Test ID: UT_COMPANY_02
     * Objective: Verify that companies can be fetched with pagination
     * Input: Specification, Pageable
     * Expected: Correctly paginated companies
     */
    @Test
    public void testHandleGetCompany() {
        // Given
        companyService.handleCreateCompany(testCompany);

        Company company2 = new Company();
        company2.setName("Second Company");
        company2.setAddress("Second Address");
        companyService.handleCreateCompany(company2);

        Pageable pageable = PageRequest.of(0, 10);
        Specification<Company> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = companyService.handleGetCompany(spec, pageable);

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
     * Test handleUpdateCompany with valid company
     * Test ID: UT_COMPANY_03
     * Objective: Verify that a company can be updated
     * Input: Valid Company object
     * Expected: Updated company returned
     */
    @Test
    public void testHandleUpdateCompany() {
        // Given
        Company createdCompany = companyService.handleCreateCompany(testCompany);
        long companyId = createdCompany.getId();

        // Create update request
        Company updateRequest = new Company();
        updateRequest.setId(companyId);
        updateRequest.setName("Updated Company");
        updateRequest.setAddress("Updated Address");
        updateRequest.setDescription("Updated Description");
        updateRequest.setLogo("updated-logo.jpg");

        // When
        Company updatedCompany = companyService.handleUpdateCompany(updateRequest);

        // Then
        assertNotNull(updatedCompany);
        assertEquals(companyId, updatedCompany.getId());
        assertEquals("Updated Company", updatedCompany.getName());
        assertEquals("Updated Address", updatedCompany.getAddress());
        assertEquals("Updated Description", updatedCompany.getDescription());
        assertEquals("updated-logo.jpg", updatedCompany.getLogo());

        // Check database - verify update persisted
        Optional<Company> dbCompany = companyRepository.findById(companyId);
        assertTrue(dbCompany.isPresent());
        assertEquals("Updated Company", dbCompany.get().getName());
    }

    /**
     * Test handleUpdateCompany with non-existing company
     * Test ID: UT_COMPANY_04
     * Objective: Verify handling of update for non-existent company
     * Input: Invalid company ID
     * Expected: null returned
     */
    @Test
    public void testHandleUpdateCompanyNonExisting() {
        // Given
        Company updateRequest = new Company();
        updateRequest.setId(99999L);
        updateRequest.setName("Updated Company");

        // When
        Company updatedCompany = companyService.handleUpdateCompany(updateRequest);

        // Then
        assertNull(updatedCompany);
    }

    /**
     * Test handleDeleteCompany with existing company
     * Test ID: UT_COMPANY_05
     * Objective: Verify that an existing company can be deleted
     * Input: Valid company ID
     * Expected: Company deleted
     */
    @Test
    public void testHandleDeleteCompany() {
        // Given
        Company createdCompany = companyService.handleCreateCompany(testCompany);
        long companyId = createdCompany.getId();

        // When
        companyService.handleDeleteCompany(companyId);

        // Then
        Optional<Company> dbCompany = companyRepository.findById(companyId);
        assertFalse(dbCompany.isPresent());
    }

    /**
     * Test handleDeleteCompany deletes associated users
     * Test ID: UT_COMPANY_06
     * Objective: Verify that deleting a company also deletes its associated users
     * Input: Company with associated users
     * Expected: Company and users deleted
     */
    @Test
    public void testHandleDeleteCompanyWithAssociatedUsers() {
        // Given
        Company createdCompany = companyService.handleCreateCompany(testCompany);
        testUser.setCompany(createdCompany);
        User createdUser = userService.handleCreateUser(testUser);

        long companyId = createdCompany.getId();
        long userId = createdUser.getId();

        // Verify user exists and is associated with company
        assertNotNull(userRepository.findById(userId).orElse(null));
        assertEquals(companyId, userRepository.findById(userId).get().getCompany().getId());

        // When
        companyService.handleDeleteCompany(companyId);

        // Then
        // Check company is deleted
        Optional<Company> dbCompany = companyRepository.findById(companyId);
        assertFalse(dbCompany.isPresent());

        // Check associated user is deleted
        Optional<User> dbUser = userRepository.findById(userId);
        assertFalse(dbUser.isPresent());
    }

    /**
     * Test findById with existing company
     * Test ID: UT_COMPANY_07
     * Objective: Verify that an existing company can be fetched by ID
     * Input: Valid company ID
     * Expected: Correct company returned
     */
    @Test
    public void testFindById() {
        // Given
        Company createdCompany = companyService.handleCreateCompany(testCompany);
        long companyId = createdCompany.getId();

        // When
        Optional<Company> result = companyService.findById(companyId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(companyId, result.get().getId());
        assertEquals(testCompany.getName(), result.get().getName());
        assertEquals(testCompany.getAddress(), result.get().getAddress());
    }

    /**
     * Test findById with non-existing company
     * Test ID: UT_COMPANY_08
     * Objective: Verify handling of non-existent company ID
     * Input: Invalid company ID
     * Expected: Empty Optional returned
     */
    @Test
    public void testFindByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        Optional<Company> result = companyService.findById(nonExistingId);

        // Then
        assertFalse(result.isPresent());
    }
}
