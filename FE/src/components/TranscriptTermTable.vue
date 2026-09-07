<script setup lang="ts">
import { computed } from 'vue'

import type { AssessmentType, ResTermSubjectResultDTO } from '@/types/transcript'

const props = withDefaults(
  defineProps<{
    subjects: ResTermSubjectResultDTO[]
    dtbhk?: number | null
    excusedAbsences?: number | null
    unexcusedAbsences?: number | null
  }>(),
  {
    dtbhk: null,
    excusedAbsences: null,
    unexcusedAbsences: null,
  },
)

function isKttx(type: string | undefined | null): boolean {
  const t = (type ?? '').trim().toUpperCase()
  return t === 'KTTT' || t === 'KTTX'
}

function isKtdk(type: string | undefined | null): boolean {
  const t = (type ?? '').trim().toUpperCase()
  return t === 'KTDK' || t === 'KTĐK'
}

function isKtck(type: string | undefined | null): boolean {
  const t = (type ?? '').trim().toUpperCase()
  return t === 'KTCK'
}

function matchesAssessmentType(colType: string | undefined | null, targetType: string | undefined | null): boolean {
  if (isKttx(targetType)) return isKttx(colType)
  if (isKtdk(targetType)) return isKtdk(colType)
  if (isKtck(targetType)) return isKtck(colType)
  return (colType ?? '').trim().toUpperCase() === (targetType ?? '').trim().toUpperCase()
}

const dynamicColumns = computed(() => {
  let maxKttx = 1
  let maxKtdk = 1
  let maxKtck = 1

  props.subjects.forEach((sub) => {
    (sub.assessmentColumns ?? []).forEach((c) => {
      const colNo = Number(c.columnNo) || 0
      if (isKttx(c.assessmentType)) {
        if (colNo > maxKttx) maxKttx = colNo
      } else if (isKtdk(c.assessmentType)) {
        if (colNo > maxKtdk) maxKtdk = colNo
      } else if (isKtck(c.assessmentType)) {
        if (colNo > maxKtck) maxKtck = colNo
      }
    })

    const kttxCount = sub.assessmentColumns?.filter((c) => isKttx(c.assessmentType)).length ?? 0
    const ktdkCount = sub.assessmentColumns?.filter((c) => isKtdk(c.assessmentType)).length ?? 0
    const ktckCount = sub.assessmentColumns?.filter((c) => isKtck(c.assessmentType)).length ?? 0

    if (kttxCount > maxKttx) maxKttx = kttxCount
    if (ktdkCount > maxKtdk) maxKtdk = ktdkCount
    if (ktckCount > maxKtck) maxKtck = ktckCount
  })

  return {
    maxKttx,
    maxKtdk,
    maxKtck,
    totalAssessmentCols: maxKttx + maxKtdk + maxKtck,
  }
})

function getScore(sub: ResTermSubjectResultDTO, type: AssessmentType | string, colIndex: number): string {
  const cols = (sub.assessmentColumns ?? []).filter((c) => matchesAssessmentType(c.assessmentType, type))
  let col = cols.find((c) => c.columnNo === colIndex)
  if (!col && !cols.some((c) => c.columnNo != null && c.columnNo > 0)) {
    cols.sort((a, b) => a.columnNo - b.columnNo)
    col = cols[colIndex - 1]
  }
  if (!col || col.scoreValue === null || col.scoreValue === undefined) {
    return '—'
  }
  return Number(col.scoreValue).toFixed(1)
}

function formatTbm(sub: ResTermSubjectResultDTO): string {
  if (sub.subjectType === 'SKILL') {
    if (sub.skillScore === null || sub.skillScore === undefined) return '—'
    return Number(sub.skillScore) >= 5 ? 'Đạt' : 'Chưa đạt'
  }
  if (sub.dtbmh === null || sub.dtbmh === undefined) return '—'
  return Number(sub.dtbmh).toFixed(1)
}

const formattedDtbhk = computed(() => {
  if (props.dtbhk === null || props.dtbhk === undefined) return '—'
  return Number(props.dtbhk).toFixed(1)
})
</script>

<template>
  <div class="transcript-term-workspace">
    <div class="table-container">
      <table class="transcript-grid-table" aria-label="Bảng điểm học kỳ">
        <thead>
          <tr class="header-group-row">
            <th rowspan="2" class="col-stt">STT</th>
            <th rowspan="2" class="col-subject">Môn học</th>
            <th :colspan="dynamicColumns.maxKttx" class="group-kttx">KTTX (Kiểm tra thường xuyên)</th>
            <th :colspan="dynamicColumns.maxKtdk" class="group-ktdk">KTĐK (Giữa kỳ)</th>
            <th :colspan="dynamicColumns.maxKtck" class="group-ktck">KTCK (Cuối kỳ)</th>
            <th rowspan="2" class="col-tbm">TBMHK</th>
            <th rowspan="2" class="col-note">Ghi chú</th>
          </tr>
          <tr class="header-sub-row">
            <th v-for="n in dynamicColumns.maxKttx" :key="'kttx-' + n" class="col-sub">{{ n }}</th>
            <th v-for="n in dynamicColumns.maxKtdk" :key="'ktdk-' + n" class="col-sub">{{ n }}</th>
            <th v-for="n in dynamicColumns.maxKtck" :key="'ktck-' + n" class="col-sub">{{ n }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="props.subjects.length === 0">
            <td :colspan="dynamicColumns.totalAssessmentCols + 4" class="empty-cell">
              Chưa có dữ liệu môn học trong học kỳ này
            </td>
          </tr>
          <tr v-for="(sub, idx) in props.subjects" :key="sub.subjectId">
            <td class="cell-center">{{ idx + 1 }}</td>
            <td class="cell-subject">
              <span class="subject-title">{{ sub.subjectName }}</span>
              <span v-if="sub.subjectType === 'SKILL'" class="skill-badge">Đánh giá</span>
            </td>

            <!-- SKILL SUBJECT ROW -->
            <template v-if="sub.subjectType === 'SKILL'">
              <td :colspan="dynamicColumns.totalAssessmentCols" class="cell-center skill-evaluated">
                {{ formatTbm(sub) }}
              </td>
            </template>

            <!-- ACADEMIC SUBJECT ROW -->
            <template v-else>
              <td
                v-for="n in dynamicColumns.maxKttx"
                :key="'val-kttx-' + n"
                class="cell-score"
              >
                {{ getScore(sub, 'KTTT', n) }}
              </td>
              <td
                v-for="n in dynamicColumns.maxKtdk"
                :key="'val-ktdk-' + n"
                class="cell-score"
              >
                {{ getScore(sub, 'KTDK', n) }}
              </td>
              <td
                v-for="n in dynamicColumns.maxKtck"
                :key="'val-ktck-' + n"
                class="cell-score"
              >
                {{ getScore(sub, 'KTCK', n) }}
              </td>
            </template>

            <td class="cell-tbm">{{ formatTbm(sub) }}</td>
            <td class="cell-note">
              <span v-if="sub.subjectType === 'SKILL' && sub.skillScore !== null">
                {{ Number(sub.skillScore) >= 5 ? 'Đạt yêu cầu rèn luyện' : 'Cần rèn luyện thêm' }}
              </span>
              <span v-else>—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- FOOTER SUMMARY CARD -->
    <div class="summary-footer">
      <div class="summary-card">
        <table class="summary-card-table">
          <tbody>
            <tr>
              <td class="summary-label">Điểm trung bình học kì:</td>
              <td class="summary-value highlight">{{ formattedDtbhk }}</td>
            </tr>
            <tr>
              <td class="summary-label">Số buổi vắng có phép:</td>
              <td class="summary-value">{{ props.excusedAbsences ?? '—' }}</td>
            </tr>
            <tr>
              <td class="summary-label">Số buổi vắng không phép:</td>
              <td class="summary-value">{{ props.unexcusedAbsences ?? '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.transcript-term-workspace {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.table-container {
  overflow-x: auto;
  border: 1px solid #94a3b8;
  border-radius: 6px;
  background: #ffffff;
}

.transcript-grid-table {
  width: 100%;
  border-collapse: collapse;
  text-align: center;
  font-size: 13px;
}

.transcript-grid-table th,
.transcript-grid-table td {
  border: 1px solid #94a3b8;
  padding: 8px 10px;
}

.header-group-row th {
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 700;
  vertical-align: middle;
}

.header-sub-row th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  font-size: 12px;
  padding: 6px 8px;
}

.col-stt { width: 45px; }
.col-subject { min-width: 170px; text-align: left; }
.col-sub { width: 46px; }
.col-tbm { width: 85px; }
.col-note { min-width: 140px; text-align: left; }

.cell-center { text-align: center; }
.cell-subject { text-align: left; font-weight: 600; color: #1e293b; }
.subject-title { display: inline-block; margin-right: 6px; }
.skill-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: normal;
  padding: 1px 6px;
  background: #f1f5f9;
  border-radius: 4px;
  color: #64748b;
}

.cell-score {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 500;
  text-align: center;
}

.cell-tbm {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 800;
  color: #1d4ed8;
  background: #f8faff;
  font-size: 14px;
}

.cell-note {
  text-align: left;
  font-size: 12px;
  color: #64748b;
}

.skill-evaluated {
  color: #15803d;
  font-weight: 600;
}

.empty-cell {
  padding: 32px;
  color: #94a3b8;
  font-style: italic;
}

.summary-footer {
  display: flex;
  justify-content: flex-end;
}

.summary-card {
  width: 360px;
  border: 1px solid #94a3b8;
  border-radius: 6px;
  overflow: hidden;
  background: #ffffff;
}

.summary-card-table {
  width: 100%;
  border-collapse: collapse;
}

.summary-card-table td {
  border: 1px solid #94a3b8;
  padding: 8px 14px;
  font-size: 13px;
}

.summary-label {
  font-weight: 700;
  color: #1e293b;
  background: #f1f5f9;
  width: 65%;
}

.summary-value {
  font-weight: 800;
  font-size: 14px;
  text-align: right;
  font-family: ui-monospace, SFMono-Regular, monospace;
}

.summary-value.highlight {
  color: #1d4ed8;
  font-size: 16px;
}
</style>
