package com.example.ndbx.service;

import com.example.ndbx.model.User;
import com.example.ndbx.repository.UserRepository;
import com.example.ndbx.util.Constants;
import com.example.ndbx.util.OffsetScrollRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public User registerUser(String fullName, String username, String password) {
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        return userRepository.save(user);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Page<User> searchUsers(String name, int limit, int offset) {
        Pageable pageRequest = new OffsetScrollRequest(offset, limit);
        if (name != null && !name.trim().isEmpty()) {
            return userRepository.findByFullNameLike(name, pageRequest);
        } else {
            return userRepository.findAll(pageRequest);
        }
    }

    public Map<String, Object> userToMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(Constants.FLD_ID, u.getId());
        map.put(Constants.FLD_FULL_NAME, u.getFullName());
        map.put(Constants.FLD_USERNAME, u.getUsername());
        return map;
    }
}
