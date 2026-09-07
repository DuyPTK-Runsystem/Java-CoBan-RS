import { setup } from '@storybook/vue3'
import type { Preview } from '@storybook/vue3'
import Aura from '@primevue/themes/aura'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'

import '../src/styles.css'
import 'primeicons/primeicons.css'

setup((app) => {
  app.use(PrimeVue, {
    theme: {
      preset: Aura,
      options: {
        prefix: 'p',
        darkModeSelector: 'none',
      },
    },
  })
  app.use(ConfirmationService)
})

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
}

export default preview
