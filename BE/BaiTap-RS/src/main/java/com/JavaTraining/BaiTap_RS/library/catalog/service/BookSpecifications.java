package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.Locale;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> catalog(String keyword, String category, Integer publishedYear,
            Boolean available) {
        return (root, query, builder) -> {
            Predicate predicate = builder.isNull(root.get("archivedAt"));
            if (keyword != null && !keyword.isBlank()) {
                String normalized = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("title")), normalized, '\\'),
                        builder.like(builder.lower(root.get("author")), normalized, '\\'),
                        builder.like(builder.lower(root.get("isbn")), normalized, '\\')));
            }
            if (category != null && !category.isBlank()) {
                predicate = builder.and(predicate,
                        builder.equal(builder.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT)));
            }
            if (publishedYear != null) {
                predicate = builder.and(predicate, builder.equal(root.get("publishedYear"), publishedYear));
            }
            if (available != null) {
                Subquery<Long> copies = query.subquery(Long.class);
                Root<BookCopy> copy = copies.from(BookCopy.class);
                copies.select(copy.get("id")).where(
                        builder.equal(copy.get("book"), root),
                        builder.equal(copy.get("status"), BookCopyStatus.AVAILABLE),
                        builder.isFalse(copy.get("referenceOnly")));
                Predicate availabilityPredicate = available
                        ? builder.exists(copies)
                        : builder.not(builder.exists(copies));
                predicate = builder.and(predicate, availabilityPredicate);
            }
            return predicate;
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
