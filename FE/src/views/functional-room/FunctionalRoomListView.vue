<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useConfirm } from 'primevue/useconfirm'

import FunctionalRoomDialog from '@/components/functional-room/FunctionalRoomDialog.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import {
  createFunctionalRoom,
  deleteFunctionalRoom,
  listFunctionalRooms,
  updateFunctionalRoom,
} from '@/services/functionalRoomApi'
import { extractApiErrorMessage } from '@/types/api'
import type { FunctionalRoom, RoomStatus } from '@/types/functionalRoom'
import type { LoadingState } from '@/types/ui'

const confirm = useConfirm()
const { requireAccessToken } = useAuthSession()

const rooms = ref<FunctionalRoom[]>([])
const loadingState = ref<LoadingState>('loading')
const search = ref('')
const selectedStatus = ref<RoomStatus | 'ALL'>('ALL')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const generalError = ref('')
const dialogError = ref('')
const isDialogVisible = ref(false)
const dialogLoading = ref(false)
const editingRoom = ref<FunctionalRoom | null>(null)

const statusFilterOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Tạm ngưng', value: 'INACTIVE' },
]

async function loadRooms() {
  const token = requireAccessToken()
  if (!token) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const res = await listFunctionalRooms(
      {
        search: search.value.trim() || undefined,
        status: selectedStatus.value === 'ALL' ? undefined : selectedStatus.value,
        page: page.value,
        size: size.value,
      },
      token,
    )
    rooms.value = res.result
    totalElements.value = res.meta.totalElements
    loadingState.value = rooms.value.length > 0 ? 'idle' : 'empty'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải danh sách phòng chức năng')
  }
}

watch([selectedStatus], () => {
  page.value = 0
  void loadRooms()
})

function handleSearch() {
  page.value = 0
  void loadRooms()
}

function handlePageChange(event: { page: number; rows: number }) {
  page.value = event.page
  size.value = event.rows
  void loadRooms()
}

function openCreateDialog() {
  editingRoom.value = null
  dialogError.value = ''
  isDialogVisible.value = true
}

function openEditDialog(room: FunctionalRoom) {
  editingRoom.value = room
  dialogError.value = ''
  isDialogVisible.value = true
}

async function handleSaveRoom(payload: {
  code: string
  name: string
  status: RoomStatus
  expectedVersion?: number
}) {
  const token = requireAccessToken()
  if (!token) return
  dialogLoading.value = true
  dialogError.value = ''
  try {
    if (editingRoom.value) {
      await updateFunctionalRoom(
        editingRoom.value.id,
        {
          code: payload.code,
          name: payload.name,
          status: payload.status,
          expectedVersion: payload.expectedVersion ?? 0,
        },
        token,
      )
    } else {
      await createFunctionalRoom({ code: payload.code, name: payload.name }, token)
    }
    isDialogVisible.value = false
    void loadRooms()
  } catch (err) {
    dialogError.value = extractApiErrorMessage(err, 'Không thể lưu thông tin phòng chức năng')
  } finally {
    dialogLoading.value = false
  }
}

function handleDeleteRoom(room: FunctionalRoom) {
  confirm.require({
    message: `Bạn có chắc chắn muốn xóa phòng chức năng "${room.name}" (${room.code})?`,
    header: 'Xác nhận xóa',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Xóa',
    rejectLabel: 'Hủy',
    acceptClass: 'p-button-danger',
    accept: async () => {
      const token = requireAccessToken()
      if (!token) return
      try {
        await deleteFunctionalRoom(room.id, room.version, token)
        void loadRooms()
      } catch (err) {
        generalError.value = extractApiErrorMessage(err, 'Không thể xóa phòng chức năng')
      }
    },
  })
}

onMounted(() => {
  void loadRooms()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <ConfirmDialog />

    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Quản lý phòng chức năng</h1>
        <p class="text-sm text-gray-500">Quản lý các phòng bộ môn, phòng thực hành, nhà đa năng</p>
      </div>
      <Button label="Thêm phòng chức năng" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />

    <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row gap-4 justify-between">
      <div class="flex flex-1 gap-2 max-w-md">
        <InputText
          v-model="search"
          placeholder="Tìm theo mã, tên phòng..."
          class="w-full"
          @keyup.enter="handleSearch"
        />
        <Button icon="pi pi-search" severity="secondary" @click="handleSearch" />
      </div>
      <div class="w-56">
        <Select
          v-model="selectedStatus"
          :options="statusFilterOptions"
          option-label="label"
          option-value="value"
          class="w-full"
        />
      </div>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadRooms"
    />

    <div v-else class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <DataTable
        :value="rooms"
        lazy
        paginator
        :rows="size"
        :total-records="totalElements"
        :first="page * size"
        :rows-per-page-options="[10, 20, 50]"
        table-style="min-width: 50rem"
        @page="handlePageChange"
      >
        <template #empty>
          <div class="p-8 text-center text-gray-500">Không tìm thấy phòng chức năng nào</div>
        </template>

        <Column field="code" header="Mã phòng" style="width: 150px" />
        <Column field="name" header="Tên phòng" />
        <Column field="status" header="Trạng thái" style="width: 160px">
          <template #body="{ data }">
            <Tag
              :value="data.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ngưng'"
              :severity="data.status === 'ACTIVE' ? 'success' : 'secondary'"
            />
          </template>
        </Column>
        <Column field="updatedAt" header="Cập nhật gần nhất" style="width: 200px">
          <template #body="{ data }">
            <span class="text-sm text-gray-600">{{ data.updatedAt ? new Date(data.updatedAt).toLocaleDateString('vi-VN') : '—' }}</span>
          </template>
        </Column>
        <Column header="Thao tác" style="width: 120px" class="text-right">
          <template #body="{ data }">
            <div class="flex justify-end gap-1">
              <Button
                icon="pi pi-pencil"
                severity="secondary"
                text
                rounded
                @click="openEditDialog(data)"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                @click="handleDeleteRoom(data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <FunctionalRoomDialog
      v-model:visible="isDialogVisible"
      :room="editingRoom"
      :loading="dialogLoading"
      :error-message="dialogError"
      @save="handleSaveRoom"
    />
  </div>
</template>

