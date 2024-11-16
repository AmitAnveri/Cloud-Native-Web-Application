package com.csye6225.webapp.service;

import com.csye6225.webapp.model.SentEmail;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.repository.SentEmailRepository;
import com.csye6225.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EmailVerificationService {

    @Autowired
    private SentEmailRepository sentEmailRepository;

    @Autowired
    private UserRepository userRepository;

    public String verifyEmail(String token) {
        // Retrieve the email verification entry by token
        SentEmail sentEmail = sentEmailRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        // Check if the token is expired
        if (sentEmail.getSentAt().toInstant().plusSeconds(120).isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        // Mark the user as verified
        User user = userRepository.findByEmail(sentEmail.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        sentEmail.setStatus("VERIFIED");
        sentEmailRepository.save(sentEmail);

        return "Email successfully verified!";
    }
}
