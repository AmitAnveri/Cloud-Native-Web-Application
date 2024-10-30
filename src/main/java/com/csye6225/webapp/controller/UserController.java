package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.UserRequestDto;
import com.csye6225.webapp.dto.UserResponseDto;
import com.csye6225.webapp.dto.UserUpdateRequestDto;
import com.csye6225.webapp.service.UserService;
import com.timgroup.statsd.StatsDClient;
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

    @Autowired
    private StatsDClient statsDClient;

    // Get Authenticated User Details
    @GetMapping("/self")
    public ResponseEntity<?> getUserDetails() {
        // Increment the counter for the getUserDetails API
        statsDClient.incrementCounter("api.user.getUserDetails.call_count");

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userEmail = userDetails.getUsername();
        UserResponseDto userResponseDto = userService.getUserByEmail(userEmail);
        return new ResponseEntity<>(userResponseDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        // Increment the counter for the createUser API
        statsDClient.incrementCounter("api.user.createUser.call_count");

        // Create the user
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // Return 201 Created status with user data
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        // Increment the counter for the updateUser API
        statsDClient.incrementCounter("api.user.updateUser.call_count");

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userEmail = userDetails.getUsername();

        if (!userEmail.equals(userUpdateRequestDto.getEmail())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        userService.updateUser(userEmail, userUpdateRequestDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/self", method = RequestMethod.HEAD)
    public ResponseEntity<?> handleHead() {
        // Increment the counter for the handleHead API
        statsDClient.incrementCounter("api.user.handleHead.call_count");

        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @RequestMapping(value = "/self", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        // Increment the counter for the handleOptions API
        statsDClient.incrementCounter("api.user.handleOptions.call_count");

        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsBase() {
        // Increment the counter for the handleOptionsBase API
        statsDClient.incrementCounter("api.user.handleOptionsBase.call_count");

        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

}
