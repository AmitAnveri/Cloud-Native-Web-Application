package com.csye6225.webapp.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.csye6225.webapp.dto.ProfilePicResponseDto;
import com.csye6225.webapp.dto.UserRequestDto;
import com.csye6225.webapp.dto.UserResponseDto;
import com.csye6225.webapp.dto.UserUpdateRequestDto;
import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StatsDClient statsDClient;

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        long startExists = System.currentTimeMillis();
        logger.info("Attempting to create user with email: {}", userRequestDto.getEmail());

        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            logger.warn("User with email {} already exists", userRequestDto.getEmail());
            statsDClient.recordExecutionTime("db.userRepository.existsByEmail.time", System.currentTimeMillis() - startExists);
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        User user = new User();
        user.setEmail(userRequestDto.getEmail());
        user.setFirstName(userRequestDto.getFirstName());
        user.setLastName(userRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        long startSave = System.currentTimeMillis();
        userRepository.save(user);
        statsDClient.recordExecutionTime("db.userRepository.save.time", System.currentTimeMillis() - startSave);
        logger.info("User with email {} created successfully", userRequestDto.getEmail());

        return mapToUserResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        long start = System.currentTimeMillis();
        logger.info("Fetching user with email: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            logger.warn("User with email {} not found", email);
        }

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
        long startFind = System.currentTimeMillis();
        logger.info("Updating user with email: {}", email);

        User user = userRepository.findByEmail(email);
        statsDClient.recordExecutionTime("db.userRepository.findByEmail.time", System.currentTimeMillis() - startFind);

        user.setFirstName(userUpdateRequestDto.getFirstName());
        user.setLastName(userUpdateRequestDto.getLastName());
        user.setPassword(passwordEncoder.encode(userUpdateRequestDto.getPassword()));

        long startSave = System.currentTimeMillis();
        userRepository.save(user);
        statsDClient.recordExecutionTime("db.userRepository.save.time", System.currentTimeMillis() - startSave);
        logger.info("User with email {} updated successfully", email);
    }

    public ResponseEntity<ProfilePicResponseDto> uploadProfilePic(String userEmail, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail);

        // Check if the user already has an image uploaded
        if (user.getProfilePicUrl() != null) {
            logger.warn("User with email {} already has a profile picture", userEmail);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        logger.info("Uploading profile picture for user with email: {}", userEmail);

        String fileName = file.getOriginalFilename();
        String key = "profile-pictures/" + user.getId() + "/" + fileName;
        String uniqueId = UUID.randomUUID().toString();

        amazonS3.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), null));

        user.setProfilePicUrl(key);
        userRepository.save(user);

        logger.info("Profile picture uploaded successfully for user: {}", userEmail);

        ProfilePicResponseDto responseDto = new ProfilePicResponseDto(
                fileName,
                uniqueId,
                amazonS3.getUrl(bucketName, key).toString(),
                LocalDate.now(),
                user.getId().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    public ResponseEntity<?> deleteProfilePic(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        String key = user.getProfilePicUrl();

        if (key == null || !amazonS3.doesObjectExist(bucketName, key)) {
            logger.warn("No profile picture found for user with email: {}", userEmail);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        logger.info("Deleting profile picture for user with email: {}", userEmail);
        user.setProfilePicUrl(null);
        userRepository.save(user);
        amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        logger.info("Profile picture deleted successfully for user: {}", userEmail);

        return ResponseEntity.noContent().build();
    }
}
