package com.veda.recommendation.service;

import com.veda.recommendation.dto.CreateUserRequest;
import com.veda.recommendation.entity.User;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.exception.ResourceNotFoundException;
import com.veda.recommendation.repository.UserFeatureRepository;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserFeatureRepository userFeatureRepository;

    public UserService(UserRepository userRepository, UserFeatureRepository userFeatureRepository) {
        this.userRepository = userRepository;
        this.userFeatureRepository = userFeatureRepository;
    }

    @Transactional
    public User create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setAgeGroup(request.ageGroup());
        user.setCountry(request.country());
        User saved = userRepository.save(user);

        UserFeature feature = new UserFeature();
        feature.setUserId(saved.getId());
        userFeatureRepository.save(feature);
        return saved;
    }

    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
