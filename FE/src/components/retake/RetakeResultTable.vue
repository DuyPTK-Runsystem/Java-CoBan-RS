<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'

import type { RetakeRowItem } from '@/types/retake'

const props = withDefaults(
  defineProps<{
    items: RetakeRowItem[]
    loading?: boolean
  }>(),
  {
    loading: false,
  },
)

const emit = defineEmits<{
  editScore: [item: RetakeRowItem]
  cancel: [item: RetakeRowItem]
  viewDetail: [item: RetakeRowItem]
}>()

function formatScore(score: number | null | undefined): string {
  if (score === null || score === undefined) return '—'
  return score.toFixed(1)
}
</script>

<template>
  <div class="table-wrap">
    <table class="table" data-testid="retake-table">
      <thead>
        <tr>
          <th>Học sinh</th>
          <th>Năm / môn</th>
          <th>Trước thi lại</th>
          <th>Điểm thi lại</th>
          <th>Official sau tính</th>
          <th>Calculation</th>
          <th>Trạng thái</th>
          <th>Thao tác</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in props.items" :key="item.retakeId" :data-testid="`retake-row-${item.retakeId}`">
          <td>
            <strong>{{ item.studentName || `Học sinh #${item.studentId}` }}</strong>
            <div class="muted">
              studentId: {{ item.studentId }}
              <template v-if="item.studentCode"> · {{ item.studentCode }}</template>
            </div>
          </td>
          <td>
            <strong>{{ item.academicYearCode || `Năm #${item.academicYearId}` }}</strong>
            <div class="muted">
              {{ item.subjectName || 'Môn học' }} · subjectId: {{ item.subjectId }}
            </div>
          </td>
          <td class="score before">
            {{ formatScore(item.preRetakeScore) }}
          </td>
          <td>
            <template v-if="item.status === 'CANCELLED'">
              <span class="muted">—</span>
            </template>
            <template v-else-if="item.retakeScore !== null && item.retakeScore !== undefined">
              <span class="score after">{{ formatScore(item.retakeScore) }}</span>
            </template>
            <template v-else>
              <span class="muted">Chưa nhập</span>
            </template>
          </td>
          <td>
            <template v-if="item.status === 'CANCELLED'">
              <span class="muted">—</span>
            </template>
            <template v-else-if="item.officialDtbmhCn !== null && item.officialDtbmhCn !== undefined">
              <span class="score after">{{ formatScore(item.officialDtbmhCn) }}</span>
            </template>
            <template v-else>
              <span class="muted">—</span>
            </template>
          </td>
          <td>
            <template v-if="item.status === 'CANCELLED'">
              <Tag value="Không áp dụng" severity="secondary" />
            </template>
            <template v-else-if="item.calculationStatus === 'IN_PROGRESS'">
              <Tag value="IN_PROGRESS" severity="warn" />
              <div class="muted">Đang chờ worker</div>
            </template>
            <template v-else-if="item.calculationStatus === 'FINISH'">
              <Tag value="FINISH" severity="success" />
              <div v-if="item.lastTaskId" class="muted">task #{{ item.lastTaskId }}</div>
              <div v-else class="muted">Đã đồng bộ</div>
            </template>
            <template v-else>
              <Tag value="—" severity="secondary" />
              <div class="muted">Chưa tạo task</div>
            </template>
          </td>
          <td>
            <Tag
              v-if="item.status === 'PLANNED'"
              value="PLANNED"
              severity="warn"
            />
            <Tag
              v-else-if="item.status === 'SCORED'"
              value="SCORED"
              severity="success"
            />
            <Tag
              v-else
              value="CANCELLED"
              severity="secondary"
            />
          </td>
          <td>
            <div v-if="item.status === 'CANCELLED'" class="muted">
              Read-only
            </div>
            <div v-else class="row-actions">
              <Button
                :label="item.status === 'PLANNED' ? 'Nhập điểm' : 'Xem/sửa'"
                text
                size="small"
                :data-testid="`btn-score-${item.retakeId}`"
                @click="emit('editScore', item)"
              />
              <Button
                label="Hủy"
                text
                severity="danger"
                size="small"
                :data-testid="`btn-cancel-${item.retakeId}`"
                @click="emit('cancel', item)"
              />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrap {
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}
.table th,
.table td {
  text-align: left;
  padding: 12px 10px;
  border-bottom: 1px solid #e9edf3;
  vertical-align: top;
}
.table th {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #68768e;
  background: #fafbfd;
}
.table tr:hover td {
  background: #fbfcfe;
}
.muted {
  color: #6c7890;
  font-size: 12px;
  margin-top: 2px;
}
.score {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  font-size: 14px;
}
.score.before {
  color: #697790;
}
.score.after {
  color: #176d4b;
}
.row-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
