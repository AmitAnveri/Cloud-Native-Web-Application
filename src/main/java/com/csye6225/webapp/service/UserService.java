package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.UserRequestDto;
import com.csye6225.webapp.dto.UserResponseDto;
import com.csye6225.webapp.dto.UserUpdateRequestDto;
import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StatsDClient statsDClient;

    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        // Start timer for checking if user exists
        long startExists = System.currentTimeMillis();

        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            // Record execution time for existsByEmail
            statsDClient.recordExecutionTime("db.userRepository.existsByEmail.time", System.currentTimeMillis() - startExists);
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        // Create a new user entity
        User user = new User();
        user.setEmail(userRequestDto.getEmail());
        user.setFirstName(userRequestDto.getFirstName());
        user.setLastName(userRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        // Start timer for saving user
        long startSave = System.currentTimeMillis();

        userRepository.save(user);

        // Record execution time for save
        statsDClient.recordExecutionTime("db.userRepository.save.time", System.currentTimeMillis() - startSave);

        return mapToUserResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        // Start timer for retrieving user by email
        long start = System.currentTimeMillis();

        User user = userRepository.findByEmail(email);

        // Record execution time for findByEmail
        long duration = System.currentTimeMillis() - start;
        statsDClient.recordExecutionTime("db.userRepository.findByEmail.time", duration);

        return mapToUserResponseDto(user);
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setId(user.getId());
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setFirstName(user.getFirstName());
        userResponseDto.setLastName(user.getLastName());
        userResponseDto.setAccountCreated(user.getAccountCreated().toString());
        userResponseDto.setAccountUpdated(user.getAccountUpdated().toString());

        return userResponseDto;
    }

    public void updateUser(String email, UserUpdateRequestDto userUpdateRequestDto) {
        // Start timer for retrieving user by email
        long startFind = System.currentTimeMillis();

        User user = userRepository.findByEmail(email);

        // Record execution time for findByEmail
        statsDClient.recordExecutionTime("db.userRepository.findByEmail.time", System.currentTimeMillis() - startFind);

        user.setFirstName(userUpdateRequestDto.getFirstName());
        user.setLastName(userUpdateRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(userUpdateRequestDto.getPassword()));

        // Start timer for saving updated user
        long startSave = System.currentTimeMillis();

        userRepository.save(user);

        // Record execution time for save
        statsDClient.recordExecutionTime("db.userRepository.save.time", System.currentTimeMillis() - startSave);
    }
}
