<script setup lang="ts">
import { computed } from 'vue'

import type {
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'
import type { ResAssessmentColumnDTO } from '@/types/transcript'

const props = withDefaults(
  defineProps<{
    mode: 'TERM' | 'ANNUAL'
    subjectId: number
    subjectName: string
    title?: string
    termData?: ResClassTermTranscriptDTO | null
    annualData?: ResClassAnnualTranscriptDTO | null
  }>(),
  {
    title: '',
    termData: null,
    annualData: null,
  },
)

const emit = defineEmits<{
  (e: 'selectStudent', studentId: number): void
}>()

function isKttx(type: string | undefined | null): boolean {
  const t = (type ?? '').trim().toUpperCase()
  return t === 'KTTT' || t === 'KTTX'
}

function isKtdk(type: string | undefined | null): boolean {
  const t = (type ?? '').trim().toUpperCase()
  return t === 'KTDK' || t === 'KTĐK'
}

function isKtck(type: string | undefined | null): boolean {
  return (type ?? '').trim().toUpperCase() === 'KTCK'
}

function matchesType(colType: string | undefined | null, target: 'KTTX' | 'KTDK' | 'KTCK'): boolean {
  if (target === 'KTTX') return isKttx(colType)
  if (target === 'KTDK') return isKtdk(colType)
  if (target === 'KTCK') return isKtck(colType)
  return false
}

function formatScore(val: number | null | undefined): string {
  if (val === null || val === undefined) return '—'
  return Number(val).toFixed(1)
}

const dynamicColumns = computed(() => {
  let maxKttx = 1
  let maxKtdk = 1
  if (props.mode === 'TERM' && props.termData) {
    props.termData.students.forEach((stu) => {
      const sub = stu.subjects.find((s) => s.subjectId === props.subjectId)
      if (sub && sub.assessmentColumns) {
        sub.assessmentColumns.forEach((c) => {
          const colNo = Number(c.columnNo) || 1
          if (isKttx(c.assessmentType) && colNo > maxKttx) maxKttx = colNo
          if (isKtdk(c.assessmentType) && colNo > maxKtdk) maxKtdk = colNo
        })
      }
    })
  }
  return {
    kttxCount: Math.min(Math.max(maxKttx, 1), 6),
    ktdkCount: Math.min(Math.max(maxKtdk, 1), 4),
  }
})

function getScore(columns: ResAssessmentColumnDTO[] | undefined, type: 'KTTX' | 'KTDK', colNo: number): string {
  if (!columns) return '—'
  const found = columns.find((c) => matchesType(c.assessmentType, type) && Number(c.columnNo) === colNo)
  return formatScore(found?.scoreValue)
}

function getKtckScore(columns: ResAssessmentColumnDTO[] | undefined): string {
  if (!columns) return '—'
  const found = columns.find((c) => isKtck(c.assessmentType))
  return formatScore(found?.scoreValue)
}

// Summary stats
const stats = computed(() => {
  if (props.mode === 'TERM') {
    const students = props.termData?.students ?? []
    const total = students.length
    if (total === 0) return { total: 0, avg: '—', passRate: '—' }

    let sum = 0
    let count = 0
    let passCount = 0
    students.forEach((stu) => {
      const sub = stu.subjects.find((s) => s.subjectId === props.subjectId)
      const score = sub?.dtbmh ?? sub?.skillScore
      if (score != null) {
        sum += score
        count++
        if (score >= 5.0) passCount++
      }
    })
    return {
      total,
      avg: count > 0 ? (sum / count).toFixed(1) : '—',
      passRate: count > 0 ? `${((passCount / count) * 100).toFixed(1)}%` : '—',
    }
  } else {
    const students = props.annualData?.students ?? []
    const total = students.length
    if (total === 0) return { total: 0, avg: '—', passRate: '—' }

    let sum = 0
    let count = 0
    let passCount = 0
    students.forEach((stu) => {
      const sub = stu.subjects.find((s) => s.subjectId === props.subjectId)
      const score = sub?.officialDtbmhCn ?? sub?.regularDtbmhCn
      if (score != null) {
        sum += score
        count++
        if (score >= 5.0) passCount++
      }
    })
    return {
      total,
      avg: count > 0 ? (sum / count).toFixed(1) : '—',
      passRate: count > 0 ? `${((passCount / count) * 100).toFixed(1)}%` : '—',
    }
  }
})
</script>

<template>
  <div class="class-subject-transcript-workspace">
    <div class="table-card">
      <div class="table-top-bar">
        <div>
          <h2 class="table-heading">{{ props.title || `Bảng điểm Môn ${props.subjectName}` }}</h2>
          <p class="table-subtitle">
            {{
              props.mode === 'TERM'
                ? 'Bảng điểm học kỳ: STT, Họ và tên, KTTX (1..n), KTĐK (1..m), KTCK, TBMHK, Ghi chú.'
                : 'Bảng điểm cả năm: STT, Họ và tên, TBM HK1, TBM HK2, ĐTBCN ban đầu, Thi lại, ĐTBCN chính thức.'
            }}
          </p>
        </div>
      </div>

      <div class="table-responsive">
        <!-- MODE 1A: MÔN - HỌC KỲ -->
        <table v-if="props.mode === 'TERM'" class="transcript-table" aria-label="Bảng điểm môn học kỳ">
          <thead>
            <tr>
              <th rowspan="2" class="col-stt">STT</th>
              <th rowspan="2" class="col-name">Họ và tên</th>
              <th :colspan="dynamicColumns.kttxCount">KTTX</th>
              <th :colspan="dynamicColumns.ktdkCount">KTĐK</th>
              <th rowspan="2" class="col-score">KTCK</th>
              <th rowspan="2" class="col-dtb">TBMHK</th>
              <th rowspan="2" class="col-note">Ghi chú</th>
            </tr>
            <tr>
              <th v-for="i in dynamicColumns.kttxCount" :key="'tx-' + i" class="col-subscore">{{ i }}</th>
              <th v-for="i in dynamicColumns.ktdkCount" :key="'dk-' + i" class="col-subscore">{{ i }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!props.termData || props.termData.students.length === 0">
              <td :colspan="6 + dynamicColumns.kttxCount + dynamicColumns.ktdkCount" class="empty-cell">
                Chưa có dữ liệu học sinh trong lớp
              </td>
            </tr>
            <tr v-for="(stu, idx) in props.termData?.students ?? []" :key="stu.studentId">
              <td class="cell-center">{{ idx + 1 }}</td>
              <td class="col-name">
                <button
                  type="button"
                  class="student-link-btn"
                  :title="'Xem bảng điểm chi tiết của ' + stu.fullName"
                  @click="emit('selectStudent', stu.studentId)"
                >
                  {{ stu.fullName || stu.studentCode }}
                </button>
              </td>
              <!-- KTTX -->
              <td
                v-for="i in dynamicColumns.kttxCount"
                :key="'val-tx-' + i"
                class="cell-score"
              >
                {{ getScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.assessmentColumns, 'KTTX', i) }}
              </td>
              <!-- KTDK -->
              <td
                v-for="i in dynamicColumns.ktdkCount"
                :key="'val-dk-' + i"
                class="cell-score"
              >
                {{ getScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.assessmentColumns, 'KTDK', i) }}
              </td>
              <!-- KTCK -->
              <td class="cell-score">
                {{ getKtckScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.assessmentColumns) }}
              </td>
              <!-- TBMHK -->
              <td class="cell-score cell-dtb">
                {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.dtbmh) }}
              </td>
              <td class="col-note cell-center">—</td>
            </tr>
          </tbody>
        </table>

        <!-- MODE 1B: MÔN - CẢ NĂM -->
        <table v-else class="transcript-table" aria-label="Bảng điểm môn cả năm">
          <thead>
            <tr>
              <th class="col-stt">STT</th>
              <th class="col-name">Họ và tên</th>
              <th class="col-score">TBM HK1</th>
              <th class="col-score">TBM HK2</th>
              <th class="col-score">ĐTBCN Ban đầu</th>
              <th class="col-score">Điểm Thi lại</th>
              <th class="col-dtb">ĐTBCN Chính thức</th>
              <th class="col-note">Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!props.annualData || props.annualData.students.length === 0">
              <td colspan="8" class="empty-cell">Chưa có dữ liệu học sinh trong lớp</td>
            </tr>
            <tr v-for="(stu, idx) in props.annualData?.students ?? []" :key="stu.studentId">
              <td class="cell-center">{{ idx + 1 }}</td>
              <td class="col-name">
                <button
                  type="button"
                  class="student-link-btn"
                  :title="'Xem bảng điểm chi tiết của ' + stu.fullName"
                  @click="emit('selectStudent', stu.studentId)"
                >
                  {{ stu.fullName || stu.studentCode }}
                </button>
              </td>
              <!-- TBM HK1 -->
              <td class="cell-score">
                {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.hk1) }}
              </td>
              <!-- TBM HK2 -->
              <td class="cell-score">
                {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.hk2) }}
              </td>
              <!-- DTBCN Ban đầu -->
              <td
                class="cell-score"
                :class="{
                  'score-struck': stu.subjects.find((s) => s.subjectId === props.subjectId)?.retake != null,
                }"
              >
                {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.regularDtbmhCn) }}
              </td>
              <!-- Thi lại -->
              <td class="cell-score">
                <span
                  v-if="stu.subjects.find((s) => s.subjectId === props.subjectId)?.retake?.retakeScore != null"
                  class="badge-retake"
                >
                  {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.retake?.retakeScore) }}
                </span>
                <span v-else>—</span>
              </td>
              <!-- DTBCN Chinh thuc -->
              <td class="cell-score cell-dtb">
                {{ formatScore(stu.subjects.find((s) => s.subjectId === props.subjectId)?.officialDtbmhCn) }}
              </td>
              <td class="col-note cell-center">
                {{ stu.subjects.find((s) => s.subjectId === props.subjectId)?.retake?.note || '—' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- SUMMARY FOOTER STATS -->
      <div class="summary-grid">
        <div class="stat-card">
          <div class="stat-title">Sĩ số lớp</div>
          <div class="stat-val">{{ stats.total }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Điểm TB môn cả lớp</div>
          <div class="stat-val primary">{{ stats.avg }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Tỷ lệ Đạt (>= 5.0)</div>
          <div class="stat-val success">{{ stats.passRate }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.class-subject-transcript-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-card {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-heading {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.table-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.table-responsive {
  overflow-x: auto;
}

.transcript-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  text-align: left;
}

.transcript-table th,
.transcript-table td {
  border: 1px solid #cbd5e1;
  padding: 8px 10px;
}

.transcript-table thead th {
  background-color: #f1f5f9;
  font-weight: 700;
  color: #334155;
  text-align: center;
}

.col-stt {
  width: 50px;
  text-align: center;
}

.col-name {
  min-width: 180px;
}

.col-score,
.col-subscore {
  width: 60px;
  text-align: center;
}

.col-dtb {
  width: 80px;
  text-align: center;
  font-weight: 700;
  background-color: #f8fafc;
}

.col-note {
  width: 140px;
}

.cell-center {
  text-align: center;
}

.cell-score {
  text-align: center;
}

.cell-dtb {
  font-weight: 700;
  color: #1d4ed8;
  background-color: #f8fafc;
}

.score-struck {
  text-decoration: line-through;
  color: #94a3b8;
}

.badge-retake {
  display: inline-block;
  background-color: #ffedd5;
  color: #9a3412;
  border: 1px solid #fdba74;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 12px;
}

.empty-cell {
  text-align: center;
  padding: 24px;
  color: #64748b;
}

.student-link-btn {
  background: none;
  border: none;
  padding: 0;
  color: #0f172a;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
  text-decoration-color: #cbd5e1;
}

.student-link-btn:hover {
  color: #1d4ed8;
  text-decoration-color: #1d4ed8;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #cbd5e1;
}

.stat-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 14px;
}

.stat-title {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  margin-bottom: 2px;
}

.stat-val {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.stat-val.primary {
  color: #1d4ed8;
}

.stat-val.success {
  color: #15803d;
}
</style>

