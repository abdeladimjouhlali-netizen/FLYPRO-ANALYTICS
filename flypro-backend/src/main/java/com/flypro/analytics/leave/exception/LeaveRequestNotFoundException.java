package com.flypro.analytics.leave.exception;

import com.flypro.analytics.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class LeaveRequestNotFoundException extends ApiException {

    public LeaveRequestNotFoundException(Long id) {
        super("Leave request not found with id: " + id, HttpStatus.NOT_FOUND, "LEAVE_REQUEST_NOT_FOUND");
    }
}
