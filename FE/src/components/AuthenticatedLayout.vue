<script setup lang="ts">
import Button from 'primevue/button'

defineProps<{ userName: string }>()
const emit = defineEmits<{ logout: [] }>()
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
        <nav>
          <RouterLink to="/students"><i class="pi pi-users" aria-hidden="true" />Students</RouterLink>
          <RouterLink to="/students/new"><i class="pi pi-user-plus" aria-hidden="true" />Add student</RouterLink>
        </nav>
      </aside>
      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>
