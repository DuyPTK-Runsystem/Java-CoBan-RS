import type { PlacementResultsPage, PlacementSession } from '@/types/placement'

export const placementReviewFixture: PlacementSession = {
  id: 74,
  academicYearId: 2026,
  targetGradeId: 8,
  status: 'READY_FOR_CONFIRM',
  ruleVersion: '074-v1',
  version: 3,
  targetClassIds: [81, 82],
  targetClasses: [
    { classId: 81, classCode: '8A1', className: 'Lớp 8A1', profile: 'REGULAR', capacity: 35, genderTargetMale: 17, genderTargetFemale: 18 },
    { classId: 82, classCode: '8A2', className: 'Lớp 8A2', profile: 'ADVANCED', capacity: 30, genderTargetMale: 15, genderTargetFemale: 15 },
  ],
  results: [
    { id: 1, studentId: 1, targetClassId: 81, resultStatus: 'AUTO_ASSIGNED', score: 8.6, issueCode: null, issueSeverity: null, explanation: 'Phân bổ theo cách cân bằng học lực và nam nữ.' },
    { id: 2, studentId: 2, targetClassId: null, resultStatus: 'MANUAL_REQUIRED', score: null, issueCode: 'MISSING_DATA', issueSeverity: 'WARNING', explanation: 'Thiếu điểm hoặc giới tính; tiếp tục bằng quy trình xếp lớp thủ công hiện có.' },
  ],
}

export const placementResultsPageFixture: PlacementResultsPage = {
  meta: {
    page: 0,
    pageSize: 20,
    totalPages: 1,
    totalItems: 2,
  },
  result: placementReviewFixture.results,
}
