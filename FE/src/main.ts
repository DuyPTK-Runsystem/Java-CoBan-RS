import { vietnameseLocale } from './locales/vi'
import { createApp } from 'vue'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'

import App from './App.vue'
import router from './router'
import { syncAuthSession } from './services/authSession'
import './styles.css'
import 'primeicons/primeicons.css'

window.addEventListener('storage', (event) => {
  if (syncAuthSession(event)) window.location.reload()
})

const app = createApp(App)

app.use(router)
app.use(ConfirmationService)
app.use(PrimeVue, {
    locale: vietnameseLocale,
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: 'none',
    },
  },
})

app.mount('#app')
