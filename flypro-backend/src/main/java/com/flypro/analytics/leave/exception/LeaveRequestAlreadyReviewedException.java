package com.flypro.analytics.leave.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class LeaveRequestAlreadyReviewedException extends ApiException {

    public LeaveRequestAlreadyReviewedException(Long id) {
        super("Leave request " + id + " has already been reviewed", HttpStatus.CONFLICT, "LEAVE_REQUEST_ALREADY_REVIEWED");
    }
}
