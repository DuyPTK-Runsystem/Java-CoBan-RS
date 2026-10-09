package com.JavaTraining.BaiTap_RS.library.circulation.batch;

public class LibraryOverdueFineItemException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final Long loanId;

    public LibraryOverdueFineItemException(Long loanId, RuntimeException cause) {
        super("Overdue fine processing failed for loan " + loanId, cause);
        this.loanId = loanId;
    }

    public Long getLoanId() {
        return loanId;
    }
}
