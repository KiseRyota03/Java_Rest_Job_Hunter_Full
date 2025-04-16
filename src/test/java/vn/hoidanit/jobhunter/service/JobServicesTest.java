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
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJobDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobServicesTest {

  @Autowired
  private JobService jobService;

  @Autowired
  private JobRepository jobRepository;

  @Autowired
  private CompanyService companyService;

  @Autowired
  private SkillService skillService;

  private Job testJob;
  private Company testCompany;
  private Skill testSkill;

  @BeforeEach
  public void setup() {
    // Create test company
    testCompany = new Company();
    testCompany.setName("Test Company");
    testCompany.setAddress("Test Address");
    testCompany = companyService.handleCreateCompany(testCompany);

    // Create single test skill using SkillService
    testSkill = new Skill();
    testSkill.setName("Java");
    testSkill = skillService.createSkill(testSkill);

    // Create test job
    testJob = new Job();
    testJob.setName("Test Job");
    testJob.setLocation("Test Location");
    testJob.setSalary(50000.0);
    testJob.setQuantity(5);
    testJob.setLevel(LevelEnum.MIDDLE);
    testJob.setDescription("Test Description");
    testJob.setActive(true);
    testJob.setCompany(testCompany);
    testJob.setSkills(List.of(testSkill)); // Single skill
  }

  @Test
  public void testCreate() {
    ResCreateJobDTO result = jobService.create(testJob);
    assertNotNull(result);
    assertThat(result.getId()).isGreaterThan(0);
    assertEquals("Test Job", result.getName());
    assertEquals("Test Location", result.getLocation());
    assertEquals(50000.0, result.getSalary());
    assertEquals(5, result.getQuantity());
    assertEquals(LevelEnum.MIDDLE, result.getLevel());
    assertNotNull(result.getSkills());
    assertEquals(1, result.getSkills().size());
    assertTrue(result.getSkills().contains("Java"));
    assertTrue(jobRepository.findById(result.getId()).isPresent());
  }

  @Test
  public void testCreateWithoutCompany() {
    testJob.setCompany(null);
    ResCreateJobDTO result = jobService.create(testJob);
    assertNotNull(result);
    Optional<Job> dbJob = jobRepository.findById(result.getId());
    assertTrue(dbJob.isPresent());
    assertNull(dbJob.get().getCompany());
  }

  @Test
  public void testCreateWithoutSkills() {
    testJob.setSkills(null);
    ResCreateJobDTO result = jobService.create(testJob);
    assertNotNull(result);
    assertTrue(result.getSkills() == null || result.getSkills().isEmpty());
  }

  @Test
  public void testUpdate() {
    ResCreateJobDTO createResult = jobService.create(testJob);
    Job existingJob = jobRepository.findById(createResult.getId()).get();

    Job updateJob = new Job();
    updateJob.setId(existingJob.getId());
    updateJob.setName("Updated Job");
    updateJob.setLocation("Updated Location");
    updateJob.setSalary(60000.0);
    updateJob.setQuantity(10);
    updateJob.setLevel(LevelEnum.SENIOR);
    updateJob.setActive(false);

    Skill newSkill = new Skill();
    newSkill.setName("React");
    newSkill = skillService.createSkill(newSkill); // <-- use service
    updateJob.setSkills(List.of(newSkill));

    ResUpdateJobDTO result = jobService.update(updateJob, existingJob);
    assertNotNull(result);
    assertEquals(existingJob.getId(), result.getId());
    assertEquals("Updated Job", result.getName());
    assertEquals("Updated Location", result.getLocation());
    assertEquals(60000.0, result.getSalary());
    assertEquals(10, result.getQuantity());
    assertEquals(LevelEnum.SENIOR, result.getLevel());
    assertFalse(result.isActive());
    assertNotNull(result.getSkills());
    assertEquals(1, result.getSkills().size());
    assertTrue(result.getSkills().contains("React"));
    assertEquals("Updated Job", jobRepository.findById(result.getId()).get().getName());
  }

  @Test
  public void testFetchJobById() {
    ResCreateJobDTO createResult = jobService.create(testJob);
    long jobId = createResult.getId();
    Optional<Job> result = jobService.fetchJobById(jobId);
    assertTrue(result.isPresent());
    assertEquals(jobId, result.get().getId());
    assertEquals("Test Job", result.get().getName());
  }

  @Test
  public void testFetchJobByIdNonExisting() {
    Optional<Job> result = jobService.fetchJobById(99999L);
    assertFalse(result.isPresent());
  }

  @Test
  public void testDelete() {
    ResCreateJobDTO createResult = jobService.create(testJob);
    long jobId = createResult.getId();
    assertTrue(jobRepository.findById(jobId).isPresent());
    jobService.delete(jobId);
    assertFalse(jobRepository.findById(jobId).isPresent());
  }

  @Test
  public void testFetchAll() {
    jobService.create(testJob);

    Job job2 = new Job();
    job2.setName("Second Job");
    job2.setLocation("Second Location");
    job2.setSalary(40000.0);
    job2.setQuantity(3);
    job2.setLevel(LevelEnum.JUNIOR);
    job2.setCompany(testCompany);
    job2.setSkills(List.of(testSkill)); // reusing same skill
    jobService.create(job2);

    Pageable pageable = PageRequest.of(0, 10);
    Specification<Job> spec = Specification.where(null);

    ResultPaginationDTO result = jobService.fetchAll(spec, pageable);
    assertNotNull(result);
    assertNotNull(result.getMeta());
    assertNotNull(result.getResult());
    assertEquals(1, result.getMeta().getPage());
    assertEquals(10, result.getMeta().getPageSize());
    assertTrue(result.getMeta().getTotal() >= 2);
  }
}
