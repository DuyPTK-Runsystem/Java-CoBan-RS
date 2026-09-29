package com.JavaTraining.BaiTap_RS.scorebook.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scorebook-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ScorebookAuthorizationIntegrationTest {

    private static final String MY_SCOREBOOK_CONTEXT_ENDPOINT =
            "/api/v2/assignments/me/scorebook-context";
    private static final String SCOREBOOK_ENDPOINT = "/api/v2/scorebooks/1";
    private static final String CLASS_SUBJECT_LOOKUP_ENDPOINT =
            "/api/v2/scorebooks/by-class-subject/20";
    private static final AtomicLong TEST_ID = new AtomicLong(500L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScorebookRepository scorebookRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private SubjectTeachingAssignmentRepository assignmentRepository;

    @BeforeEach
    void clearScorebooks() {
        scorebookRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessTeacherScorebookContextLookup() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(MY_SCOREBOOK_CONTEXT_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status);
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void officeMustContinueUsingOfficeLookupsRatherThanTeacherSelfContext() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(MY_SCOREBOOK_CONTEXT_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherWithoutMappedPrincipalCannotAccessTeacherScorebookContextLookup() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(MY_SCOREBOOK_CONTEXT_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status);
    }

    @Test
    void anonymousCannotAccessTeacherScorebookContextLookup() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(MY_SCOREBOOK_CONTEXT_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status);
    }

    @Test
    void assignedTeacherCanCreateScorebook() throws Exception {
        long userId = TEST_ID.incrementAndGet();
        Teacher teacher = teacherRepository.save(new Teacher(
                userId, "TEST-" + userId, "Test Teacher", null, null, null, null, null, null,
                TeacherStatus.ACTIVE));
        Subject subject = subjectRepository.save(new Subject(
                "TST-" + userId, "Test Subject", SubjectType.ACADEMIC, ApplicationScope.GRADE, SubjectStatus.ACTIVE));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Semester semester = semesterRepository.save(new Semester(
                userId, "SEM-" + userId, "Test Semester", 1, today.minusDays(30), today.plusDays(30), null,
                SemesterStatus.ACTIVE));
        ClassSubject classSubject = classSubjectRepository.save(new ClassSubject(
                userId, subject.getId(), semester.getId(), ClassSubjectStatus.ACTIVE));
        assignmentRepository.save(new SubjectTeachingAssignment(
                classSubject.getId(), teacher.getId(), today.minusDays(1), null, AssignmentStatus.ACTIVE, null));

        User user = new User("teacher" + userId, "password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role("TEACHER", "Teacher", "Teacher role"));
        UserPrincipal principal = new UserPrincipal(user);
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/scorebooks")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classSubjectId\":" + classSubject.getId() + "}"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(201, status);
        Assertions.assertEquals(
                ScorebookStatus.DRAFT,
                scorebookRepository.findByClassSubjectId(classSubject.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherWithoutMappedAssignmentContextIsForbiddenFromCreatingScorebook() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/scorebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classSubjectId\":20}"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status);
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficeRetainsAccessToCreateScorebookEndpoint() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/scorebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classSubjectId\":20}"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(404, status, "office request should pass authorization and fail on absent fixture");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessScorebookMetadata() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access scorebook foundation endpoint");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotLookupScorebookByClassSubject() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(CLASS_SUBJECT_LOOKUP_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access class-subject scorebook lookup");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherWithoutApplicationTeacherContextCannotAccessScorebook() throws Exception {
        Scorebook scorebook = scorebookRepository.save(new Scorebook(20L, ScorebookStatus.OPEN));
        int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/scorebooks/" + scorebook.getId()))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher without mapped teacher context should be forbidden");
    }

    @Test
    void anonymousCannotAccessScorebookMetadata() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
    }

    @Test
    void anonymousCannotLookupScorebookByClassSubject() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(CLASS_SUBJECT_LOOKUP_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous lookup should be unauthorized");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficePassesAuthorizationBeforeNotFound() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(
                404,
                status,
                "office should pass authorization and fail only because scorebook is absent");
    }
}
