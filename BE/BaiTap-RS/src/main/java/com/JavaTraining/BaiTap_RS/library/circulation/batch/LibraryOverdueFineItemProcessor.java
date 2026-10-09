package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.ItemProcessor;

public class LibraryOverdueFineItemProcessor implements ItemProcessor<Long, Long> {

    private final LibraryOverdueFineItemService itemService;
    private final LocalDate runDate;
    private final Long actorUserId;

    public LibraryOverdueFineItemProcessor(LibraryOverdueFineItemService itemService, LocalDate runDate,
            Long actorUserId) {
        this.itemService = itemService;
        this.runDate = runDate;
        this.actorUserId = actorUserId;
    }

    @Override
    public Long process(Long loanId) {
        try {
            itemService.process(loanId, runDate, actorUserId);
            return loanId;
        } catch (com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException
                | org.springframework.dao.DataAccessException exception) {
            throw new LibraryOverdueFineItemException(loanId, exception);
        }
    }
}
