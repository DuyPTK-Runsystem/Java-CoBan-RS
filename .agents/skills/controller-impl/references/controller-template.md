# Controller template

```java
package <base-package>.<module>.controller;

@RestController
@RequestMapping("/api/v1/<resources>")
@RequiredArgsConstructor
public class <Entity>Controller {
    private final <Entity>Service service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Res<Entity>DTO create(@Valid @RequestBody ReqCreate<Entity>DTO request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Res<Entity>DTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Page<Res<Entity>DTO> getAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.getAll(keyword, pageable);
    }

    @PutMapping("/{id}")
    public Res<Entity>DTO update(@PathVariable Long id,
            @Valid @RequestBody ReqUpdate<Entity>DTO request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

Add imports and adapt response wrapper, `@ApiMessage`, authorization, and delete policy to the repository. Use `Long id` for path identifiers by default. Use `Req<Action><Entity>DTO` and `Res<Entity>DTO` by default; use special names only when the contract is genuinely specialized. Never copy placeholders literally.
