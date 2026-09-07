# Service template

```java
package <base-package>.<module>.service;

@Service
@RequiredArgsConstructor
public class <Entity>Service {
    private final <Entity>Repository repository;

    @Transactional
    public Res<Entity>DTO create(ReqCreate<Entity>DTO request) {
        validateCreate(request);
        <Entity> entity = new <Entity>();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Res<Entity>DTO getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<Res<Entity>DTO> getAll(String keyword, Pageable pageable) {
        return repository.findAll(buildSpecification(keyword), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public Res<Entity>DTO update(Long id, ReqUpdate<Entity>DTO request) {
        <Entity> entity = findById(id);
        validateUpdate(entity, request);
        apply(entity, request);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        <Entity> entity = findById(id);
        validateDelete(entity);
        repository.delete(entity);
    }

    private <Entity> findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("<Entity> not found: " + id));
    }
}
```

Add imports and replace validation, mapping, specification, and exception placeholders with repository conventions. Use `Long id` for service identifiers by default. Default to `Req<Action><Entity>DTO` and `Res<Entity>DTO`; preserve additional qualifiers only for distinct business contracts.
