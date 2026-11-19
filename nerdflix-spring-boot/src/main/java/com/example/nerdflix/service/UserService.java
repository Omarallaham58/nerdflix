package com.example.nerdflix.service;

import com.example.nerdflix.dto.mapper.UserMapper;
import com.example.nerdflix.dto.request.CreateUserRequest;
import com.example.nerdflix.dto.response.UserDto;
import com.example.nerdflix.entity.Role;
import com.example.nerdflix.entity.User;
import com.example.nerdflix.exception.UserAlreadyExistsException;
import com.example.nerdflix.exception.UserNotFoundException;
import com.example.nerdflix.repository.RoleRepository;
import com.example.nerdflix.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder bCryptPasswordEncoder;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder bCryptPasswordEncoder,
                       UserMapper userMapper, RoleRepository roleRepository){

        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
    }



    @Transactional
    public UserDto createUser(CreateUserRequest request){

        String username = request.getUsername();
        String password = request.getPassword();

        //empty fields should be handled on the gui level if exists
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password must not be empty");
        }

        //check if already exists
        if(userRepository.existsByUsername(username)){

            throw new UserAlreadyExistsException("User having username '" + username +
                    "' already exists in the database");
        }

        //otherwise: valid
        User user = userMapper.toEntity(request);
        user.setPassword(bCryptPasswordEncoder.encode(password));

        Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_CLIENT not found"));

        user.setRoles(Set.of(clientRole));

        // userRepository.save(user);
         return userMapper.toDto(userRepository.save(user));

    }


    @Transactional
    @PreAuthorize("#id == authentication.principal.userId or hasRole('ADMIN')")
    public UserDto updateUser(Integer id, CreateUserRequest updateRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        // Update username if provided and different
        if (updateRequest.getUsername() != null &&
                !updateRequest.getUsername().isBlank() &&
                !existingUser.getUsername().equals(updateRequest.getUsername())) {

            // Check if new username is already taken by ANOTHER user
            if (userRepository.existsByUsername(updateRequest.getUsername())) {
                throw new UserAlreadyExistsException("Username '" + updateRequest.getUsername() + "' is already taken");
            }
            existingUser.setUsername(updateRequest.getUsername());
        }

        // Update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isBlank()) {

            existingUser.setPassword(bCryptPasswordEncoder.encode(updateRequest.getPassword()));
        }

        return  userMapper.toDto(userRepository.save(existingUser));

    }


    @Transactional
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public void deleteUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers(){

        List<User> users =  userRepository.findAll();
        List<UserDto> dtos = new ArrayList<>();
        for(User u:users)
            dtos.add(userMapper.toDto(u));

        return dtos;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("#username == authentication.principal.username or hasRole('ADMIN')")
    public UserDto getUserByUsername(String username){

        return userMapper.toDto(userRepository.findByUsername(username).orElseThrow(() -> new
                UsernameNotFoundException("User with username '" + username + "' is not found" )));


    }

    @Transactional(readOnly = true)
    @PreAuthorize("#id == authentication.principal.userId or hasRole('ADMIN')")
    public UserDto getUserById(Integer id){
        return userMapper.toDto(userRepository.findById(id).
                orElseThrow(() -> new UsernameNotFoundException("User with id " + id + " is not found")));
    }

    //for auth purposes
    @Transactional(readOnly = true)
    public User getUserEntityForAuth(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }


    public boolean userExists(Integer id) {
        return userRepository.existsById(id);
    }



}
