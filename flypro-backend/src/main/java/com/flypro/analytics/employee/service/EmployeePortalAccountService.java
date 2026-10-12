package com.flypro.analytics.employee.service;

import com.flypro.analytics.common.enums.RoleName;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.role.entity.Role;
import com.flypro.analytics.role.repository.RoleRepository;
import com.flypro.analytics.user.entity.User;
import com.flypro.analytics.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée automatiquement un compte portail {@code ROLE_EMPLOYEE} lors de la création d'un employé.
 */
@Service
public class EmployeePortalAccountService {

    public static final String DEFAULT_TEMPORARY_PASSWORD = "Employee@123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeePortalAccountService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record PortalCredentials(String username, String temporaryPassword) {
    }

    @Transactional
    public PortalCredentials createPortalAccount(Employee employee) {
        if (employee.getId() != null && userRepository.existsByEmployeeId(employee.getId())) {
            return userRepository.findByEmployeeId(employee.getId())
                    .map(u -> new PortalCredentials(u.getUsername(), null))
                    .orElseThrow();
        }

        Role employeeRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("ROLE_EMPLOYEE manquant en base (seed V2)"));

        String username = resolveUsername(employee);
        String password = DEFAULT_TEMPORARY_PASSWORD;

        User user = User.builder()
                .username(username)
                .email(employee.getEmail())
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .accountLocked(false)
                .role(employeeRole)
                .employeeId(employee.getId())
                .build();

        userRepository.save(user);
        return new PortalCredentials(username, password);
    }

    @Transactional
    public void syncEnabledFlag(Long employeeId, boolean enabled) {
        userRepository.findByEmployeeId(employeeId).ifPresent(user -> {
            user.setEnabled(enabled);
            userRepository.save(user);
        });
    }

    private String resolveUsername(Employee employee) {
        String base = employee.getEmployeeNumber() == null
                ? "emp"
                : employee.getEmployeeNumber().trim().toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "emp";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int i = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + i;
            i++;
            if (i > 9999) {
                candidate = base + System.currentTimeMillis();
                break;
            }
        }
        if (userRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalStateException(
                    "Un compte existe déjà avec l'email " + employee.getEmail()
                            + ". Impossible de provisionner le portail automatiquement."
            );
        }
        return candidate;
    }
}
