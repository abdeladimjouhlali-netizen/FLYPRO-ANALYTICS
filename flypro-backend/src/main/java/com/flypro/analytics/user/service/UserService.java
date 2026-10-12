package com.flypro.analytics.user.service;

import com.flypro.analytics.common.enums.RoleName;
import com.flypro.analytics.common.exceptions.EntityNotFoundException;
import com.flypro.analytics.common.pagination.PageResponse;
import com.flypro.analytics.role.entity.Role;
import com.flypro.analytics.role.repository.RoleRepository;
import com.flypro.analytics.user.dto.CreateUserRequest;
import com.flypro.analytics.user.dto.UserResponse;
import com.flypro.analytics.user.entity.User;
import com.flypro.analytics.user.exception.InvalidUserRoleException;
import com.flypro.analytics.user.exception.UserAlreadyExistsException;
import com.flypro.analytics.user.mapper.UserMapper;
import com.flypro.analytics.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des utilisateurs console — création Admin/Analyste réservée au Super Admin.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public UserResponse getResponseByUsername(String username) {
        return userMapper.toResponse(getByUsername(username));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    /**
     * Super Admin uniquement (enforced au controller) :
     * peut créer ROLE_ADMIN ou ROLE_ANALYST — jamais SUPER_ADMIN / EMPLOYEE ici.
     */
    @Transactional
    public UserResponse createBySuperAdmin(CreateUserRequest request) {
        RoleName roleName = request.role();
        if (roleName != RoleName.ROLE_ADMIN && roleName != RoleName.ROLE_ANALYST) {
            throw new InvalidUserRoleException(
                    "Seul ROLE_ADMIN ou ROLE_ANALYST peut être créé. "
                            + "Le Super Admin est unique ; les employés sont créés via le module Employés."
            );
        }

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists: " + email);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .accountLocked(false)
                .role(role)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        if (user.getRole().getName() == RoleName.ROLE_SUPER_ADMIN && !enabled) {
            throw new InvalidUserRoleException("Le compte Super Admin ne peut pas être désactivé");
        }
        user.setEnabled(enabled);
        return userMapper.toResponse(userRepository.save(user));
    }
}
