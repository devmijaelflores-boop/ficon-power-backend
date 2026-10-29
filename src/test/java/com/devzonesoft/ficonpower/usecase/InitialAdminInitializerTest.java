package com.devzonesoft.ficonpower.usecase;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devzonesoft.ficonpower.config.InitialAdminProperties;
import com.devzonesoft.ficonpower.domain.UserRole;
import com.devzonesoft.ficonpower.ports.output.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class InitialAdminInitializerTest {

    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);

    @Test
    void createsTheOnlyInitialAccountFromEnvironmentConfiguration() throws Exception {
        InitialAdminProperties properties = new InitialAdminProperties(
                " Administrador FICON ", " SOPORTE@FICON.PE ", "clave-segura");
        when(users.findByUsername("soporte@ficon.pe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave-segura")).thenReturn("{bcrypt}hash");

        new InitialAdminInitializer(users, passwordEncoder, properties).run(null);

        verify(users).create(
                "soporte@ficon.pe",
                "{bcrypt}hash",
                UserRole.SUPERADMIN,
                "Administrador FICON",
                "soporte@ficon.pe");
    }

    @Test
    void doesNothingWhenEnvironmentConfigurationIsIncomplete() throws Exception {
        InitialAdminProperties properties = new InitialAdminProperties(
                "Administrador", "", "clave-segura");

        new InitialAdminInitializer(users, passwordEncoder, properties).run(null);

        verify(users, never()).create(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
