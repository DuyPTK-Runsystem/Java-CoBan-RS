import { createApp } from 'vue'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'

import App from './App.vue'
import router from './router'
import './styles.css'
import 'primeicons/primeicons.css'

const app = createApp(App)

app.use(router)
app.use(ConfirmationService)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: 'none',
    },
  },
})

app.mount('#app')
