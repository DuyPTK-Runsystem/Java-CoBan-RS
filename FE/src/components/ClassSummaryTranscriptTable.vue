<script setup lang="ts">
import { computed } from 'vue'

import type {
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
} from '@/types/classTranscript'

const props = withDefaults(
  defineProps<{
    mode: 'TERM' | 'ANNUAL'
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

function formatScore(val: number | null | undefined): string {
  if (val === null || val === undefined) return '—'
  return Number(val).toFixed(1)
}

interface SubjectHeader {
  id: number
  name: string
}

// Collect unique subjects across all students
const subjectsList = computed<SubjectHeader[]>(() => {
  const map = new Map<number, string>()
  if (props.mode === 'TERM') {
    (props.termData?.students ?? []).forEach((stu) => {
      stu.subjects.forEach((s) => {
        if (!map.has(s.subjectId)) {
          map.set(s.subjectId, s.subjectName)
        }
      })
    })
  } else {
    (props.annualData?.students ?? []).forEach((stu) => {
      stu.subjects.forEach((s) => {
        if (!map.has(s.subjectId)) {
          map.set(s.subjectId, s.subjectName)
        }
      })
    })
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
})

// Calculate summary stats
const stats = computed(() => {
  if (props.mode === 'TERM') {
    const students = props.termData?.students ?? []
    const total = students.length
    if (total === 0) return { total: 0, avg: '—', passRate: '—' }

    let sum = 0
    let count = 0
    let passCount = 0
    students.forEach((stu) => {
      if (stu.dtbhk != null) {
        sum += stu.dtbhk
        count++
        if (stu.dtbhk >= 5.0) passCount++
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
      const dtb = stu.finalDtbcn ?? stu.regularDtbcn
      if (dtb != null) {
        sum += dtb
        count++
        if (dtb >= 5.0) passCount++
      }
    })
    return {
      total,
      avg: count > 0 ? (sum / count).toFixed(1) : '—',
      passRate: count > 0 ? `${((passCount / count) * 100).toFixed(1)}%` : '—',
    }
  }
})

const displayTitle = computed(() => {
  return (
    props.title ||
    (props.mode === 'TERM'
      ? 'Bảng điểm tổng kết học kỳ'
      : 'Bảng điểm tổng kết cả năm')
  )
})

const displaySubtitle = computed(() => {
  return props.mode === 'TERM'
    ? 'Tổng hợp điểm tất cả các môn: STT, Họ và tên, các môn học, TBHK, Ghi chú.'
    : 'Tổng hợp cả năm: STT, Họ và tên, các môn học (hiển thị thi lại trực tiếp), TBCN, Ghi chú.'
})
</script>

<template>
  <div class="class-summary-transcript-workspace">
    <div class="table-card">
      <div class="table-top-bar">
        <div>
          <h2 class="table-heading">{{ displayTitle }}</h2>
          <p class="table-subtitle">{{ displaySubtitle }}</p>
        </div>
      </div>

      <div class="table-responsive">
        <!-- MODE 2A: TỔNG KẾT - HỌC KỲ -->
        <table v-if="props.mode === 'TERM'" class="transcript-table" aria-label="Bảng điểm tổng kết học kỳ">
          <thead>
            <tr>
              <th class="col-stt">STT</th>
              <th class="col-name">Họ và tên</th>
              <th
                v-for="sub in subjectsList"
                :key="'head-term-' + sub.id"
                class="col-subject"
              >
                {{ sub.name }}
              </th>
              <th class="col-dtb">TBHK</th>
              <th class="col-note">Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!props.termData || props.termData.students.length === 0">
              <td :colspan="3 + subjectsList.length" class="empty-cell">
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
              <!-- Mon hoc -->
              <td
                v-for="sub in subjectsList"
                :key="'cell-term-' + sub.id"
                class="cell-score"
              >
                {{ formatScore(stu.subjects.find((s) => s.subjectId === sub.id)?.dtbmh) }}
              </td>
              <!-- TBHK -->
              <td class="cell-score cell-dtb">
                {{ formatScore(stu.dtbhk) }}
              </td>
              <td class="col-note cell-center">—</td>
            </tr>
          </tbody>
        </table>

        <!-- MODE 2B: TỔNG KẾT - CẢ NĂM -->
        <table v-else class="transcript-table" aria-label="Bảng điểm tổng kết cả năm">
          <thead>
            <tr>
              <th class="col-stt">STT</th>
              <th class="col-name">Họ và tên</th>
              <th
                v-for="sub in subjectsList"
                :key="'head-ann-' + sub.id"
                class="col-subject"
                :style="sub.name.length > 8 ? 'min-width: 140px;' : ''"
              >
                {{ sub.name }}
              </th>
              <th class="col-dtb">TBCN</th>
              <th class="col-note">Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!props.annualData || props.annualData.students.length === 0">
              <td :colspan="3 + subjectsList.length" class="empty-cell">
                Chưa có dữ liệu học sinh trong lớp
              </td>
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
              <!-- Cac mon hoc voi dinh dang inline thi lai: 2.8 (Thi lại: 5.5) -->
              <td
                v-for="sub in subjectsList"
                :key="'cell-ann-' + sub.id"
                class="cell-score"
              >
                <template v-if="stu.subjects.find((s) => s.subjectId === sub.id)?.retake?.retakeScore != null">
                  <span class="score-struck">
                    {{ formatScore(stu.subjects.find((s) => s.subjectId === sub.id)?.regularDtbmhCn) }}
                  </span>
                  <strong class="retake-inline">
                    (Thi lại: {{ formatScore(stu.subjects.find((s) => s.subjectId === sub.id)?.retake?.retakeScore) }})
                  </strong>
                </template>
                <template v-else>
                  {{ formatScore(stu.subjects.find((s) => s.subjectId === sub.id)?.officialDtbmhCn) }}
                </template>
              </td>
              <!-- TBCN -->
              <td class="cell-score cell-dtb">
                {{ formatScore(stu.finalDtbcn ?? stu.regularDtbcn) }}
              </td>
              <td class="col-note cell-center">
                {{
                  stu.subjects.some((s) => s.retake?.retakeScore != null)
                    ? 'Lên lớp sau thi lại'
                    : (stu.finalDtbcn ?? 0) >= 5.0 ? 'Lên lớp thẳng' : '—'
                }}
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
          <div class="stat-title">Điểm TB cả lớp</div>
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
.class-summary-transcript-workspace {
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

.col-subject {
  min-width: 85px;
  text-align: center;
}

.col-dtb {
  width: 80px;
  text-align: center;
  font-weight: 700;
  background-color: #f8fafc;
}

.col-note {
  width: 150px;
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
  margin-right: 4px;
}

.retake-inline {
  color: #b45309;
  font-weight: 700;
  white-space: nowrap;
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

