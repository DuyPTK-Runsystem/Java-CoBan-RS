export interface LibraryFineTier {
  throughDay: number | null
  dailyRate: number
}

export interface LibraryCirculationPolicy {
  policyVersion: number
  effectiveAt: string
  maxActiveLoans: number
  loanDurationDays: number
  maxRenewals: number
  renewalDurationDays: number
  reservationPickupDays: number
  fineTiers: LibraryFineTier[]
  fineCapPerLoan: number
  fineSuspensionThreshold: number
  updatedAt: string
  updatedBy: number
}

export type UpdateLibraryCirculationPolicyRequest = Omit<LibraryCirculationPolicy, 'updatedAt' | 'updatedBy'> & {
  expectedVersion: number
}
