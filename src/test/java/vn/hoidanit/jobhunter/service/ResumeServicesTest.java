package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Resume;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResFetchResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.ResumeRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ResumeServicesTest {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyService companyService;

    private Resume testResume;
    private User testUser;
    private Job testJob;
    private Company testCompany;

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

        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test.user@example.com");
        testUser.setPassword("password123");
        testUser = userService.handleCreateUser(testUser);

        // Create test job
        testJob = new Job();
        testJob.setName("Test Job");
        testJob.setLocation("Test Location");
        testJob.setSalary(50000.0);
        testJob.setQuantity(5);
        testJob.setLevel(LevelEnum.MIDDLE);
        testJob.setCompany(testCompany);
        testJob = jobRepository.save(testJob);

        // Create test resume
        testResume = new Resume();
        testResume.setEmail("test.applicant@example.com");
        testResume.setUrl("http://example.com/resume.pdf");
        testResume.setStatus(ResumeStateEnum.PENDING);
        testResume.setUser(testUser);
        testResume.setJob(testJob);
    }

    /**
     * Test checkResumeExistByUserAndJob with valid data
     * Test ID: UT_RESUME_01
     * Objective: Verify that valid user and job can be confirmed
     * Input: Resume with valid user and job
     * Expected: true
     */
    @Test
    public void testCheckResumeExistByUserAndJobValid() {
        // When
        boolean result = resumeService.checkResumeExistByUserAndJob(testResume);

        // Then
        assertTrue(result);
    }

    /**
     * Test checkResumeExistByUserAndJob with null user
     * Test ID: UT_RESUME_02
     * Objective: Verify handling of null user
     * Input: Resume with null user
     * Expected: false
     */
    @Test
    public void testCheckResumeExistByUserAndJobNullUser() {
        // Given
        testResume.setUser(null);

        // When
        boolean result = resumeService.checkResumeExistByUserAndJob(testResume);

        // Then
        assertFalse(result);
    }

    /**
     * Test checkResumeExistByUserAndJob with invalid user
     * Test ID: UT_RESUME_03
     * Objective: Verify handling of invalid user ID
     * Input: Resume with non-existent user ID
     * Expected: false
     */
    @Test
    public void testCheckResumeExistByUserAndJobInvalidUser() {
        // Given
        User invalidUser = new User();
        invalidUser.setId(99999L);
        testResume.setUser(invalidUser);

        // When
        boolean result = resumeService.checkResumeExistByUserAndJob(testResume);

        // Then
        assertFalse(result);
    }

    /**
     * Test checkResumeExistByUserAndJob with null job
     * Test ID: UT_RESUME_04
     * Objective: Verify handling of null job
     * Input: Resume with null job
     * Expected: false
     */
    @Test
    public void testCheckResumeExistByUserAndJobNullJob() {
        // Given
        testResume.setJob(null);

        // When
        boolean result = resumeService.checkResumeExistByUserAndJob(testResume);

        // Then
        assertFalse(result);
    }

    /**
     * Test checkResumeExistByUserAndJob with invalid job
     * Test ID: UT_RESUME_05
     * Objective: Verify handling of invalid job ID
     * Input: Resume with non-existent job ID
     * Expected: false
     */
    @Test
    public void testCheckResumeExistByUserAndJobInvalidJob() {
        // Given
        Job invalidJob = new Job();
        invalidJob.setId(99999L);
        testResume.setJob(invalidJob);

        // When
        boolean result = resumeService.checkResumeExistByUserAndJob(testResume);

        // Then
        assertFalse(result);
    }

    /**
     * Test create with valid resume
     * Test ID: UT_RESUME_06
     * Objective: Verify that a valid resume can be created
     * Input: Valid Resume object
     * Expected: Resume saved with generated ID and correct DTO returned
     */
    @Test
    public void testCreate() {
        // When
        ResCreateResumeDTO result = resumeService.create(testResume);

        // Then
        assertNotNull(result);
        assertThat(result.getId()).isGreaterThan(0);
        assertNotNull(result.getCreatedAt());

        // Check database - verify resume exists
        Optional<Resume> dbResume = resumeRepository.findById(result.getId());
        assertTrue(dbResume.isPresent());
        assertEquals("test.applicant@example.com", dbResume.get().getEmail());
        assertEquals("http://example.com/resume.pdf", dbResume.get().getUrl());
        assertEquals(ResumeStateEnum.PENDING, dbResume.get().getStatus());
        assertEquals(testUser.getId(), dbResume.get().getUser().getId());
        assertEquals(testJob.getId(), dbResume.get().getJob().getId());
    }

    /**
     * Test update with valid resume
     * Test ID: UT_RESUME_07
     * Objective: Verify that a resume can be updated
     * Input: Valid Resume object
     * Expected: Updated resume and correct DTO returned
     */
    @Test
    public void testUpdate() {
        // Given - create initial resume
        testResume = resumeRepository.save(testResume);

        // Update resume status
        testResume.setStatus(ResumeStateEnum.REVIEWING);

        // When
        ResUpdateResumeDTO result = resumeService.update(testResume);

        // Then
        assertNotNull(result);
        assertNotNull(result.getUpdatedAt());

        // Check database - verify resume is updated
        Optional<Resume> dbResume = resumeRepository.findById(testResume.getId());
        assertTrue(dbResume.isPresent());
        assertEquals(ResumeStateEnum.REVIEWING, dbResume.get().getStatus());
    }

    /**
     * Test fetchById with existing resume
     * Test ID: UT_RESUME_08
     * Objective: Verify that an existing resume can be fetched by ID
     * Input: Valid resume ID
     * Expected: Correct Optional<Resume> returned
     */
    @Test
    public void testFetchById() {
        // Given
        testResume = resumeRepository.save(testResume);
        long resumeId = testResume.getId();

        // When
        Optional<Resume> result = resumeService.fetchById(resumeId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(resumeId, result.get().getId());
        assertEquals("test.applicant@example.com", result.get().getEmail());
        assertEquals("http://example.com/resume.pdf", result.get().getUrl());
    }

    /**
     * Test fetchById with non-existing resume
     * Test ID: UT_RESUME_09
     * Objective: Verify handling of non-existent resume ID
     * Input: Invalid resume ID
     * Expected: Empty Optional returned
     */
    @Test
    public void testFetchByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        Optional<Resume> result = resumeService.fetchById(nonExistingId);

        // Then
        assertFalse(result.isPresent());
    }

    /**
     * Test delete with existing resume
     * Test ID: UT_RESUME_10
     * Objective: Verify that an existing resume can be deleted
     * Input: Valid resume ID
     * Expected: Resume deleted
     */
    @Test
    public void testDelete() {
        // Given
        testResume = resumeRepository.save(testResume);
        long resumeId = testResume.getId();

        // Verify resume exists
        assertTrue(resumeRepository.findById(resumeId).isPresent());

        // When
        resumeService.delete(resumeId);

        // Then
        Optional<Resume> dbResume = resumeRepository.findById(resumeId);
        assertFalse(dbResume.isPresent());
    }

    /**
     * Test getResume with valid resume
     * Test ID: UT_RESUME_11
     * Objective: Verify conversion from Resume to ResFetchResumeDTO
     * Input: Valid Resume object
     * Expected: Correctly populated DTO
     */
    @Test
    public void testGetResume() {
        // Given
        testResume = resumeRepository.save(testResume);

        // When
        ResFetchResumeDTO result = resumeService.getResume(testResume);

        // Then
        assertNotNull(result);
        assertEquals(testResume.getId(), result.getId());
        assertEquals("test.applicant@example.com", result.getEmail());
        assertEquals("http://example.com/resume.pdf", result.getUrl());
        assertEquals(ResumeStateEnum.PENDING, result.getStatus());
        assertEquals("Test User", result.getUser().getName());
        assertEquals("Test Job", result.getJob().getName());
        assertEquals("Test Company", result.getCompanyName());
    }

    /**
     * Test fetchAllResume with pagination
     * Test ID: UT_RESUME_12
     * Objective: Verify that resumes can be fetched with pagination
     * Input: Specification, Pageable
     * Expected: Correctly paginated resumes
     */
    @Test
    public void testFetchAllResume() {
        // Given
        testResume = resumeRepository.save(testResume);

        Resume resume2 = new Resume();
        resume2.setEmail("second.applicant@example.com");
        resume2.setUrl("http://example.com/resume2.pdf");
        resume2.setStatus(ResumeStateEnum.APPROVED);
        resume2.setUser(testUser);
        resume2.setJob(testJob);
        resumeRepository.save(resume2);

        Pageable pageable = PageRequest.of(0, 10);
        Specification<Resume> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = resumeService.fetchAllResume(spec, pageable);

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
     * Test fetchResumeByUser - requires authenticated user
     * Test ID: UT_RESUME_13
     * Objective: Verify that resumes can be fetched for the current user
     * Input: Pageable
     * Expected: Correctly filtered resumes for current user
     */
    @Test
    @WithMockUser(username = "test.user@example.com")
    public void testFetchResumeByUser() {
        // Given
        testResume = resumeRepository.save(testResume);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        ResultPaginationDTO result = resumeService.fetchResumeByUser(pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertNotNull(result.getResult());

        // The test uses a mock user, so actual results may vary
        // We're mostly testing that the method executes without error
    }
}
