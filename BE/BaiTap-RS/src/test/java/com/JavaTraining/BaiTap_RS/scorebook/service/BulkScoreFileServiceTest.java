package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFilePreviewDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.AvoidDuplicateLiterals",
        "PMD.CouplingBetweenObjects",
        "PMD.SingularField",
        "PMD.TooManyStaticImports",
        "PMD.UseVarargs"
})
class BulkScoreFileServiceTest {

    private static final Long COLUMN_ID = 7L;
    private static final Long CLASS_SUBJECT_ID = 21L;
    private static final Long CLASS_ID = 31L;

    @Mock
    private ScoreEntryContext entryContext;

    @Mock
    private ScorebookGuard scorebookGuard;

    @Mock
    private StudentYearEnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentScoreRepository scoreRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private BulkScoreFileService service;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new BulkScoreFileService(
                entryContext, scorebookGuard, enrollmentRepository, studentRepository, scoreRepository,
                schoolClassRepository, subjectRepository);
        student = new Student("Nguyen Minh An", "HS001");
        student.setId(11L);

        AssessmentColumn column = new AssessmentColumn(
                91L, AssessmentType.KTTT, 1, "TX1", BigDecimal.ONE, false);
        column.setId(COLUMN_ID);
        Scorebook scorebook = new Scorebook(CLASS_SUBJECT_ID, ScorebookStatus.OPEN);
        ClassSubject classSubject = new ClassSubject(CLASS_ID, 41L, 51L, ClassSubjectStatus.ACTIVE);

        when(entryContext.findActiveColumn(COLUMN_ID)).thenReturn(column);
        when(entryContext.findWritableScorebook(91L)).thenReturn(scorebook);
        when(entryContext.findClassSubject(CLASS_SUBJECT_ID)).thenReturn(classSubject);
        when(enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(new StudentYearEnrollment(
                        11L, 61L, CLASS_ID, EnrollmentStatus.ACTIVE, java.time.LocalDateTime.now())));
        when(studentRepository.findAllById(List.of(11L))).thenReturn(List.of(student));
        when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(List.of(COLUMN_ID), List.of(11L)))
                .thenReturn(List.of());
        doNothing().when(scorebookGuard).assertCanManage(scorebook);
    }

    @Test
    void previewNormalizesScoreAndReturnsExistingBulkPayloadShape() throws IOException {
        MockMultipartFile file = workbookFile(new String[][] {
                {"HS001", "Nguyen Minh An", "8.47", "kiem tra"}
        });

        ResBulkScoreFilePreviewDTO preview = service.preview(COLUMN_ID, file);

        assertEquals(1, preview.summary().totalRows());
        assertEquals(1, preview.summary().validRows());
        assertEquals(0, preview.summary().errorRows());
        assertEquals(1, preview.items().size());
        assertEquals(new BigDecimal("8.5"), preview.items().get(0).scoreValue());
        assertEquals("VALID", preview.rows().get(0).result());
        assertEquals(11L, preview.rows().get(0).studentId());
    }

    @Test
    void previewReportsDuplicateStudentCodeWithoutCreatingSecondWriteItem() throws IOException {
        MockMultipartFile file = workbookFile(new String[][] {
                {"HS001", "Nguyen Minh An", "8", ""},
                {"HS001", "Nguyen Minh An", "9", ""}
        });

        ResBulkScoreFilePreviewDTO preview = service.preview(COLUMN_ID, file);

        assertEquals(1, preview.summary().validRows());
        assertEquals(1, preview.summary().errorRows());
        assertEquals(1, preview.items().size());
        assertEquals("ERROR", preview.rows().get(1).result());
        assertEquals("INVALID_ROW", preview.rows().get(1).errorCode());
    }

    @Test
    void previewMapsAllSupportedSpecialScoreCodes() throws IOException {
        assertEquals(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus.ABSENT,
                service.preview(COLUMN_ID, workbookFile(new String[][] {{"HS001", "Nguyen Minh An", "11", ""}}))
                        .items().get(0).scoreStatus());
        assertEquals(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus.EXEMPTED,
                service.preview(COLUMN_ID, workbookFile(new String[][] {{"HS001", "Nguyen Minh An", "12", ""}}))
                        .items().get(0).scoreStatus());
        assertEquals(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus.CANCELLED,
                service.preview(COLUMN_ID, workbookFile(new String[][] {{"HS001", "Nguyen Minh An", "13", ""}}))
                        .items().get(0).scoreStatus());
    }

    @Test
    void createTemplateUsesFiveColumnFormLayout() throws IOException {
        SchoolClass schoolClass = new SchoolClass(61L, 1L, "6A1", "6A1", null, null);
        Subject subject = new Subject("TOAN", "Toán", null, null, null);
        Semester semester = mock(Semester.class);
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(java.util.Optional.of(schoolClass));
        when(subjectRepository.findById(41L)).thenReturn(java.util.Optional.of(subject));
        when(entryContext.findSemesterForScoring(51L)).thenReturn(semester);
        when(semester.getAcademicYearId()).thenReturn(61L);
        when(semester.getName()).thenReturn("Học kỳ I");

        byte[] template = service.createTemplate(COLUMN_ID);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            Sheet sheet = workbook.getSheet("Form nhập điểm");
            assertEquals("Form nhập điểm", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(HorizontalAlignment.CENTER, sheet.getRow(0).getCell(0).getCellStyle().getAlignment());
            assertEquals("Điểm số", sheet.getRow(5).getCell(3).getStringCellValue());
            assertEquals("Ghi chú", sheet.getRow(5).getCell(4).getStringCellValue());
            assertEquals(5, sheet.getRow(5).getLastCellNum());
            assertEquals("Ghi chú:", sheet.getRow(8).getCell(0).getStringCellValue());
            assertEquals("- Giá trị 13 đại diện cho Hủy", sheet.getRow(11).getCell(1).getStringCellValue());
        }
    }

    @Test
    void previewRejectsNonXlsxFileBeforeReadingRoster() {
        MockMultipartFile file = new MockMultipartFile("file", "scores.csv", "text/csv", "HS001".getBytes());

        AppException exception = assertThrows(AppException.class, () -> service.preview(COLUMN_ID, file));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Chỉ hỗ trợ file .xlsx", exception.getMessage());
    }

    private MockMultipartFile workbookFile(String[][] values) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Form nhập điểm");
            String[] headers = {"STT", "Mã học sinh", "Họ tên học sinh", "Điểm số", "Ghi chú"};
            writeRow(sheet.createRow(5), headers);
            for (int index = 0; index < values.length; index++) {
                Row row = sheet.createRow(index + 6);
                row.createCell(0).setCellValue(index + 1);
                row.createCell(1).setCellValue(values[index][0]);
                row.createCell(2).setCellValue(values[index][1]);
                row.createCell(3).setCellValue(values[index][2]);
                row.createCell(4).setCellValue(values[index][3]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file", "scores.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }

    private void writeRow(Row row, String[] values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(values[index]);
        }
    }
}
