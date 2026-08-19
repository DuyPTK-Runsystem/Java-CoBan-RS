---
name: dev-trace-logging
description: Add or revise developer trace logs in Java/Spring code when request and execution context must be visible. Do not use for application audit logging or observability-platform configuration.
---

# Dev Trace Logging

Use this skill whenever adding or changing a trace log written by a developer or agent. Preserve existing project logging conventions unless they conflict with the requirements below.

## Required log shape

Every developer trace log must:

- Start with `>>>`.
- Identify the owning module immediately after the prefix. Use the feature or bounded-context name, such as `User`, `Student`, or `BatchCsvExport`.
- Include operation-specific detail only when it helps diagnosis; never include secrets, credentials, access tokens, passwords, or unnecessary personal data.
- Include the current thread name and HTTP request ID at the end, in this exact order: `[threadName] [HttpRequestId]`.

Preferred rendered form:

```text
>>>User (creating account for email hash=...): request accepted [http-nio-8080-exec-3] [7dc...]
```

Use parameterized SLF4J logging, not string concatenation:

```java
log.debug(">>>User (creating account): request accepted [{}] [{}]",
        Thread.currentThread().getName(),
        MDC.get("requestId"));
```

Use an appropriate level; `debug` is normally suitable. Do not add noisy per-item logs inside loops unless diagnosis explicitly requires them.

## HTTP request ID

Read the HTTP request ID from MDC key `requestId`. If the application does not already populate it, add one `OncePerRequestFilter` that generates a UUID, calls `MDC.put("requestId", requestId)` before the filter chain, and always calls `MDC.remove("requestId")` in `finally`.

- Reuse an existing request-ID filter or MDC key when present; do not create a second mechanism.
- For execution outside an HTTP request, render a clear fallback such as `N/A` in the request-ID slot.
