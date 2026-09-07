# API service pattern

```ts
export interface StudentQuery {
  page: number
  size: number
  studentCode?: string
  studentName?: string
  birthday?: string
  sort?: string
}

export async function getStudents(query: StudentQuery) {
  // Build URL/search params.
  // Call the backend.
  // Validate HTTP success.
  // Return the typed API result.
}
```

The service owns transport details. The view owns loading, notifications, navigation, and rendering.
