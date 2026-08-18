# Component patterns

## Form boundary

```text
LoginView
  -> LoginForm(props/state)
  <- submit(credentials)
```

`LoginForm` renders and validates UI input; the view coordinates the API call and navigation.

## Table boundary

```text
StudentListView
  -> StudentTable(rows, loading, pagination, sort)
  <- page-change
  <- sort-change
  <- edit
  <- delete
```

For server-side lists, the table emits query-state changes; the view reloads data through the service.
