package com.project.enginuity.post.Exception;

import com.project.enginuity.profile.Exception.CustomException;

public class InvalidFileTypeException extends CustomException {
    public InvalidFileTypeException(String message) {
        super(message);
    }
}
