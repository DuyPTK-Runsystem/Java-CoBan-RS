<script setup lang="ts">
import Button from 'primevue/button'

export interface NavigationItem {
  label: string
  to: string
  icon: string
}

const props = withDefaults(defineProps<{
  userName: string
  navigation?: NavigationItem[]
}>(), {
  navigation: undefined,
})
const emit = defineEmits<{ logout: [] }>()
const defaultNavigation: NavigationItem[] = [
  { label: 'Students', to: '/students', icon: 'pi pi-users' },
  { label: 'Add student', to: '/students/new', icon: 'pi pi-user-plus' },
]
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="brand" to="/students">
        <span class="brand-mark" aria-hidden="true">AC</span>
        <span>Academic Core</span>
      </RouterLink>
      <div class="header-actions">
        <span class="welcome">Welcome, {{ userName }}</span>
        <Button label="Logout" icon="pi pi-sign-out" severity="secondary" text @click="emit('logout')" />
      </div>
    </header>
    <div class="app-body">
      <aside class="sidebar" aria-label="Main navigation">
        <nav aria-label="Workspace navigation">
          <slot name="navigation">
            <RouterLink
              v-for="item in props.navigation ?? defaultNavigation"
              :key="item.to"
              :to="item.to"
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
