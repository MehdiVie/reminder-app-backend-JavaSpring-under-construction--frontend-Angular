package com.example.reminder.service;
import com.example.reminder.dto.RegisterRequest;
import com.example.reminder.exception.BadRequestException;
import com.example.reminder.model.Role;
import com.example.reminder.model.User;
import com.example.reminder.repository.RoleRepository;
import com.example.reminder.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
public class UserService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, RoleRepository roleRepo,@Lazy PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /*
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepo.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(
                                user.getUserRoles().stream()
                                        .map(role -> "ROLE_" + role.getRole().getName())
                                        .toArray(String[]::new)
                        )
                        .build()
                )
                .orElseThrow(() ->
                new UsernameNotFoundException("User not found with email: " + email)
                );
    }*/


    public User registerUser(RegisterRequest request) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Role role = roleRepo.findByName("USER")
                    .orElseGet(()-> {
                        Role newRole = new Role();
                        newRole.setName("USER");
                        return  roleRepo.save(newRole);
                    });
        // create User
        User user =new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // add Role
        user.addRole(role);

        return userRepo.save(user);
    }

}
