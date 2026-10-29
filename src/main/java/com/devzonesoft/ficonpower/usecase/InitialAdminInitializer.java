package com.devzonesoft.ficonpower.usecase;

import com.devzonesoft.ficonpower.config.InitialAdminProperties;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties properties;

    public InitialAdminInitializer(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            InitialAdminProperties properties
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.warn("Initial administrator was not created: ADMIN_NAME, ADMIN_EMAIL and "
                    + "ADMIN_PASSWORD must all be configured");
            return;
        }

        String email = properties.normalizedEmail();
        if (users.findByUsername(email).isPresent()) {
            return;
        }

        users.create(
                email,
                passwordEncoder.encode(properties.password()),
                UserRole.SUPERADMIN,
                properties.normalizedName(),
                email);
        log.info("Initial SUPERADMIN account created from environment configuration");
    }
}
