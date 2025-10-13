package com.project.enginuity.post.Exception;

import com.project.enginuity.profile.Exception.CustomException;

public class FileSizeLimitExceededException extends CustomException {
    public FileSizeLimitExceededException(String message) {
        super(message);
    }
}
