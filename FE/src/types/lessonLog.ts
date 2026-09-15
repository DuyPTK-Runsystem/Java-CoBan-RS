export type LessonLogStatus = 'DRAFT' | 'SUBMITTED' | 'REVIEWED' | 'AMENDED' | 'UNLOGGED'
export type CompletionStatus = 'ON_SCHEDULE' | 'BEHIND_SCHEDULE' | 'AHEAD_OF_SCHEDULE'
export type LessonGrade = 'A' | 'B' | 'C' | 'D'
export type SessionType = 'MORNING' | 'AFTERNOON'
export type WeeklyReviewStatus = 'UNSIGNED' | 'SIGNED' | 'STALE'
export type DeadlineMode = 'FIXED_HOURS' | 'END_OF_WEEK'

export interface RubricItem { code: LessonGrade; label: string; description: string }
export interface LessonLogPolicy { policyId: number; policyVersion: number; version: number; effectiveFrom: string; timezone: string; deadlineMode: DeadlineMode; editWindowHours: number | null; requireHomeroomReview: boolean; rubric: RubricItem[] }
export interface LessonLogEntry {
  entryId: number
  timetableEntryId: number
  timetableRevisionId: number | null
  assignmentId: number | null
  semesterId: number | null
  lessonDate: string
  classId: number
  subjectId: number | null
  assignedTeacherId: number | null
  className: string | null
  subjectName: string | null
  teacherName: string | null
  functionalRoomName?: string | null
  session: SessionType
  periodIndex: number
  lessonEndsAt: string | null
  editWindowExpiresAt: string | null
  title: string | null
  content: string | null
  completionStatus: CompletionStatus | null
  presentCount: number | null
  absentCount: number | null
  absentStudentNotes: string | null
  comments: string | null
  homework: string | null
  grade: LessonGrade | null
  status: LessonLogStatus
  version: number
  policyId: number | null
  rubric: RubricItem[]
  rosterCountSnapshot: number | null
  canTeacherEdit: boolean
  canSubmit: boolean
  canReview: boolean
  canAmend: boolean
  canLateRecord: boolean
  blockedReason: string | null
  submittedAt?: string | null
  submittedBy?: number | null
  reviewedAt?: string | null
  reviewedBy?: number | null
  reviewComment?: string | null
}
export interface ScheduleItem {
  entryId: number; timetableEntryId: number; timetableRevisionId: number | null; assignmentId: number | null
  classId: number; subjectId: number | null; assignedTeacherId: number | null; semesterId: number
  className: string | null; subjectName: string | null; teacherName: string | null; functionalRoomName: string | null
  lessonDate: string; session: SessionType; periodIndex: number; lessonEndsAt: string
  editWindowExpiresAt: string | null; status: LessonLogStatus; version: number; policyId: number | null
  rosterCountSnapshot: number | null; canCreate: boolean; canLateRecord: boolean; blockedReason: string | null
  entry: LessonLogEntry | null
}
export interface TeacherDailyScheduleResponse { date: string; timezone: string; items: ScheduleItem[] }
export interface CalendarDay { date: string; label: string; kind: 'SCHOOL_DAY' | 'HOLIDAY' | 'OUTSIDE_SEMESTER' }
export interface WeeklySummary { scheduled: number; unlogged: number; draft: number; submitted: number; reviewed: number; amended: number; gradeCounts: Record<LessonGrade, number> }
export interface ExpectedEntry { entryId: number; version: number }
export interface WeeklyReview { reviewId: number | null; version: number; status: WeeklyReviewStatus; comment: string | null; grade: LessonGrade | null; signedAt: string | null; signedBy: string | null; signedSnapshot: unknown | null; expectedEntries: ExpectedEntry[]; canSignWeek: boolean; blockedReasons: string[]; requiresReason?: boolean }
export type ClassWeekItem = LessonLogEntry
export interface ClassWeeklyLessonLogResponse { classId: number; className?: string | null; semesterId: number; weekStart: string; weekEnd: string; calendarDays: CalendarDay[]; items: ClassWeekItem[]; summary: WeeklySummary; weeklyReview: WeeklyReview | null }
export interface LessonLogRevision { revisionId: number; action: string; actorName: string; createdAt: string; reason: string | null; beforeState: unknown; afterState: unknown }
export interface LessonLogRequestFields { title?: string | null; content?: string | null; completionStatus?: CompletionStatus | null; presentCount?: number | null; absentCount?: number | null; absentStudentNotes?: string | null; comments?: string | null; homework?: string | null; grade?: LessonGrade | null }
export interface LessonLogPolicyPayload { expectedVersion: number; effectiveFrom: string; timezone: string; deadlineMode: DeadlineMode; editWindowHours?: number | null; requireHomeroomReview: boolean; rubric: RubricItem[]; reason: string }
export interface WeeklyReviewPayload { semesterId: number; weekStart: string; expectedVersion: number; expectedEntries: ExpectedEntry[]; weeklyComment?: string; weeklyGrade?: LessonGrade; reason?: string }

export interface LessonLogAuditPage {
  result: LessonLogRevision[]
  meta: { page: number; pageSize: number; totalPages: number; totalItems: number }
}
