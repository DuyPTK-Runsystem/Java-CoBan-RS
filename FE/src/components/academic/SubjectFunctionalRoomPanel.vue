<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import MultiSelect from 'primevue/multiselect'

import FormAlert from '@/components/common/FormAlert.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { lookupFunctionalRooms } from '@/services/functionalRoomApi'
import { getRoomsForSubject, updateRoomsForSubject } from '@/services/subjectFunctionalRoomApi'
import { extractApiErrorMessage } from '@/types/api'
import type { FunctionalRoom } from '@/types/functionalRoom'

const props = defineProps<{
  subjectId: number
  subjectName?: string
}>()

const { requireAccessToken } = useAuthSession()

const availableRooms = ref<FunctionalRoom[]>([])
const selectedRoomIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const successMessage = ref('')

async function loadData() {
  const token = requireAccessToken()
  if (!token || !props.subjectId) return
  loading.value = true
  error.value = ''
  successMessage.value = ''
  try {
    const [lookupList, assigned] = await Promise.all([
      lookupFunctionalRooms({ status: 'ACTIVE' }, token),
      getRoomsForSubject(props.subjectId, token),
    ])
    availableRooms.value = lookupList
    selectedRoomIds.value = assigned.rooms.map((r) => r.id)
  } catch (err) {
    error.value = extractApiErrorMessage(err, 'Không thể tải thông tin phòng chức năng của môn học')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.subjectId,
  () => {
    void loadData()
  },
)

async function handleSave() {
  const token = requireAccessToken()
  if (!token || !props.subjectId) return
  saving.value = true
  error.value = ''
  successMessage.value = ''
  try {
    const res = await updateRoomsForSubject(
      props.subjectId,
      { functionalRoomIds: selectedRoomIds.value },
      token,
    )
    selectedRoomIds.value = res.rooms.map((r) => r.id)
    successMessage.value = 'Đã cập nhật cấu hình phòng chức năng thành công'
  } catch (err) {
    error.value = extractApiErrorMessage(err, 'Không thể lưu phòng chức năng cho môn học')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <section class="subject-functional-room-panel" aria-labelledby="subject-functional-room-title">
    <div class="subject-functional-room-header">
      <div class="subject-functional-room-copy">
        <h4 id="subject-functional-room-title">Phòng chức năng sử dụng cho môn học</h4>
        <p>
          Chỉ các môn học có phòng chức năng được chọn mới yêu cầu phòng khi xếp thời khóa biểu.
        </p>
      </div>
      <Button
        class="subject-functional-room-save"
        label="Lưu cấu hình"
        size="small"
        :loading="saving"
        :disabled="loading"
        @click="handleSave"
      />
    </div>

    <div class="subject-functional-room-feedback" aria-live="polite">
      <FormAlert v-if="error" :message="error" tone="error" />
      <FormAlert v-if="successMessage" :message="successMessage" tone="success" />
    </div>

    <div class="subject-functional-room-field">
      <label for="subject-functional-room-select">Chọn phòng chức năng áp dụng:</label>
      <MultiSelect
        v-model="selectedRoomIds"
        input-id="subject-functional-room-select"
        :options="availableRooms"
        option-label="name"
        option-value="id"
        placeholder="Chọn các phòng bộ môn phù hợp..."
        :loading="loading"
        display="chip"
        class="subject-functional-room-select"
      />
    </div>
  </section>
</template>
