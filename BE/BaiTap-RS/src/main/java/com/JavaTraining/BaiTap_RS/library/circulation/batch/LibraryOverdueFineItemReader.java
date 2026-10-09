package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.data.domain.PageRequest;

/** Restartable paged reader whose page and offset are checkpointed with each committed chunk. */
public class LibraryOverdueFineItemReader implements ItemStreamReader<Long> {

    private static final String PAGE_KEY = "libraryOverdueFineReader.page";
    private static final String INDEX_KEY = "libraryOverdueFineReader.index";
    private static final int PAGE_SIZE = 100;

    private final LibraryLoanRepository loanRepository;
    private final LocalDate runDate;
    private int pageNumber;
    private int itemIndex;
    private List<Long> pageItems = List.of();

    public LibraryOverdueFineItemReader(LibraryLoanRepository loanRepository, LocalDate runDate) {
        this.loanRepository = loanRepository;
        this.runDate = runDate;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        pageNumber = executionContext.getInt(PAGE_KEY, 0);
        itemIndex = executionContext.getInt(INDEX_KEY, 0);
        pageItems = List.of();
    }

    @Override
    public Long read() {
        while (true) {
            if (itemIndex < pageItems.size()) {
                return pageItems.get(itemIndex++);
            }
            if (!pageItems.isEmpty()) {
                pageNumber++;
                itemIndex = 0;
            }
            pageItems = loanRepository.findOverdue(LoanStatus.ACTIVE,
                            LocalDateTime.of(runDate, LocalTime.MAX), PageRequest.of(pageNumber, PAGE_SIZE))
                    .map(LibraryLoan::getId).getContent();
            if (pageItems.isEmpty()) {
                return null;
            }
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(PAGE_KEY, pageNumber);
        executionContext.putInt(INDEX_KEY, itemIndex);
    }
}
