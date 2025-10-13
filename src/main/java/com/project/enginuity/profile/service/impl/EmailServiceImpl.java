package com.project.enginuity.profile.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to,String otp){
        try{
            SimpleMailMessage message=new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your Otp Code");
            message.setText("Your otp is:"+otp+"\n It will expire in 5 minutes");
            mailSender.send(message);
        }catch (Exception e){
            throw new RuntimeException("Failed to send otp...",e);
        }
    }
}
