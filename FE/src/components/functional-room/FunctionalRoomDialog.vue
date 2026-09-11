<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/common/FormAlert.vue'
import type { FunctionalRoom, RoomStatus } from '@/types/functionalRoom'

const props = defineProps<{
  visible: boolean
  room: FunctionalRoom | null
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (
    e: 'save',
    payload: {
      code: string
      name: string
      status: RoomStatus
      expectedVersion?: number
    },
  ): void
}>()

const code = ref('')
const name = ref('')
const status = ref<RoomStatus>('ACTIVE')
const errors = ref<{ code?: string; name?: string }>({})

const isEdit = computed(() => Boolean(props.room))

const statusOptions = [
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Tạm ngưng', value: 'INACTIVE' },
]

watch(
  () => props.room,
  (newVal) => {
    if (newVal) {
      code.value = newVal.code
      name.value = newVal.name
      status.value = newVal.status
    } else {
      code.value = ''
      name.value = ''
      status.value = 'ACTIVE'
    }
    errors.value = {}
  },
  { immediate: true },
)

function validate(): boolean {
  errors.value = {}
  if (!code.value.trim()) {
    errors.value.code = 'Mã phòng không được để trống'
  }
  if (!name.value.trim()) {
    errors.value.name = 'Tên phòng không được để trống'
  }
  return Object.keys(errors.value).length === 0
}

function handleSave() {
  if (!validate()) return
  emit('save', {
    code: code.value.trim(),
    name: name.value.trim(),
    status: status.value,
    expectedVersion: props.room?.version,
  })
}

function handleCancel() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    :header="isEdit ? 'Chỉnh sửa phòng chức năng' : 'Thêm mới phòng chức năng'"
    modal
    :style="{ width: '480px' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

      <div class="flex flex-col gap-1">
        <label for="room-code" class="font-medium text-sm">Mã phòng <span class="text-red-500">*</span></label>
        <InputText
          id="room-code"
          v-model="code"
          placeholder="Ví dụ: LAB_01"
          :disabled="isEdit"
          :invalid="Boolean(errors.code)"
        />
        <small v-if="errors.code" class="text-red-500 text-xs">{{ errors.code }}</small>
      </div>

      <div class="flex flex-col gap-1">
        <label for="room-name" class="font-medium text-sm">Tên phòng <span class="text-red-500">*</span></label>
        <InputText
          id="room-name"
          v-model="name"
          placeholder="Ví dụ: Phòng Thực hành Tin học 1"
          :invalid="Boolean(errors.name)"
        />
        <small v-if="errors.name" class="text-red-500 text-xs">{{ errors.name }}</small>
      </div>

      <div v-if="isEdit" class="flex flex-col gap-1">
        <label for="room-status" class="font-medium text-sm">Trạng thái</label>
        <Select
          id="room-status"
          v-model="status"
          :options="statusOptions"
          option-label="label"
          option-value="value"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2 mt-4">
        <Button label="Hủy" severity="secondary" text @click="handleCancel" />
        <Button
          :label="isEdit ? 'Cập nhật' : 'Tạo mới'"
          :loading="loading"
          @click="handleSave"
        />
      </div>
    </template>
  </Dialog>
</template>

