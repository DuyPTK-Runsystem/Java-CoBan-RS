<script setup lang="ts">
import Button from 'primevue/button'

export interface NavigationItem {
  label: string
  to: string
  icon: string
  active?: boolean
}

const props = withDefaults(defineProps<{
  userName: string
  navigation?: NavigationItem[]
}>(), {
  navigation: undefined,
})
const emit = defineEmits<{ logout: [] }>()
const defaultNavigation: NavigationItem[] = [
  { label: 'Hồ sơ học sinh', to: '/students', icon: 'pi pi-users' },
  { label: 'Thêm học sinh', to: '/students/new', icon: 'pi pi-user-plus' },
]
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/v2">
        <span class="brand-mark" aria-hidden="true">AC</span>
        <span>Academic Core</span>
      </RouterLink>
      <div class="header-actions">
        <span class="welcome">Xin chào, {{ userName }}</span>
        <Button label="Đăng xuất" icon="pi pi-sign-out" severity="secondary" text @click="emit('logout')" />
      </div>
    </header>
    <div class="app-body">
      <aside class="sidebar" aria-label="Điều hướng chính">
        <nav aria-label="Điều hướng chức năng">
          <slot name="navigation">
            <RouterLink
              v-for="item in props.navigation ?? defaultNavigation"
              :key="item.to"
              :to="item.to"
              :class="{ 'router-link-active': item.active }"
            >
              <i :class="item.icon" aria-hidden="true" />{{ item.label }}
            </RouterLink>
          </slot>
        </nav>
      </aside>
      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>
