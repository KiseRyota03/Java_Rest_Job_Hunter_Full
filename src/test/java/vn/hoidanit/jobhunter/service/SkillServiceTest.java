package vn.hoidanit.jobhunter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.Subscriber;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.SkillRepository;
import vn.hoidanit.jobhunter.repository.SubscriberRepository;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SkillServiceTest {

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private Skill testSkill;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    public void setup() {
        // Create test skill
        testSkill = new Skill();
        testSkill.setName("Java Programming");
    }

    /**
     * Test isNameExist with new skill name
     * Test ID: UT_SKILL_01
     * Objective: Verify that new skill name doesn't exist
     * Input: New skill name
     * Expected: false
     */
    @Test
    public void testIsNameExistNew() {
        // When
        boolean exists = skillService.isNameExist("New Skill Name");

        // Then
        assertFalse(exists);
    }

    /**
     * Test isNameExist with existing skill name
     * Test ID: UT_SKILL_02
     * Objective: Verify that existing skill name is detected
     * Input: Existing skill name
     * Expected: true
     */
    @Test
    public void testIsNameExistExisting() {
        // Given
        skillRepository.save(testSkill);

        // When
        boolean exists = skillService.isNameExist("Java Programming");

        // Then
        assertTrue(exists);
    }

    /**
     * Test fetchSkillById with existing skill
     * Test ID: UT_SKILL_03
     * Objective: Verify that an existing skill can be fetched by ID
     * Input: Valid skill ID
     * Expected: Correct skill returned
     */
    @Test
    public void testFetchSkillById() {
        // Given
        Skill savedSkill = skillRepository.save(testSkill);
        long skillId = savedSkill.getId();

        // When
        Skill fetchedSkill = skillService.fetchSkillById(skillId);

        // Then
        assertNotNull(fetchedSkill);
        assertEquals(skillId, fetchedSkill.getId());
        assertEquals("Java Programming", fetchedSkill.getName());
    }

    /**
     * Test fetchSkillById with non-existing skill
     * Test ID: UT_SKILL_04
     * Objective: Verify handling of non-existent skill ID
     * Input: Invalid skill ID
     * Expected: null returned
     */
    @Test
    public void testFetchSkillByIdNonExisting() {
        // Given
        long nonExistingId = 99999L;

        // When
        Skill fetchedSkill = skillService.fetchSkillById(nonExistingId);

        // Then
        assertNull(fetchedSkill);
    }

    /**
     * Test createSkill with valid skill
     * Test ID: UT_SKILL_05
     * Objective: Verify that a valid skill can be created
     * Input: Valid Skill object
     * Expected: Skill saved with generated ID
     */
    @Test
    public void testCreateSkill() {
        // When
        Skill createdSkill = skillService.createSkill(testSkill);

        // Then
        assertNotNull(createdSkill);
        assertThat(createdSkill.getId()).isGreaterThan(0);
        assertEquals("Java Programming", createdSkill.getName());

        // Check database - verify skill exists
        Optional<Skill> dbSkill = skillRepository.findById(createdSkill.getId());
        assertTrue(dbSkill.isPresent());
    }

    /**
     * Test updateSkill with valid skill
     * Test ID: UT_SKILL_06
     * Objective: Verify that a skill can be updated
     * Input: Valid Skill object
     * Expected: Updated skill returned
     */
    @Test
    public void testUpdateSkill() {
        // Given
        Skill savedSkill = skillRepository.save(testSkill);
        long skillId = savedSkill.getId();

        // Create update request
        Skill updateRequest = new Skill();
        updateRequest.setId(skillId);
        updateRequest.setName("Updated Skill Name");

        // When
        Skill updatedSkill = skillService.updateSkill(updateRequest);

        // Then
        assertNotNull(updatedSkill);
        assertEquals(skillId, updatedSkill.getId());
        assertEquals("Updated Skill Name", updatedSkill.getName());

        // Check database - verify update persisted
        Optional<Skill> dbSkill = skillRepository.findById(skillId);
        assertTrue(dbSkill.isPresent());
        assertEquals("Updated Skill Name", dbSkill.get().getName());
    }

    /**
     * Test deleteSkill with existing skill
     * Test ID: UT_SKILL_07
     * Objective: Verify that an existing skill can be deleted
     * Input: Valid skill ID
     * Expected: Skill deleted
     */
    @Test
    public void testDeleteSkill() {
        // Given
        Skill savedSkill = skillRepository.save(testSkill);
        long skillId = savedSkill.getId();

        // When
        skillService.deleteSkill(skillId);

        // Then
        Optional<Skill> dbSkill = skillRepository.findById(skillId);
        assertFalse(dbSkill.isPresent());
    }

    /**
     * Test deleteSkill with skill associated to jobs
     * Test ID: UT_SKILL_08
     * Objective: Verify that a skill can be deleted and removed from jobs
     * Input: Skill ID with job associations
     * Expected: Skill deleted and removed from jobs
     */
    @Test
    public void testDeleteSkillWithJobAssociations() {
        // Given
        Skill savedSkill = skillRepository.save(testSkill);
        long skillId = savedSkill.getId();

        // Create a job and associate the skill
        Job job = new Job();
        job.setName("Developer Position");
        job.setLocation("Remote");
        job.setSalary(100000.0);
        job.setQuantity(1);
        job.setLevel(LevelEnum.SENIOR);

        List<Skill> jobSkills = new ArrayList<>();
        jobSkills.add(savedSkill);
        job.setSkills(jobSkills);

        job = jobRepository.save(job);
        long jobId = job.getId();

        // Verify job has the skill
        Job savedJob = jobRepository.findById(jobId).orElse(null);
        assertNotNull(savedJob);
        assertEquals(1, savedJob.getSkills().size());
        assertEquals(skillId, savedJob.getSkills().get(0).getId());

        // When
        skillService.deleteSkill(skillId);

        // Then
        // Check skill is deleted
        Optional<Skill> dbSkill = skillRepository.findById(skillId);
        assertFalse(dbSkill.isPresent());

        // Check job doesn't have the skill anymore
        Job updatedJob = jobRepository.findById(jobId).orElse(null);
        assertNotNull(updatedJob);
        assertTrue(updatedJob.getSkills() == null || updatedJob.getSkills().isEmpty());
    }

    /**
     * Test deleteSkill with skill associated to subscribers
     * Test ID: UT_SKILL_09
     * Objective: Verify that a skill can be deleted and removed from subscribers
     * Input: Skill ID with subscriber associations
     * Expected: Skill deleted and removed from subscribers
     */

    @Test
    public void testFetchAllSkills() {
        // Given
        skillRepository.save(testSkill);

        Skill skill2 = new Skill();
        skill2.setName("Spring Boot");
        skillRepository.save(skill2);

        Pageable pageable = PageRequest.of(0, 10);
        Specification<Skill> spec = Specification.where(null);

        // When
        ResultPaginationDTO result = skillService.fetchAllSkills(spec, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertNotNull(result.getResult());

        // Check pagination metadata
        assertEquals(1, result.getMeta().getPage());
        assertEquals(10, result.getMeta().getPageSize());
        assertTrue(result.getMeta().getTotal() >= 2);

        // Check returned skills
        List<?> skills = (List<?>) result.getResult();
        assertTrue(skills.size() >= 2);
    }
}
