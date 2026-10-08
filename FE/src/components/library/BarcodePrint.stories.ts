import type { Meta, StoryObj } from '@storybook/vue3'

import type { BookCopy } from '@/types/library/catalog'
import BarcodePrint from './BarcodePrint.vue'

function makeCopies(count: number): BookCopy[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    bookId: 101,
    barcode: `LIB-${String(i + 1).padStart(9, '0')}`,
    shelfLocation: `A-03-05-0${(i % 9) + 1}`,
    status: 'AVAILABLE' as const,
    referenceOnly: false,
    version: 0,
  }))
}

const meta = {
  title: 'Library/BarcodePrint',
  component: BarcodePrint,
  tags: ['autodocs'],
  args: {
    visible: true,
    bookTitle: 'Lập trình Java chuyên sâu',
    copies: makeCopies(50),
  },
} satisfies Meta<typeof BarcodePrint>

export default meta
type Story = StoryObj<typeof meta>

export const SinglePage50Copies: Story = {}

export const MultiPage80Copies: Story = {
  args: {
    copies: makeCopies(80),
  },
}

export const LongBookTitle: Story = {
  args: {
    bookTitle: 'Thiên thần và ác quỷ - Tiểu thuyết trinh thám Dan Brown',
    copies: makeCopies(50),
  },
}

export const EmptySelection: Story = {
  args: {
    copies: [],
  },
}

