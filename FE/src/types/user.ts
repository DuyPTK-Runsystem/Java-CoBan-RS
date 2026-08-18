export interface LoginValues {
  userName: string
  password: string
}

export interface RegisterValues extends LoginValues {
  confirmPassword: string
}

export type FieldErrors<T extends string> = Partial<Record<T, string>>

export type LoginField = keyof LoginValues
export type RegisterField = keyof RegisterValues
