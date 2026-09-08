<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

type ScorebookStatus = 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED'
type ReviewState = 'READY' | 'EMPTY' | 'FORBIDDEN' | 'CONFLICT'
type ScoreStatus = 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'

interface ScoreRow {
  studentCode: string
  studentName: string
  ktTh: number | null
  ktDk: number | null
  ktCk: number | null
  status?: ScoreStatus
}

const props = withDefaults(defineProps<{
  status?: ScorebookStatus
  reviewState?: ReviewState
  activeTab?: 'grid' | 'columns'
  rows?: ScoreRow[]
}>(), {
  status: 'OPEN',
  reviewState: 'READY',
  activeTab: 'grid',
  rows: () => [
    { studentCode: 'HS-001', studentName: 'Nguyễn Minh An', ktTh: 8.5, ktDk: 7, ktCk: null },
    { studentCode: 'HS-002', studentName: 'Trần Khánh Linh', ktTh: 0, ktDk: 9, ktCk: null },
    { studentCode: 'HS-003', studentName: 'Lê Hoàng Nam', ktTh: null, ktDk: null, ktCk: null, status: 'ABSENT' },
  ],
})

const emit = defineEmits<{
  'update:activeTab': [value: 'grid' | 'columns']
  open: []
  publish: []
  'edit-score': [row: ScoreRow]
}>()

const statusLabels: Record<ScorebookStatus, string> = {
  DRAFT: 'Bản nháp',
  OPEN: 'Đang mở',
  PUBLISHED: 'Đã công bố',
  CLOSED: 'Đã đóng',
}

const statusSeverity: Record<ScorebookStatus, 'secondary' | 'success' | 'info' | 'warn'> = {
  DRAFT: 'secondary', OPEN: 'success', PUBLISHED: 'info', CLOSED: 'warn',
}

const contextOptions = [{ label: '2026–2027 · HK1 · 6A1 · Toán', value: 1 }]
const contextValue = 1

function displayScore(value: number | null, status?: ScoreStatus): string {
  if (status === 'ABSENT') return 'Vắng'
  if (value === null) return 'Chưa nhập'
  return value.toFixed(1)
}
</script>

<template>
  <section class="storybook-screen scorebook-review" aria-label="Scorebook workspace review">
    <header class="page-heading scorebook-review-heading">
      <div>
        <p class="eyebrow">V2 · SCOREBOOK</p>
        <h1>Sổ điểm</h1>
        <p>Quản lý điểm theo đúng năm học, học kỳ, lớp và môn/lớp.</p>
      </div>
      <div class="page-heading-actions">
        <Button label="Mở sổ" icon="pi pi-lock-open" :disabled="props.status !== 'DRAFT'" @click="emit('open')" />
        <Button label="Công bố" icon="pi pi-check" :disabled="props.status !== 'OPEN'" severity="success" @click="emit('publish')" />
      </div>
    </header>

    <div class="content-surface scorebook-review-context">
      <div class="field-group">
        <label for="scorebook-context">Context học vụ</label>
        <Select id="scorebook-context" :model-value="contextValue" :options="contextOptions" option-label="label" option-value="value" fluid />
      </div>
      <div class="scorebook-review-context-summary">
        <span>2026–2027</span><span>Học kỳ 1</span><span>6A1 · Lớp 6A1</span><strong>TOAN · Toán</strong>
      </div>
    </div>

    <div v-if="props.reviewState === 'FORBIDDEN'" class="form-alert form-alert-error" role="alert">
      Bạn không có quyền thao tác scorebook này. Phiên đăng nhập vẫn được giữ nguyên.
    </div>
    <div v-else-if="props.reviewState === 'CONFLICT'" class="form-alert form-alert-warning" role="alert">
      Dữ liệu đã thay đổi ở nơi khác. Hãy tải lại trước khi lưu điểm để tránh ghi đè.
    </div>

    <div class="scorebook-review-tabs" role="tablist" aria-label="Scorebook sections">
      <Button label="Bảng điểm" :outlined="props.activeTab !== 'grid'" icon="pi pi-table" @click="emit('update:activeTab', 'grid')" />
      <Button label="Cấu hình cột" :outlined="props.activeTab !== 'columns'" icon="pi pi-sliders-h" @click="emit('update:activeTab', 'columns')" />
    </div>

    <div class="content-surface">
      <div class="section-heading">
        <div>
          <h2>{{ props.activeTab === 'grid' ? 'Bảng điểm lớp 6A1' : 'Cấu hình assessment column' }}</h2>
          <p class="section-caption">{{ props.activeTab === 'grid' ? 'Điểm 0 là hợp lệ; ô thiếu điểm hiển thị riêng.' : 'KTTT, KTĐK và KTCK theo thứ tự backend trả về.' }}</p>
        </div>
        <Tag :value="statusLabels[props.status]" :severity="statusSeverity[props.status]" />
      </div>

      <template v-if="props.activeTab === 'grid'">
        <div v-if="props.reviewState === 'EMPTY'" class="empty-state"><i class="pi pi-inbox" aria-hidden="true" /><p>Chưa có score entry trong context này.</p></div>
        <div v-else class="scorebook-table-scroll">
          <DataTable :value="props.rows" striped-rows responsive-layout="scroll" class="scorebook-review-table">
            <Column field="studentCode" header="Mã HS" />
            <Column field="studentName" header="Họ và tên" />
            <Column header="KTTT"><template #body="{ data }"><button class="scorebook-score-cell" type="button" @click="emit('edit-score', data)">{{ displayScore(data.ktTh, data.status) }}</button></template></Column>
            <Column header="KTĐK"><template #body="{ data }"><button class="scorebook-score-cell" type="button" @click="emit('edit-score', data)">{{ displayScore(data.ktDk, data.status) }}</button></template></Column>
            <Column header="KTCK"><template #body="{ data }"><button class="scorebook-score-cell" type="button" @click="emit('edit-score', data)">{{ displayScore(data.ktCk, data.status) }}</button></template></Column>
          </DataTable>
        </div>
        <p class="field-hint scorebook-review-note">Trang 1/3 · 23 học sinh · missing score không phải là 0</p>
      </template>
      <template v-else>
        <div class="scorebook-column-summary"><strong>3 cột đang hoạt động</strong><span>Thứ tự và loại cột do backend xác định.</span><Button label="Thêm cột" icon="pi pi-plus" size="small" /></div>
        <DataTable :value="[{ type: 'KTTT', name: 'Thường xuyên 1', order: 1 }, { type: 'KTĐK', name: 'Giữa kỳ', order: 2 }, { type: 'KTCK', name: 'Cuối kỳ', order: 3 }]" responsive-layout="scroll">
          <Column field="order" header="#" /><Column field="type" header="Loại" /><Column field="name" header="Tên cột" /><Column header="Thao tác"><template #body><Button label="Sửa" text size="small" /></template></Column>
        </DataTable>
      </template>
    </div>
  </section>
</template>

<style scoped>
.scorebook-review { max-width: 1180px; margin: 0 auto; }
.scorebook-review-heading { margin-bottom: 20px; }
.scorebook-review-context { display: grid; grid-template-columns: minmax(260px, 1fr) 2fr; align-items: end; gap: 20px; }
.scorebook-review-context-summary { display: flex; flex-wrap: wrap; gap: 8px 18px; padding: 12px 14px; border-left: 3px solid var(--primary); background: #eef2ff; color: #334155; font-size: 13px; }
.scorebook-review-context-summary strong { color: #1e293b; }
.scorebook-review-tabs { display: flex; gap: 10px; margin: 0 0 20px; }
.scorebook-table-scroll { max-width: 100%; overflow-x: auto; }
.scorebook-review-table { min-width: 680px; }
.scorebook-score-cell { min-width: 70px; padding: 7px 9px; border: 0; border-radius: 6px; color: #1e293b; background: transparent; cursor: pointer; text-align: left; }
.scorebook-score-cell:hover, .scorebook-score-cell:focus-visible { outline: 2px solid var(--primary); background: #eef2ff; }
.scorebook-review-note { margin: 14px 0 0; }
.scorebook-column-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 18px; margin-bottom: 16px; }
.scorebook-column-summary span { flex: 1; color: #64748b; font-size: 13px; }
@media (max-width: 680px) { .scorebook-review-context { grid-template-columns: 1fr; } .scorebook-review-tabs { overflow-x: auto; } }
</style>
