# Frontend module example

```text
src/
├── components/
│   ├── StudentSearchForm.vue
│   ├── StudentTable.vue
│   └── StudentForm.vue
├── views/
│   ├── StudentListView.vue
│   └── StudentFormView.vue
├── services/
│   └── studentApi.ts
└── types/
    └── student.ts
```

Flow:

```text
StudentListView
├── StudentSearchForm
├── StudentTable
└── studentApi

StudentFormView
├── StudentForm
└── studentApi
```

Do not make child components fetch unrelated page data directly.
