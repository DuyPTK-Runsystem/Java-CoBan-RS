<script setup lang="ts">
import { computed } from 'vue'

import type { ResAnnualSubjectResultDTO } from '@/types/transcript'

const props = withDefaults(
  defineProps<{
    subjects: ResAnnualSubjectResultDTO[]
    regularDtbcn?: number | null
    finalDtbcn?: number | null
  }>(),
  {
    regularDtbcn: null,
    finalDtbcn: null,
  },
)

function formatScore(val: number | null | undefined): string {
  if (val === null || val === undefined) return '—'
  return Number(val).toFixed(1)
}

const formattedRegularDtb = computed(() => formatScore(props.regularDtbcn))
const formattedFinalDtb = computed(() => formatScore(props.finalDtbcn))
</script>

<template>
  <div class="transcript-annual-workspace">
    <div class="table-container">
      <table class="transcript-grid-table" aria-label="Bảng điểm cả năm">
        <thead>
          <tr class="header-row">
            <th class="col-stt">STT</th>
            <th class="col-subject">Môn học</th>
            <th class="col-term">ĐTB HK1</th>
            <th class="col-term">ĐTB HK2</th>
            <th class="col-term">ĐTB Cả năm</th>
            <th class="col-retake">Điểm thi lại</th>
            <th class="col-final">Điểm chính thức</th>
            <th class="col-note">Ghi chú & kết quả thi lại</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="props.subjects.length === 0">
            <td colspan="8" class="empty-cell">
              Chưa có dữ liệu bảng điểm cả năm
            </td>
          </tr>
          <tr v-for="(sub, idx) in props.subjects" :key="sub.subjectId">
            <td class="cell-center">{{ idx + 1 }}</td>
            <td class="cell-subject">
              <span class="subject-title">{{ sub.subjectName }}</span>
              <span v-if="sub.subjectType === 'SKILL'" class="skill-badge">Đánh giá</span>
            </td>
            <td class="cell-score">{{ formatScore(sub.hk1) }}</td>
            <td class="cell-score">{{ formatScore(sub.hk2) }}</td>
            <td class="cell-score" :class="{ 'score-struck': sub.calculationSource === 'RETAKE' }">
              {{ formatScore(sub.regularDtbmhCn) }}
            </td>
            <td class="cell-score" :class="{ 'retake-score': sub.retake?.retakeScore != null }">
              {{ formatScore(sub.retake?.retakeScore) }}
            </td>
            <td class="cell-tbm" :class="{ 'retake-score': sub.calculationSource === 'RETAKE' }">
              {{ formatScore(sub.officialDtbmhCn) }}
            </td>
            <td class="cell-note">
              <template v-if="sub.calculationSource === 'RETAKE' && sub.retake">
                <span class="badge retake-badge">RETAKE</span>
                <span class="retake-detail">
                  Thi lại (điểm cũ: {{ formatScore(sub.retake.preRetakeScore) }} ➔ {{ formatScore(sub.retake.retakeScore) }})
                </span>
              </template>
              <template v-else-if="sub.retake && sub.retake.status === 'PLANNED'">
                <span class="badge planned-badge">PLANNED</span>
                <span class="retake-detail">Đã lên lịch thi lại</span>
              </template>
              <template v-else>
                <span class="badge regular-badge">REGULAR</span>
                <span class="retake-detail">Đạt chuẩn</span>
              </template>
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
              <td class="summary-label">ĐTB cả năm ban đầu:</td>
              <td class="summary-value">{{ formattedRegularDtb }}</td>
            </tr>
            <tr>
              <td class="summary-label">ĐTB cả năm chính thức:</td>
              <td class="summary-value highlight">{{ formattedFinalDtb }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.transcript-annual-workspace {
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

.header-row th {
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 700;
  vertical-align: middle;
}

.col-stt { width: 45px; }
.col-subject { min-width: 170px; text-align: left; }
.col-term { width: 85px; }
.col-retake { width: 100px; }
.col-final { width: 110px; }
.col-note { min-width: 180px; text-align: left; }

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

.score-struck {
  text-decoration: line-through;
  color: #94a3b8;
}

.retake-score {
  color: #b91c1c;
  font-weight: 700;
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

.badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 6px;
}

.retake-badge { background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; }
.planned-badge { background: #fef3c7; color: #b45309; border: 1px solid #fde68a; }
.regular-badge { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; }

.retake-detail {
  font-size: 12px;
  color: #334155;
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
