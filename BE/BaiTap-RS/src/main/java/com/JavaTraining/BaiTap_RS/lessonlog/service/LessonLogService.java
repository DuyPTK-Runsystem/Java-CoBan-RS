package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.*;
import java.util.*;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.*;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.*;
import com.JavaTraining.BaiTap_RS.assignment.repository.*;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.*;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.*;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.*;
import com.JavaTraining.BaiTap_RS.timetable.repository.*;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports","PMD.CouplingBetweenObjects","PMD.TooManyMethods"})
public class LessonLogService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogEntryRepository entries; private final LessonLogRevisionRepository audits;
    private final LessonLogPolicyRepository policies; private final LessonLogWeeklyReviewRepository weeks;
    private final TimetableEntryRepository timetableEntries; private final TimetableRevisionRepository timetableRevisions;
    private final TimetablePeriodRepository periods; private final TimetableCalendarRepository calendars;
    private final TimetableClosedDateRepository closedDates; private final SubjectTeachingAssignmentRepository assignments;
    private final ClassSubjectRepository classSubjects; private final SemesterRepository semesters;
    private final SchoolClassRepository schoolClasses;
    private final HomeroomAssignmentRepository homerooms;
    private final TeacherRepository teachers; private final StudentYearEnrollmentRepository enrollmentRepository;

    @Transactional public LessonLogEntryResponse create(ReqCreateLessonLogDTO r) {
        Source s=source(r.timetableEntryId(),r.lessonDate()); assertTeacherAssignment(s.assignment.getTeacherId()); String session=s.period.getSession().name();
        if(entries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(s.classId,r.lessonDate(),session,s.period.getPeriodIndex()).isPresent()) throw err(HttpStatus.CONFLICT,"Tiết này đã có sổ đầu bài");
        LessonLogPolicy p=policy(r.lessonDate()); LocalDateTime end=end(r.lessonDate(),s.period), expiry=expiry(p,end); writable(s.semester,end,expiry);
        LessonLogEntry e=new LessonLogEntry(s.timetable.getId(),s.revision.getId(),s.assignment.getId(),s.semester.getId(),s.classId,s.subjectId,s.assignment.getTeacherId(),p.getId(),r.lessonDate(),session,s.period.getPeriodIndex(),snapshot(s,p),end,expiry,actor()); e.setRosterCountSnapshot(roster(s.classId));
        set(e,r.title(),r.content(),r.completionStatus(),r.grade(),r.presentCount(),r.absentCount(),r.comments(),r.absentStudentNotes(),r.homework());
        e=entries.save(e); audit(e,"CREATE",null); ensureWeeklyReview(e); return map(e);
    }
    @Transactional(readOnly=true) public LessonLogEntryResponse get(Long id){LessonLogEntry e=load(id);assertEntryReadScope(e);return map(e);}
    @Transactional public LessonLogEntryResponse update(Long id,ReqUpdateLessonLogDTO r){
        LessonLogEntry e=load(id); assertTeacherAssignment(e.getAssignedTeacherId()); version(e.getVersion(),r.expectedVersion());
        if(e.getStatus()==LessonLogStatus.AMENDED||e.getStatus()==LessonLogStatus.REVIEWED) throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Sổ đã được duyệt/điều chỉnh");
        writable(e.getLessonEndsAt(),e.getEditWindowExpiresAt()); String before=state(e); set(e,r.title(),r.content(),r.completionStatus(),r.grade(),r.presentCount(),r.absentCount(),r.comments(),r.absentStudentNotes(),r.homework()); entries.save(e); audit(e,"UPDATE",null,before,state(e)); invalidateWeek(e); return map(e);
    }
    @Transactional public LessonLogEntryResponse submit(Long id,Long v){LessonLogEntry e=load(id);version(e.getVersion(),v);assertTeacherAssignment(e.getAssignedTeacherId());writable(e.getLessonEndsAt(),e.getEditWindowExpiresAt());complete(e);String before=state(e);e.setStatus(LessonLogStatus.SUBMITTED);e.setSubmittedAt(now());e.setSubmittedBy(actor());entries.save(e);audit(e,"SUBMIT",null,before,state(e));invalidateWeek(e);return map(e);}
    @Transactional public LessonLogEntryResponse review(Long id,ReqTransitionLessonLogDTO r){LessonLogEntry e=load(id);version(e.getVersion(),r.expectedVersion());if(e.getStatus()!=LessonLogStatus.SUBMITTED&&e.getStatus()!=LessonLogStatus.AMENDED)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Chỉ duyệt sổ đã nộp hoặc đã điều chỉnh");String before=state(e);e.setStatus(LessonLogStatus.REVIEWED);e.setReviewComment(r.comment());e.setReviewedAt(now());e.setReviewedBy(actor());entries.save(e);audit(e,"REVIEW",r.reason(),before,state(e));invalidateWeek(e);return map(e);}
    @Transactional public LessonLogEntryResponse amend(Long id,ReqTransitionLessonLogDTO r){LessonLogEntry e=load(id);version(e.getVersion(),r.expectedVersion());if(e.getStatus()==LessonLogStatus.DRAFT)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Không điều chỉnh bản nháp");complete(e);String before=state(e);e.setStatus(LessonLogStatus.AMENDED);e.setReviewedAt(null);e.setReviewedBy(null);entries.save(e);audit(e,"AMEND",r.reason(),before,state(e));invalidateWeek(e);return map(e);}
    @Transactional public LessonLogEntryResponse lateRecord(ReqLateRecordLessonLogDTO r){
        Source s=source(r.timetableEntryId(),r.lessonDate());String session=s.period.getSession().name();if(entries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(s.classId,r.lessonDate(),session,s.period.getPeriodIndex()).isPresent())throw err(HttpStatus.CONFLICT,"Tiết đã có sổ");
        LessonLogPolicy p=policy(r.lessonDate());LocalDateTime end=end(r.lessonDate(),s.period),ex=expiry(p,end);if(now().isBefore(ex))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Chưa đến thời điểm ghi bổ sung");
        LessonLogEntry e=new LessonLogEntry(s.timetable.getId(),s.revision.getId(),s.assignment.getId(),s.semester.getId(),s.classId,s.subjectId,s.assignment.getTeacherId(),p.getId(),r.lessonDate(),session,s.period.getPeriodIndex(),snapshot(s,p),end,ex,actor());e.setRosterCountSnapshot(roster(s.classId));set(e,r.title(),r.content(),r.completionStatus(),r.grade(),r.presentCount(),r.absentCount(),r.comments(),r.absentStudentNotes(),r.homework());complete(e);e.setStatus(LessonLogStatus.AMENDED);e=entries.save(e);audit(e,"LATE_RECORD",r.reason(),null,state(e));invalidateWeek(e);return map(e);
    }
    @Transactional(readOnly=true) public ResultPaginationDTO<LessonLogRevision> revisions(Long id,int page,int size){load(id);Page<LessonLogRevision> p=audits.findByEntryIdOrderByCreatedAtDesc(id,PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size))));return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(p.getNumber(),p.getSize(),p.getTotalPages(),p.getTotalElements()),p.getContent());}
    @Transactional(readOnly=true) public LessonLogScheduleResponse mySchedule(LocalDate date){Long u=actor();if(u==null)return new LessonLogScheduleResponse(date,ZONE.getId(),List.of());Long t=teachers.findByUserId(u).orElseThrow(()->err(HttpStatus.FORBIDDEN,"Tài khoản chưa có giáo viên")).getId();return new LessonLogScheduleResponse(date,ZONE.getId(),entries.findByAssignedTeacherIdAndLessonDate(t,date).stream().map(this::map).toList());}
    @Transactional(readOnly=true) public List<LessonLogClassResponse> classes(Long semesterId){
        semesters.findById(semesterId).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy học kỳ"));
        Set<Long> ids=new LinkedHashSet<>();
        if(manager()) ids.addAll(schoolClasses.findAllBySemesterId(semesterId).stream().map(SchoolClass::getId).toList());
        else {
            Long user=actor(); Long teacher=teachers.findByUserId(user==null?-1L:user).map(x->x.getId()).orElseThrow(()->err(HttpStatus.FORBIDDEN,"Tài khoản chưa có giáo viên"));
            ids.addAll(homerooms.findClassIdsByTeacherIdAndStatus(teacher,AssignmentStatus.ACTIVE));
            assignments.findAllByTeacherIdOrderByValidFromDesc(teacher).forEach(a -> classSubjects.findById(a.getClassSubjectId()).ifPresent(cs -> { if(Objects.equals(cs.getSemesterId(),semesterId)) ids.add(cs.getClassId()); }));
        }
        return schoolClasses.findAllByIdInOrderByClassCodeAsc(ids).stream().map(c->new LessonLogClassResponse(c.getId(),semesterId,c.getClassCode(),c.getClassName())).toList();
    }
    @Transactional(readOnly=true) public LessonLogClassWeekResponse classWeek(Long classId,Long semesterId,LocalDate weekStart){
        requireMonday(weekStart); assertClassScope(classId,weekStart,weekStart.plusDays(6));
        semesters.findById(semesterId).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy học kỳ"));
        List<LessonLogEntry> list=entries.findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(classId,semesterId,weekStart,weekStart.plusDays(6));
        LessonLogWeeklyReview review=weeks.findByClassIdAndSemesterIdAndWeekStart(classId,semesterId,weekStart).orElse(null);
        return new LessonLogClassWeekResponse(classId,semesterId,weekStart,weekStart.plusDays(6),list.stream().map(this::map).toList(),review==null?null:weekly(review,canSign(review,list),blocked(list)));
    }
    @Transactional(readOnly=true) public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(Long classId,Long semesterId,LocalDate weekStart,int page,int size){
        requireMonday(weekStart); assertClassScope(classId,weekStart,weekStart.plusDays(6));
        LessonLogWeeklyReview w=weeks.findByClassIdAndSemesterIdAndWeekStart(classId,semesterId,weekStart).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Chưa có tổng kết tuần"));
        Page<LessonLogRevision> p=audits.findByWeeklyReviewIdOrderByCreatedAtDesc(w.getId(),paging(page,size)); return auditPage(p);
    }
    @Transactional(readOnly=true) public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(int page,int size){return auditPage(audits.findPolicyRevisions(paging(page,size)));}
    @Transactional(readOnly=true) public LessonLogPolicyResponse getPolicy(LocalDate d){return policyResponse(policy(d==null?LocalDate.now():d));}
    @Transactional public LessonLogPolicyResponse putPolicy(ReqPolicyDTO r){LessonLogPolicy old=policies.findTopByOrderByPolicyVersionDesc().orElse(null);if(old!=null&&!Objects.equals(old.getVersion(),r.expectedVersion()))throw err(HttpStatus.CONFLICT,"Policy đã thay đổi");if(r.effectiveFrom().isBefore(LocalDate.now()))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Policy chỉ có hiệu lực từ ngày tương lai");if(!Set.of("FIXED_HOURS","END_OF_WEEK").contains(r.deadlineMode()))throw err(HttpStatus.BAD_REQUEST,"deadlineMode không hợp lệ");if("FIXED_HOURS".equals(r.deadlineMode())&&(r.editWindowHours()==null||r.editWindowHours()<1||r.editWindowHours()>168))throw err(HttpStatus.BAD_REQUEST,"editWindowHours phải từ 1 đến 168");if("END_OF_WEEK".equals(r.deadlineMode())&&r.editWindowHours()!=null)throw err(HttpStatus.BAD_REQUEST,"END_OF_WEEK không nhận editWindowHours");LessonLogPolicy p=new LessonLogPolicy();p.setPolicyVersion(old==null?1:old.getPolicyVersion()+1);p.setEffectiveFrom(r.effectiveFrom());p.setTimezone(r.timezone());p.setDeadlineMode(r.deadlineMode());p.setEditWindowHours(r.editWindowHours());p.setRequireHomeroomReview(!Boolean.FALSE.equals(r.requireHomeroomReview()));p.setRubricJson(r.rubric());p.setCreatedBy(actor());p=policies.save(p);audits.save(new LessonLogRevision(null,null,p.getId(),"POLICY_CREATE",actor(),r.reason(),null,"policyVersion="+p.getPolicyVersion()));return policyResponse(p);}
    @Transactional public WeeklyReviewResponse signWeek(Long classId,ReqWeeklyReviewDTO r){requireMonday(r.weekStart());assertClassScope(classId,r.weekStart(),r.weekStart().plusDays(6));if(r.weekStart().plusDays(6).isAfter(LocalDate.now(ZONE)))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Tuần chưa kết thúc");Semester s=semesters.findById(r.semesterId()).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy học kỳ"));if(s.getStatus()==SemesterStatus.CLOSED||s.getStatus()==SemesterStatus.LOCKED)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Học kỳ đã đóng");List<LessonLogEntry> list=entries.findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(classId,r.semesterId(),r.weekStart(),r.weekStart().plusDays(6));if(list.isEmpty())throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Tuần chưa có tiết");if(list.stream().anyMatch(e->e.getStatus()==LessonLogStatus.DRAFT))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Tuần còn sổ nháp");LessonLogWeeklyReview w=weeks.findByClassIdAndSemesterIdAndWeekStart(classId,r.semesterId(),r.weekStart()).orElseGet(()->{LessonLogWeeklyReview x=new LessonLogWeeklyReview();x.setClassId(classId);x.setSemesterId(r.semesterId());x.setWeekStart(r.weekStart());return x;});version(w.getVersion(),r.expectedVersion());if(r.expectedEntries()==null||r.expectedEntries().size()!=list.size()||r.expectedEntries().stream().anyMatch(x->list.stream().noneMatch(e->Objects.equals(e.getId(),x.entryId())&&Objects.equals(e.getVersion(),x.version()))))throw err(HttpStatus.CONFLICT,"Tập tiết đã thay đổi, vui lòng tải lại");if(w.getStatus()==WeeklyReviewStatus.STALE&&(r.reason()==null||r.reason().isBlank()))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Ký lại phải có lý do");String before=state(w);w.setStatus(WeeklyReviewStatus.SIGNED);w.setWeeklyComment(r.weeklyComment());w.setWeeklyGrade(r.weeklyGrade());w.setSignedAt(now());w.setSignedBy(actor());w.setSignedSnapshotJson(list.stream().map(e->e.getId()+":"+e.getVersion()).collect(java.util.stream.Collectors.joining(",")));w=weeks.save(w);audits.save(new LessonLogRevision(null,w.getId(),null,"SIGN_WEEK",actor(),r.reason(),before,state(w)));return weekly(w,true,null);}
    private LessonLogEntry load(Long id){return entries.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy sổ đầu bài"));}
    private LessonLogPolicy policy(LocalDate d){return policies.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(d).orElseThrow(()->err(HttpStatus.UNPROCESSABLE_ENTITY,"Chưa cấu hình policy sổ đầu bài"));}
    private LocalDateTime now(){return LocalDateTime.now(ZONE);}private Long actor(){return AuditContext.currentUserId();}private AppException err(HttpStatus s,String m){return new AppException(s,m);}
    private void version(Long a,Long e){if(!Objects.equals(a,e))throw err(HttpStatus.CONFLICT,"Dữ liệu đã thay đổi, vui lòng tải lại");}
    private LocalDateTime end(LocalDate d,TimetablePeriod p){return LocalDateTime.of(d,p.getEndTime());}
    private LocalDateTime expiry(LessonLogPolicy p,LocalDateTime e){return "END_OF_WEEK".equals(p.getDeadlineMode())?e.toLocalDate().with(DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay():e.plusHours(p.getEditWindowHours());}
    private void writable(Semester s,LocalDateTime e,LocalDateTime x){if(s.getStatus()==SemesterStatus.CLOSED||s.getStatus()==SemesterStatus.LOCKED)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Học kỳ đã đóng");writable(e,x);}
    private void writable(LocalDateTime e,LocalDateTime x){if(e.isAfter(now())||!now().isBefore(x))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Tiết chưa kết thúc hoặc đã hết hạn chỉnh sửa");}
    private void complete(LessonLogEntry e){if(e.getTitle()==null||e.getTitle().isBlank()||e.getCompletionStatus()==null||e.getGrade()==null||e.getPresentCount()==null||e.getAbsentCount()==null)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Sổ chưa đủ thông tin bắt buộc");if(e.getRosterCountSnapshot()!=null&&e.getPresentCount()+e.getAbsentCount()!=e.getRosterCountSnapshot())throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Sĩ số không khớp tổng");if(!Set.of("A","B","C","D").contains(e.getGrade())||!Set.of("ON_SCHEDULE","BEHIND_SCHEDULE","AHEAD_OF_SCHEDULE").contains(e.getCompletionStatus()))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Giá trị đánh giá không hợp lệ");}
    private void set(LessonLogEntry e,String t,String c,String cs,String g,Integer p,Integer a,String co,String n,String h){e.setTitle(t==null?null:t.trim());e.setContent(c);e.setCompletionStatus(cs);e.setGrade(g);e.setPresentCount(p);e.setAbsentCount(a);e.setComments(co);e.setAbsentStudentNotes(n);e.setHomework(h);}
    private String snapshot(Source s,LessonLogPolicy p){return "{\"revisionId\":"+s.revision.getId()+",\"assignmentId\":"+s.assignment.getId()+",\"classId\":"+s.classId+",\"subjectId\":"+s.subjectId+",\"policyId\":"+p.getId()+"}";}
    private void audit(LessonLogEntry e,String a,String reason){audit(e,a,reason,null,state(e));}
    private void audit(LessonLogEntry e,String a,String reason,String before,String after){audits.save(new LessonLogRevision(e.getId(),null,null,a,actor(),reason,before,after));}
    private LessonLogEntryResponse map(LessonLogEntry e){return new LessonLogEntryResponse(e.getId(),e.getTimetableEntryId(),e.getClassId(),e.getSemesterId(),e.getLessonDate(),e.getSession(),e.getPeriodIndex(),e.getTitle(),e.getContent(),e.getCompletionStatus(),e.getGrade(),e.getPresentCount(),e.getAbsentCount(),e.getComments(),e.getAbsentStudentNotes(),e.getHomework(),e.getStatus(),e.getVersion(),e.getEditWindowExpiresAt());}
    private LessonLogPolicyResponse policyResponse(LessonLogPolicy p){return new LessonLogPolicyResponse(p.getId(),p.getPolicyVersion(),p.getVersion(),p.getEffectiveFrom(),p.getTimezone(),p.getDeadlineMode(),p.getEditWindowHours(),p.isRequireHomeroomReview(),p.getRubricJson());}
    private WeeklyReviewResponse weekly(LessonLogWeeklyReview w,boolean c,String b){return new WeeklyReviewResponse(w.getId(),w.getClassId(),w.getSemesterId(),w.getWeekStart(),w.getWeekStart().plusDays(6),w.getStatus(),w.getWeeklyComment(),w.getWeeklyGrade(),w.getVersion(),w.getSignedAt(),w.getSignedBy(),c,b,w.getSignedSnapshotJson());}
    private void requireMonday(LocalDate d){if(d==null||!d.equals(d.with(DayOfWeek.MONDAY)))throw err(HttpStatus.BAD_REQUEST,"weekStart phải là thứ Hai");}
    private void assertClassScope(Long classId,LocalDate from,LocalDate to){if(manager())return;Long user=actor();Long teacher=teachers.findByUserId(user==null?-1L:user).map(x->x.getId()).orElse(null);if(teacher==null||!homerooms.existsActiveHomeroomBetween(classId,teacher,AssignmentStatus.ACTIVE,from,to))throw err(HttpStatus.FORBIDDEN,"Chỉ giáo viên chủ nhiệm của lớp được xem hoặc ký tuần");}
    private boolean canSign(LessonLogWeeklyReview w,List<LessonLogEntry> list){return w.getStatus()!=WeeklyReviewStatus.SIGNED&&!list.isEmpty()&&list.stream().noneMatch(e->e.getStatus()==LessonLogStatus.DRAFT);}
    private String blocked(List<LessonLogEntry> list){if(list.isEmpty())return "Tuần chưa có tiết";if(list.stream().anyMatch(e->e.getStatus()==LessonLogStatus.DRAFT))return "Tuần còn sổ nháp";return null;}
    private void ensureWeeklyReview(LessonLogEntry e){weeks.findByClassIdAndSemesterIdAndWeekStart(e.getClassId(),e.getSemesterId(),e.getLessonDate().with(DayOfWeek.MONDAY)).orElseGet(()->{LessonLogWeeklyReview w=new LessonLogWeeklyReview();w.setClassId(e.getClassId());w.setSemesterId(e.getSemesterId());w.setWeekStart(e.getLessonDate().with(DayOfWeek.MONDAY));return weeks.save(w);});}
    private void invalidateWeek(LessonLogEntry e){LessonLogWeeklyReview w=weeks.findByClassIdAndSemesterIdAndWeekStart(e.getClassId(),e.getSemesterId(),e.getLessonDate().with(DayOfWeek.MONDAY)).orElse(null);if(w!=null&&w.getStatus()==WeeklyReviewStatus.SIGNED){String before=state(w);w.setStatus(WeeklyReviewStatus.STALE);weeks.save(w);audits.save(new LessonLogRevision(null,w.getId(),null,"INVALIDATE_WEEK",actor(),"Nội dung tiết đã thay đổi",before,state(w)));}}
    private PageRequest paging(int page,int size){return PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by(Sort.Direction.DESC,"createdAt"));}
    private ResultPaginationDTO<LessonLogRevisionResponse> auditPage(Page<LessonLogRevision> p){return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(p.getNumber(),p.getSize(),p.getTotalPages(),p.getTotalElements()),p.getContent().stream().map(x->new LessonLogRevisionResponse(x.getId(),x.getEntryId(),x.getWeeklyReviewId(),x.getPolicyId(),x.getAction(),x.getActorId(),x.getReason(),x.getBeforeStateJson(),x.getAfterStateJson(),x.getCorrelationId(),x.getCreatedAt())).toList());}
    private String state(LessonLogEntry e){return "{\"entryId\":"+e.getId()+",\"status\":\""+e.getStatus()+"\",\"version\":"+e.getVersion()+",\"title\":"+quote(e.getTitle())+",\"presentCount\":"+e.getPresentCount()+",\"absentCount\":"+e.getAbsentCount()+"}";}
    private String state(LessonLogWeeklyReview w){return "{\"reviewId\":"+w.getId()+",\"status\":\""+w.getStatus()+"\",\"version\":"+w.getVersion()+",\"snapshot\":"+quote(w.getSignedSnapshotJson())+"}";}
    private String quote(String value){return value==null?"null":"\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    private int roster(Long classId){return Math.toIntExact(enrollmentRepository.countByCurrentClassIdAndStatus(classId,EnrollmentStatus.ACTIVE));}
    private boolean manager(){Authentication a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN")||x.getAuthority().equals("ROLE_ACADEMIC_OFFICE"));}
    private void assertTeacherAssignment(Long teacherId){if(manager())return;Long u=actor();if(u==null)return;Long actual=teachers.findByUserId(u).map(x->x.getId()).orElse(null);if(!Objects.equals(actual,teacherId))throw err(HttpStatus.FORBIDDEN,"Giáo viên không được phân công tiết này");}
    private void assertEntryReadScope(LessonLogEntry e){if(manager())return;Long u=actor();Long t=teachers.findByUserId(u==null?-1L:u).map(x->x.getId()).orElse(null);if(!Objects.equals(t,e.getAssignedTeacherId()))throw err(HttpStatus.FORBIDDEN,"Không có quyền xem sổ này");}
    private Source source(Long id,LocalDate d){TimetableEntry t=timetableEntries.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy tiết thời khóa biểu"));TimetableRevision r=timetableRevisions.findById(t.getRevisionId()).orElseThrow(()->err(HttpStatus.UNPROCESSABLE_ENTITY,"Revision nguồn không tồn tại"));if(r.getStatus()!=TimetableRevisionStatus.PUBLISHED&&r.getStatus()!=TimetableRevisionStatus.ARCHIVED)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Nguồn tiết chưa được công bố");if(d.isBefore(t.getValidFrom())||d.isAfter(t.getValidTo())||d.isBefore(r.getEffectiveFrom())||(r.getEffectiveTo()!=null&&d.isAfter(r.getEffectiveTo())))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Ngày không thuộc hiệu lực tiết");SubjectTeachingAssignment a=assignments.findById(t.getAssignmentId()).orElseThrow(()->err(HttpStatus.UNPROCESSABLE_ENTITY,"Phân công nguồn không tồn tại"));var cs=classSubjects.findById(a.getClassSubjectId()).orElseThrow(()->err(HttpStatus.UNPROCESSABLE_ENTITY,"Môn lớp nguồn không tồn tại"));Semester s=semesters.findById(r.getSemesterId()).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy học kỳ"));TimetablePeriod p=periods.findById(t.getPeriodId()).orElseThrow(()->err(HttpStatus.UNPROCESSABLE_ENTITY,"Tiết nguồn không tồn tại"));if(p.getDayOfWeek()!=d.getDayOfWeek().getValue())throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Ngày không đúng thứ của tiết");var cal=calendars.findBySemesterId(s.getId());if(cal.isPresent()&&closedDates.existsByCalendarIdAndClosedDate(cal.get().getId(),d))throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Ngày học đã đóng");return new Source(t,r,a,cs.getClassId(),cs.getSubjectId(),s,p);}
    private record Source(TimetableEntry timetable,TimetableRevision revision,SubjectTeachingAssignment assignment,Long classId,Long subjectId,Semester semester,TimetablePeriod period){}
}
