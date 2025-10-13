package com.project.enginuity.post.Exception;

import com.project.enginuity.profile.Exception.CustomException;

public class ReelUploadFailed extends CustomException {
    public ReelUploadFailed(String message, String eMessage) {
        super(message);
    }
}
