import { apiClient } from '@/services/apiClient'
import type { CalendarDay, ClassWeeklyLessonLogResponse, LessonLogAuditPage, LessonLogEntry, LessonLogPolicy, LessonLogPolicyPayload, LessonLogRequestFields, ScheduleItem, TeacherDailyScheduleResponse, WeeklyReview, WeeklyReviewPayload, RubricItem, LessonLogRevision, WeeklySummary } from '@/types/lessonLog'

const basePath = '/api/v3/lesson-logs'
type RawPolicy = Omit<LessonLogPolicy, 'rubric'> & { rubric: RubricItem[] | string }
type RawEntry = Partial<LessonLogEntry> & { entryId: number; timetableEntryId: number; classId: number; semesterId: number; lessonDate: string; session: string; periodIndex: number; status: LessonLogEntry['status']; version: number; expiresAt?: string | null }
type RawClass = { classId: number; semesterId: number; classCode?: string | null; className?: string | null }
type RawWeeklyReview = Partial<WeeklyReview> & { reviewId?: number | null; classId?: number; semesterId?: number; weekStart?: string; weekEnd?: string; status: WeeklyReview['status']; version: number; weeklyComment?: string | null; weeklyGrade?: string | null; signedBy?: number | string | null; blockedReason?: string | null; signedSnapshotJson?: string | null }
type RawRevision = Partial<LessonLogRevision> & { revisionId: number; action: string; actorId?: number | null; actorName?: string | null; beforeStateJson?: string | null; afterStateJson?: string | null; createdAt: string }
function rubric(value: RubricItem[] | string | undefined): RubricItem[] {
  if (Array.isArray(value)) return value
  if (!value) return []
  try { const parsed: unknown = JSON.parse(value); return Array.isArray(parsed) ? parsed as RubricItem[] : [] } catch { return [] }
}
function entry(value: RawEntry): LessonLogEntry {
  return {
    entryId: value.entryId, timetableEntryId: value.timetableEntryId, timetableRevisionId: value.timetableRevisionId ?? 0,
    assignmentId: value.assignmentId ?? 0, semesterId: value.semesterId, lessonDate: value.lessonDate,
    classId: value.classId, subjectId: value.subjectId ?? 0, assignedTeacherId: value.assignedTeacherId ?? 0,
    className: value.className ?? 'Lớp chưa xác định', subjectName: value.subjectName ?? 'Môn chưa xác định',
    teacherName: value.teacherName ?? 'Giáo viên chưa xác định', functionalRoomName: value.functionalRoomName ?? null,
    session: value.session as LessonLogEntry['session'], periodIndex: value.periodIndex,
    lessonEndsAt: value.lessonEndsAt ?? value.lessonDate, editWindowExpiresAt: value.editWindowExpiresAt ?? value.expiresAt ?? value.lessonDate,
    title: value.title ?? null, content: value.content ?? null, completionStatus: value.completionStatus ?? null,
    presentCount: value.presentCount ?? null, absentCount: value.absentCount ?? null, absentStudentNotes: value.absentStudentNotes ?? null,
    comments: value.comments ?? null, homework: value.homework ?? null, grade: value.grade ?? null, status: value.status, version: value.version,
    policyId: value.policyId ?? 0, rubric: rubric(value.rubric), rosterCountSnapshot: value.rosterCountSnapshot ?? 0,
    canTeacherEdit: value.canTeacherEdit ?? false, canSubmit: value.canSubmit ?? false, canReview: value.canReview ?? false,
    canAmend: value.canAmend ?? false, canLateRecord: value.canLateRecord ?? false, blockedReason: value.blockedReason ?? null,
    submittedAt: value.submittedAt, submittedBy: value.submittedBy, reviewedAt: value.reviewedAt, reviewedBy: value.reviewedBy, reviewComment: value.reviewComment,
  }
}
function parseJson(value: string | null | undefined): unknown {
  if (!value) return null
  try { return JSON.parse(value) as unknown } catch { return value }
}
function revision(value: RawRevision): LessonLogRevision {
  return { revisionId: value.revisionId, entryId: value.entryId ?? 0, weeklyReviewId: value.weeklyReviewId ?? null, policyId: value.policyId ?? null, action: value.action, actorName: value.actorName ?? (value.actorId == null ? 'Hệ thống' : `Người dùng #${value.actorId}`), createdAt: value.createdAt, reason: value.reason ?? null, beforeState: value.beforeState ?? parseJson(value.beforeStateJson), afterState: value.afterState ?? parseJson(value.afterStateJson) }
}
function auditPage(value: { result?: RawRevision[]; meta?: LessonLogAuditPage['meta'] }): LessonLogAuditPage {
  return { result: (value.result ?? []).map(revision), meta: value.meta ?? { page: 0, pageSize: 0, totalPages: 0, totalItems: 0 } }
}
function days(weekStart: string): CalendarDay[] {
  const start = new Date(`${weekStart}T00:00:00Z`)
  return Array.from({ length: 7 }, (_, index) => { const date = new Date(start); date.setUTCDate(start.getUTCDate() + index); return { date: date.toISOString().slice(0, 10), label: `Thứ ${index + 2}`, kind: 'SCHOOL_DAY' } })
}
function summary(items: LessonLogEntry[]): WeeklySummary {
  const gradeCounts = { A: 0, B: 0, C: 0, D: 0 }
  items.forEach((item) => { if (item.grade && item.status !== 'DRAFT') gradeCounts[item.grade] += 1 })
  return { scheduled: items.length, unlogged: 0, draft: items.filter((item) => item.status === 'DRAFT').length, submitted: items.filter((item) => item.status === 'SUBMITTED').length, reviewed: items.filter((item) => item.status === 'REVIEWED').length, amended: items.filter((item) => item.status === 'AMENDED').length, gradeCounts }
}
function weekly(value: RawWeeklyReview): WeeklyReview {
  const snapshot = value.signedSnapshotJson ? value.signedSnapshotJson.split(',').flatMap((item) => { const [entryId, version] = item.split(':').map(Number); return Number.isFinite(entryId) && Number.isFinite(version) ? [{ entryId, version }] : [] }) : []
  return { reviewId: value.reviewId ?? null, version: value.version, status: value.status, comment: value.weeklyComment ?? value.comment ?? null, grade: (value.weeklyGrade ?? value.grade ?? null) as WeeklyReview['grade'], signedAt: value.signedAt ?? null, signedBy: value.signedBy == null ? null : String(value.signedBy), signedSnapshot: value.signedSnapshot ?? parseJson(value.signedSnapshotJson), expectedEntries: value.expectedEntries ?? snapshot, canSignWeek: value.canSignWeek ?? false, blockedReasons: value.blockedReason ? [value.blockedReason] : (value.blockedReasons ?? []), requiresReason: value.requiresReason ?? value.status === 'STALE' }
}
function schedule(value: { date: string; timezone: string; items: RawEntry[] }): TeacherDailyScheduleResponse {
  const items: ScheduleItem[] = value.items.map((item) => {
    const normalized = entry(item)
    return { ...normalized, entry: normalized.entryId > 0 ? normalized : null, canCreate: item.canCreate ?? false, canLateRecord: item.canLateRecord ?? false, blockedReason: item.blockedReason ?? null }
  })
  return { date: value.date, timezone: value.timezone, items }
}
function policy(value: RawPolicy): LessonLogPolicy { return { ...value, rubric: rubric(value.rubric) } }
export async function getMyLessonSchedule(date: string, token?: string): Promise<TeacherDailyScheduleResponse> { return schedule(await apiClient.get(basePath + '/my-schedule', { token, query: { date } }) as unknown as { date: string; timezone: string; items: RawEntry[] }) }
export async function listLessonLogClasses(semesterId: number, token?: string): Promise<Array<{ id: number; name: string }>> { const result = await apiClient.get(basePath + '/classes', { token, query: { semesterId } }) as unknown as RawClass[]; return result.map((item) => ({ id: item.classId, name: item.className ?? item.classCode ?? `Lớp #${item.classId}` })) }
export async function getClassWeeklyLessonLogs(classId: number, semesterId: number, weekStart: string, token?: string): Promise<ClassWeeklyLessonLogResponse> { const raw = await apiClient.get(`${basePath}/class/${classId}`, { token, query: { semesterId, weekStart } }) as unknown as { classId: number; semesterId: number; weekStart: string; weekEnd: string; items: RawEntry[]; weeklyReview: RawWeeklyReview }; const items = (raw.items ?? []).map(entry); return { classId: raw.classId, className: `Lớp #${raw.classId}`, semesterId: raw.semesterId, weekStart: raw.weekStart, weekEnd: raw.weekEnd, calendarDays: days(raw.weekStart), items, summary: summary(items), weeklyReview: weekly(raw.weeklyReview) } }
export async function getLessonLogEntry(entryId: number, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.get(`${basePath}/entries/${entryId}`, { token }) as unknown as RawEntry) }
export async function listLessonLogRevisions(entryId: number, page = 0, pageSize = 20, token?: string): Promise<LessonLogAuditPage> { return auditPage(await apiClient.get(`${basePath}/entries/${entryId}/revisions`, { token, query: { page, pageSize } }) as { result?: RawRevision[]; meta?: LessonLogAuditPage['meta'] }) }
export async function createLessonLog(payload: { timetableEntryId: number; lessonDate: string } & LessonLogRequestFields, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.post(`${basePath}/entries`, payload, { token }) as unknown as RawEntry) }
export async function updateLessonLog(entryId: number, payload: { expectedVersion: number } & LessonLogRequestFields, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.put(`${basePath}/entries/${entryId}`, payload, { token }) as unknown as RawEntry) }
export async function submitLessonLog(entryId: number, expectedVersion: number, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.post(`${basePath}/entries/${entryId}/submit`, { expectedVersion }, { token }) as unknown as RawEntry) }
export async function reviewLessonLog(entryId: number, expectedVersion: number, reason: string, reviewComment?: string, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.post(`${basePath}/entries/${entryId}/review`, { expectedVersion, comment: reviewComment, reason }, { token }) as unknown as RawEntry) }
export async function amendLessonLog(entryId: number, payload: { expectedVersion: number; reason: string } & LessonLogRequestFields, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.post(`${basePath}/entries/${entryId}/amend`, { expectedVersion: payload.expectedVersion, reason: payload.reason }, { token }) as unknown as RawEntry) }
export async function recordLateLessonLog(payload: { timetableEntryId: number; lessonDate: string; reason: string } & LessonLogRequestFields, token?: string): Promise<LessonLogEntry> { return entry(await apiClient.post(`${basePath}/entries/late-record`, payload, { token }) as unknown as RawEntry) }
export async function signWeeklyReview(classId: number, payload: WeeklyReviewPayload, token?: string): Promise<WeeklyReview> { return weekly(await apiClient.post(`${basePath}/class/${classId}/weekly-review`, payload, { token }) as unknown as RawWeeklyReview) }
export async function listWeeklyReviewRevisions(classId: number, semesterId: number, weekStart: string, page = 0, pageSize = 20, token?: string): Promise<LessonLogAuditPage> { return auditPage(await apiClient.get(`${basePath}/class/${classId}/weekly-review/revisions`, { token, query: { semesterId, weekStart, page, pageSize } }) as { result?: RawRevision[]; meta?: LessonLogAuditPage['meta'] }) }
export async function getLessonLogPolicy(lessonDate?: string, token?: string): Promise<LessonLogPolicy> { return policy(await apiClient.get(basePath + '/policy', { token, query: lessonDate ? { date: lessonDate } : undefined }) as unknown as RawPolicy) }
export async function updateLessonLogPolicy(payload: LessonLogPolicyPayload, token?: string): Promise<LessonLogPolicy> { return policy(await apiClient.put(basePath + '/policy', { ...payload, rubric: JSON.stringify(payload.rubric) }, { token }) as unknown as RawPolicy) }
export async function listLessonLogPolicyRevisions(page = 0, pageSize = 20, token?: string): Promise<LessonLogAuditPage> { return auditPage(await apiClient.get(basePath + '/policy/revisions', { token, query: { page, pageSize } }) as { result?: RawRevision[]; meta?: LessonLogAuditPage['meta'] }) }
