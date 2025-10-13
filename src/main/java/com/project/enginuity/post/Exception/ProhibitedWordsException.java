package com.project.enginuity.post.Exception;

import com.project.enginuity.profile.Exception.CustomException;

public class ProhibitedWordsException extends CustomException {
    public ProhibitedWordsException(String message) {
        super(message);
    }
}
