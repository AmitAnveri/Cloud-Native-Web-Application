package com.csye6225.webapp.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserService {

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

    public ProfilePicResponseDto uploadProfilePic(String userEmail, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail);

        String fileName = file.getOriginalFilename();
        String key = "profile-pictures/" + user.getId() + "/" + fileName;
        String uniqueId = UUID.randomUUID().toString();


        amazonS3.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), null)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        user.setProfilePicUrl(key);
        userRepository.save(user);

        return new ProfilePicResponseDto(
                fileName,
                uniqueId,
                amazonS3.getUrl(bucketName, key).toString(),
                LocalDate.now(),
                user.getId().toString()
        );
    }

    public void deleteProfilePic(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        String key = user.getProfilePicUrl();

        if (key != null && amazonS3.doesObjectExist(bucketName, key)) {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        }

        user.setProfilePicUrl(null);
        userRepository.save(user);
    }
}
