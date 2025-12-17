package com.prepxl.streaming.service;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;

public class ResponseHandler {
    public static String getText(GenerateContentResponse response) {
        StringBuilder sb = new StringBuilder();
        if (response.getCandidatesCount() > 0) {
            for (Part part : response.getCandidates(0).getContent().getPartsList()) {
                if (part.hasText()) {
                    sb.append(part.getText());
                }
            }
        }
        return sb.toString();
    }
}
