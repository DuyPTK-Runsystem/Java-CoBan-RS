export type LessonLogStatus = 'DRAFT' | 'SUBMITTED' | 'REVIEWED' | 'AMENDED'
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
  timetableRevisionId: number
  assignmentId: number
  semesterId?: number
  lessonDate: string
  classId: number
  subjectId: number
  assignedTeacherId: number
  className: string
  subjectName: string
  teacherName: string
  functionalRoomName?: string | null
  session: SessionType
  periodIndex: number
  lessonEndsAt: string
  editWindowExpiresAt: string
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
  policyId: number
  rubric: RubricItem[]
  rosterCountSnapshot: number
  canTeacherEdit: boolean
  canSubmit: boolean
  canReview: boolean
  canAmend: boolean
  canLateRecord?: boolean
  blockedReason: string | null
  submittedAt?: string
  submittedBy?: string
  reviewedAt?: string
  reviewedBy?: string
  reviewComment?: string
}
export interface ScheduleItem extends Omit<LessonLogEntry, 'entryId' | 'status' | 'version' | 'canTeacherEdit' | 'canSubmit' | 'canReview' | 'canAmend' | 'blockedReason'> { entry: LessonLogEntry | null; canCreate: boolean; canLateRecord: boolean; blockedReason: string | null }
export interface TeacherDailyScheduleResponse { date: string; timezone: string; items: ScheduleItem[] }
export interface CalendarDay { date: string; label: string; kind: 'SCHOOL_DAY' | 'HOLIDAY' | 'OUTSIDE_SEMESTER' }
export interface WeeklySummary { scheduled: number; unlogged: number; draft: number; submitted: number; reviewed: number; amended: number; gradeCounts: Record<LessonGrade, number> }
export interface ExpectedEntry { entryId: number; version: number }
export interface WeeklyReview { reviewId: number | null; version: number; status: WeeklyReviewStatus; comment: string | null; grade: LessonGrade | null; signedAt: string | null; signedBy: string | null; signedSnapshot: unknown | null; expectedEntries: ExpectedEntry[]; canSignWeek: boolean; blockedReasons: string[]; requiresReason?: boolean }
export interface ClassWeeklyLessonLogResponse { classId: number; className: string; semesterId: number; weekStart: string; weekEnd: string; calendarDays: CalendarDay[]; items: LessonLogEntry[]; summary: WeeklySummary; weeklyReview: WeeklyReview }
export interface LessonLogRevision { revisionId: number; action: string; actorName: string; createdAt: string; reason: string | null; beforeState: unknown; afterState: unknown }
export interface LessonLogRequestFields { title?: string | null; content?: string | null; completionStatus?: CompletionStatus | null; presentCount?: number | null; absentCount?: number | null; absentStudentNotes?: string | null; comments?: string | null; homework?: string | null; grade?: LessonGrade | null }
export interface LessonLogPolicyPayload { expectedVersion: number; effectiveFrom: string; timezone: string; deadlineMode: DeadlineMode; editWindowHours?: number | null; requireHomeroomReview: boolean; rubric: RubricItem[]; reason: string }
export interface WeeklyReviewPayload { semesterId: number; weekStart: string; expectedVersion: number; expectedEntries: ExpectedEntry[]; weeklyComment?: string; weeklyGrade?: LessonGrade; reason?: string }

export interface LessonLogAuditPage {
  result: LessonLogRevision[]
  meta: { page: number; pageSize: number; totalPages: number; totalItems: number }
}
