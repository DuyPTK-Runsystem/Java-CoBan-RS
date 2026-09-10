package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFileItemDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFilePreviewDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFileRowDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFileSummaryDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tạo template roster và preview file cho bulk score v2.
 * Final write vẫn đi qua ScoreEntryService.bulkUpsertScores.
 */
@Service
@SuppressWarnings({
        "PMD.CouplingBetweenObjects",
        "PMD.ExcessiveImports",
        "PMD.GodClass",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity",
        "PMD.TooManyMethods"
})
public class BulkScoreFileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 2_000;
    private static final String XLSX_EXTENSION = ".xlsx";
    private static final int HEADER_ROW_INDEX = 5;
    private static final int FIRST_DATA_ROW_INDEX = HEADER_ROW_INDEX + 1;
    private static final String FORM_FONT = "Times New Roman";
    private static final String[] HEADERS = {"STT", "Mã học sinh", "Họ tên học sinh", "Điểm số", "Ghi chú"};

    private final ScoreEntryContext entryContext;
    private final ScorebookGuard scorebookGuard;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final StudentScoreRepository scoreRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    public BulkScoreFileService(
            ScoreEntryContext entryContext,
            ScorebookGuard scorebookGuard,
            StudentYearEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            StudentScoreRepository scoreRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository) {
        this.entryContext = entryContext;
        this.scorebookGuard = scorebookGuard;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.scoreRepository = scoreRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
    }

    public byte[] createTemplate(Long columnId) {
        AssessmentColumn column = entryContext.findActiveColumn(columnId);
        Scorebook scorebook = entryContext.findWritableScorebook(column.getScorebookId());
        scorebookGuard.assertCanManage(scorebook);
        List<Student> students = loadRoster(scorebook);
        Map<Long, StudentScore> scores = loadScores(columnId, students);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Form nhập điểm");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle referenceStyle = createReferenceStyle(workbook);
            CellStyle inputStyle = createInputStyle(workbook);
            createFormContext(sheet, column, scorebook);
            Row header = sheet.createRow(HEADER_ROW_INDEX);
            for (int index = 0; index < HEADERS.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS[index]);
                cell.setCellStyle(headerStyle);
            }
            int rowNumber = FIRST_DATA_ROW_INDEX;
            int sequence = 1;
            for (Student student : students) {
                StudentScore score = scores.get(student.getId());
                Row row = sheet.createRow(rowNumber++);
                setNumber(row, 0, BigDecimal.valueOf(sequence++), referenceStyle);
                setText(row, 1, student.getStudentCode(), referenceStyle);
                setText(row, 2, student.getStudentName(), referenceStyle);
                setImportValue(row, 3, score, inputStyle);
                setText(row, 4, score == null || score.getNote() == null ? "" : score.getNote(), inputStyle);
            }
            createImportNotes(sheet, rowNumber + 1, createContextStyle(workbook), referenceStyle);
            for (int index = 0; index < HEADERS.length; index++) {
                sheet.setColumnWidth(index, index == 2 || index == 4 ? 28 * 256 : 16 * 256);
            }
            sheet.createFreezePane(0, FIRST_DATA_ROW_INDEX);
            CellRangeAddress filterRange = new CellRangeAddress(
                    HEADER_ROW_INDEX, Math.max(HEADER_ROW_INDEX, rowNumber - 1), 0, HEADERS.length - 1);
            sheet.setAutoFilter(filterRange);
            sheet.protectSheet("");
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo file mẫu nhập điểm", exception);
        }
    }

    private void createFormContext(Sheet sheet, AssessmentColumn column, Scorebook scorebook) {
        ClassSubject classSubject = entryContext.findClassSubject(scorebook.getClassSubjectId());
        SchoolClass schoolClass = schoolClassRepository.findById(classSubject.getClassId())
                .orElseThrow(() -> badRequest("Không tìm thấy lớp của sổ điểm"));
        Subject subject = subjectRepository.findById(classSubject.getSubjectId())
                .orElseThrow(() -> badRequest("Không tìm thấy môn học của sổ điểm"));
        Semester semester = entryContext.findSemesterForScoring(classSubject.getSemesterId());
        CellStyle contextStyle = createContextStyle(sheet.getWorkbook());
        CellStyle titleStyle = createTitleStyle(sheet.getWorkbook());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        Row title = sheet.createRow(0);
        setText(title, 0, "Form nhập điểm", titleStyle);
        Row classAndSubject = sheet.createRow(1);
        setText(classAndSubject, 0, "Lớp", contextStyle);
        setText(classAndSubject, 1, schoolClass.getClassCode(), contextStyle);
        setText(classAndSubject, 3, "Môn", contextStyle);
        setText(classAndSubject, 4, subject.getName(), contextStyle);
        Row semesterRow = sheet.createRow(2);
        setText(semesterRow, 0, "Năm học", contextStyle);
        setText(semesterRow, 1, String.valueOf(semester.getAcademicYearId()), contextStyle);
        setText(semesterRow, 3, "Học kì", contextStyle);
        setText(semesterRow, 4, semester.getName(), contextStyle);
        Row assessment = sheet.createRow(3);
        setText(assessment, 0, "Cột điểm", contextStyle);
        setText(assessment, 1, column.getColumnName(), contextStyle);
        setText(assessment, 3, "Giáo viên", contextStyle);
    }

    private void createImportNotes(Sheet sheet, int firstNoteRow, CellStyle labelStyle, CellStyle noteStyle) {
        Row noteLabel = sheet.createRow(firstNoteRow);
        setText(noteLabel, 0, "Ghi chú:", labelStyle);
        setText(noteLabel, 1, "- Điểm số trong khoảng từ 0-10, làm tròn một (01) chữ số thập phân", noteStyle);
        setText(sheet.createRow(firstNoteRow + 1), 1, "- Giá trị 11 đại diện cho Vắng", noteStyle);
        setText(sheet.createRow(firstNoteRow + 2), 1, "- Giá trị 12 đại diện cho Miễn", noteStyle);
        setText(sheet.createRow(firstNoteRow + 3), 1, "- Giá trị 13 đại diện cho Hủy", noteStyle);
    }

    public ResBulkScoreFilePreviewDTO preview(Long columnId, MultipartFile file) {
        validateFile(file);
        AssessmentColumn column = entryContext.findActiveColumn(columnId);
        Scorebook scorebook = entryContext.findWritableScorebook(column.getScorebookId());
        scorebookGuard.assertCanManage(scorebook);
        List<Student> students = loadRoster(scorebook);
        Map<String, Student> studentsByCode = new HashMap<>();
        for (Student student : students) {
            studentsByCode.put(student.getStudentCode(), student);
        }
        Map<Long, StudentScore> scores = loadScores(columnId, students);

        List<ResBulkScoreFileRowDTO> rows = new ArrayList<>();
        List<ResBulkScoreFileItemDTO> items = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        int totalRows = 0;
        int skippedRows = 0;
        int errorRows = 0;
        int newScores = 0;
        int updatedScores = 0;

        try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() != 1) {
                throw badRequest("File mẫu chỉ được có một trang tính");
            }
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(HEADER_ROW_INDEX));
            DataFormatter formatter = new DataFormatter();
            for (int index = FIRST_DATA_ROW_INDEX; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (isBlankRow(row, formatter)) {
                    continue;
                }
                totalRows++;
                if (totalRows > MAX_DATA_ROWS) {
                    throw badRequest("File mẫu không được quá " + MAX_DATA_ROWS + " dòng dữ liệu");
                }
                ParsedRow parsed = parseRow(row, formatter);
                Student student = studentsByCode.get(parsed.studentCode());
                StudentScore current = student == null ? null : scores.get(student.getId());
                RowDecision decision = validateRow(parsed, student, current, seenCodes);
                rows.add(toResponseRow(index + 1, parsed, student, current, decision));
                if (decision.errorMessage() != null) {
                    errorRows++;
                } else if (decision.skipped()) {
                    skippedRows++;
                } else {
                    items.add(new ResBulkScoreFileItemDTO(
                            student.getId(), student.getStudentCode(), decision.status(), decision.value(),
                            decision.note(), current == null ? null : current.getVersion()));
                    if (current == null) {
                        newScores++;
                    } else {
                        updatedScores++;
                    }
                }
            }
        } catch (IOException exception) {
            throw badRequest("Không thể đọc file mẫu .xlsx", exception);
        }

        return new ResBulkScoreFilePreviewDTO(
                columnId, file.getOriginalFilename(),
                new ResBulkScoreFileSummaryDTO(
                        totalRows, items.size(), skippedRows, errorRows, newScores, updatedScores),
                rows, items);
    }

    private List<Student> loadRoster(Scorebook scorebook) {
        ClassSubject classSubject = entryContext.findClassSubject(scorebook.getClassSubjectId());
        entryContext.findSemesterForScoring(classSubject.getSemesterId());
        List<StudentYearEnrollment> enrollments = enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                classSubject.getClassId(), EnrollmentStatus.ACTIVE);
        List<Long> ids = enrollments.stream().map(StudentYearEnrollment::getStudentId).toList();
        Map<Long, Student> students = new HashMap<>();
        for (Student student : studentRepository.findAllById(ids)) {
            students.put(student.getId(), student);
        }
        return ids.stream().map(students::get).filter(student -> student != null).toList();
    }

    private Map<Long, StudentScore> loadScores(Long columnId, List<Student> students) {
        List<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, StudentScore> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        for (StudentScore score : scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(List.of(columnId), ids)) {
            result.put(score.getStudentId(), score);
        }
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Vui lòng chọn file mẫu");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(XLSX_EXTENSION)) {
            throw badRequest("Chỉ hỗ trợ file .xlsx");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("File mẫu không được quá 10 MB");
        }
    }

    private void validateHeaders(Row header) {
        if (header == null) {
            throw badRequest("File mẫu thiếu dòng tiêu đề");
        }
        DataFormatter formatter = new DataFormatter();
        for (int index = 0; index < HEADERS.length; index++) {
            String actual = formatter.formatCellValue(header.getCell(index)).trim();
            if (!HEADERS[index].equals(actual)) {
                throw badRequest("File mẫu không đúng định dạng cột " + HEADERS[index]);
            }
        }
    }

    private ParsedRow parseRow(Row row, DataFormatter formatter) {
        return new ParsedRow(text(row.getCell(1), formatter), text(row.getCell(2), formatter),
                text(row.getCell(3), formatter), text(row.getCell(4), formatter));
    }

    private RowDecision validateRow(
            ParsedRow row,
            Student student,
            StudentScore current,
            Set<String> seenCodes) {
        if (row.studentCode().isBlank()) {
            return error("Thiếu mã học sinh");
        }
        if (!seenCodes.add(row.studentCode())) {
            return error("Trùng mã học sinh trong file");
        }
        if (student == null) {
            return error("Học sinh không thuộc lớp của cột điểm");
        }
        if (!row.studentName().isBlank() && !row.studentName().equals(student.getStudentName())) {
            return error("Tên học sinh không khớp với mã học sinh");
        }
        boolean noInput = row.valueText().isBlank() && row.note().isBlank();
        if (noInput) {
            return skipped();
        }
        ScoreStatus status = statusFor(row.valueText());
        BigDecimal value = null;
        if (status == ScoreStatus.SCORED && !row.valueText().isBlank()) {
            try {
                value = new BigDecimal(row.valueText().replace(',', '.'));
            } catch (NumberFormatException exception) {
                return error("Giá trị điểm không hợp lệ: " + row.valueText());
            }
        }
        if (value == null && !row.note().isBlank()) {
            if (current == null) {
                return error("Cần nhập điểm cho học sinh chưa có điểm");
            }
            status = current.getScoreStatus();
            value = current.getScoreValue();
        }
        if (status == ScoreStatus.SCORED) {
            if (value == null) {
                return error("Trạng thái Có điểm phải có giá trị điểm");
            }
            try {
                value = normalizeScore(value);
            } catch (AppException exception) {
                return error(exception.getMessage());
            }
        } else if (value != null) {
            return error("Trạng thái " + status + " không được có giá trị điểm");
        }
        if (row.note().length() > 500) {
            return error("Ghi chú không quá 500 ký tự");
        }
        String note = row.note().isBlank() ? null : row.note();
        if (current != null && same(current, status, value, note)) {
            return skipped();
        }
        return new RowDecision(status, value, note, false, null);
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        BigDecimal normalized = value.setScale(1, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.TEN) > 0) {
            throw badRequest("Điểm phải nằm trong khoảng 0.0 đến 10.0");
        }
        return normalized;
    }

    private ScoreStatus statusFor(String raw) {
        return switch (raw) {
            case "11" -> ScoreStatus.ABSENT;
            case "12" -> ScoreStatus.EXEMPTED;
            case "13" -> ScoreStatus.CANCELLED;
            default -> ScoreStatus.SCORED;
        };
    }

    private boolean same(StudentScore current, ScoreStatus status, BigDecimal value, String note) {
        return current.getScoreStatus() == status
                && compare(current.getScoreValue(), value)
                && java.util.Objects.equals(current.getNote(), note);
    }

    private boolean compare(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private ResBulkScoreFileRowDTO toResponseRow(
            int rowNumber,
            ParsedRow parsed,
            Student student,
            StudentScore current,
            RowDecision decision) {
        return new ResBulkScoreFileRowDTO(
                rowNumber,
                parsed.studentCode(),
                student == null ? null : student.getId(),
                student == null ? parsed.studentName() : student.getStudentName(),
                current == null ? null : current.getScoreStatus(),
                current == null ? null : current.getScoreValue(),
                current == null ? null : current.getVersion(),
                decision.status(), decision.value(), decision.note(),
                decision.errorMessage() == null ? (decision.skipped() ? "SKIPPED" : "VALID") : "ERROR",
                decision.errorMessage() == null ? null : "INVALID_ROW",
                decision.errorMessage());
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < HEADERS.length; index++) {
            if (!text(row.getCell(index), formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String text(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private void setText(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void setImportValue(Row row, int index, StudentScore score, CellStyle style) {
        if (score == null) {
            setText(row, index, "", style);
        } else if (score.getScoreStatus() == ScoreStatus.SCORED) {
            setNumber(row, index, score.getScoreValue(), style);
        } else if (score.getScoreStatus() == ScoreStatus.ABSENT) {
            setText(row, index, "11", style);
        } else if (score.getScoreStatus() == ScoreStatus.EXEMPTED) {
            setText(row, index, "12", style);
        } else {
            setText(row, index, "13", style);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontName(FORM_FONT);
        font.setBold(true);
        style.setFont(font);
        style.setLocked(true);
        return style;
    }

    private CellStyle createReferenceStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setLocked(true);
        return style;
    }

    private CellStyle createContextStyle(Workbook workbook) {
        CellStyle style = createReferenceStyle(workbook);
        Font font = workbook.createFont();
        font.setFontName(FORM_FONT);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = createContextStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontName(FORM_FONT);
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createInputStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontName(FORM_FONT);
        style.setFont(font);
        style.setLocked(false);
        return style;
    }

    private RowDecision skipped() {
        return new RowDecision(null, null, null, true, null);
    }

    private RowDecision error(String message) {
        return new RowDecision(null, null, null, false, message);
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }

    private AppException badRequest(String message, Throwable cause) {
        return new AppException(HttpStatus.BAD_REQUEST, message, cause);
    }

    private record ParsedRow(String studentCode, String studentName, String valueText, String note) {
    }

    private record RowDecision(
            ScoreStatus status,
            BigDecimal value,
            String note,
            boolean skipped,
            String errorMessage) {
    }
}
