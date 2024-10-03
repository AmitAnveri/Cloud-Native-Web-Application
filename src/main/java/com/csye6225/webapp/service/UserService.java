package com.csye6225.webapp.service;

import com.csye6225.webapp.dto.UserRequestDto;
import com.csye6225.webapp.dto.UserResponseDto;
import com.csye6225.webapp.dto.UserUpdateRequestDto;
import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.exception.UserNotFoundException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        // Create a new user entity
        User user = new User();
        user.setEmail(userRequestDto.getEmail());
        user.setFirstName(userRequestDto.getFirstName());
        user.setLastName(userRequestDto.getLastName());
        // Hash the password using BCrypt
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        // Save the user to the database
        userRepository.save(user);

        return mapToUserResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
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
        User user = userRepository.findByEmail(email);
        user.setFirstName(userUpdateRequestDto.getFirstName());
        user.setLastName(userUpdateRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(userUpdateRequestDto.getPassword()));
        userRepository.save(user);
    }
}
