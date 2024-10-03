package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.UserRequestDto;
import com.csye6225.webapp.dto.UserResponseDto;
import com.csye6225.webapp.dto.UserUpdateRequestDto;
import com.csye6225.webapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    // Get Authenticated User Details
    @GetMapping("/self")
    public ResponseEntity<?> getUserDetails() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userEmail = userDetails.getUsername();
        UserResponseDto userResponseDto = userService.getUserByEmail(userEmail);
        return new ResponseEntity<>(userResponseDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequestDto userRequestDto) {
        // Create the user
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // Return 201 Created status with user data
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userEmail = userDetails.getUsername();
        userService.updateUser(userEmail, userUpdateRequestDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
