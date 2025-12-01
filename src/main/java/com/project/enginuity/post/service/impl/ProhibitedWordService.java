package com.project.enginuity.post.service.impl;


import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class ProhibitedWordService {

    private final Set<String> prohibitedWords = new HashSet<>();

    public ProhibitedWordService(){
        try(BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/prohibited-terms.txt"))))) {
            String line;
            while ((line = br.readLine()) != null) {
                prohibitedWords.add(line.trim().toLowerCase());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prohibited words", e);

        }
    }
    public boolean containsProhibitedWord(String text){
        if(text==null || text.isEmpty()){
            return false;
        }
        String lowerCaseText = text.toLowerCase();
        return prohibitedWords.stream().anyMatch(lowerCaseText::contains);
    }

}
