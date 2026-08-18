# View flows

## Login

```text
LoginView
-> LoginForm
-> userApi.login
-> success: StudentListView
-> failure: render meaningful error
```

## Student list

```text
StudentListView
-> search/sort/page state
-> studentApi.getStudents(query)
-> StudentTable
```

Delete:

```text
click Delete
-> confirmation
-> studentApi.deleteStudent(id)
-> reload current page
```
