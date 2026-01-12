package com.talsa.rrhh.backend.config;

import com.talsa.rrhh.backend.entity.Usuario;
import com.talsa.rrhh.backend.enums.Rol;
import com.talsa.rrhh.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // --- CREACIÓN DE USUARIOS BASE ---

        // 1. Admin TI (Tú)
        crearUsuario("71220236", "talsa123", "Valentin", "Admin TI", Rol.ADMIN_TI);

        // 2. Jefe RRHH
        crearUsuario("10101010", "jefe123", "Armando", "Jefe", Rol.JEFE_RRHH);

        // 3. Personal RRHH
        crearUsuario("20202020", "rrhh123", "Robinson", "Analista", Rol.PERSONAL_RRHH);
        crearUsuario("30303030", "rrhh123", "Yesenia", "Asistente", Rol.PERSONAL_RRHH);

        // 4. Trabajadores
        crearUsuario("40404040", "work123", "Juan", "TEST1", Rol.TRABAJADOR);
        crearUsuario("50505050", "work123", "Mario", "TEST2", Rol.TRABAJADOR);
        crearUsuario("60606060", "work123", "Pepe", "TEST3", Rol.TRABAJADOR);

        System.out.println("Usuarios iniciales cargados correctamente. Base de datos de tickets vacía.");
    }

    private void crearUsuario(String username, String rawPassword, String nombre, String apellido, Rol rol) {
        if (!usuarioRepository.existsByUsername(username)) {
            Usuario nuevo = Usuario.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .nombre(nombre)
                    .apellidos(apellido)
                    .rol(rol)
                    .build();
            usuarioRepository.save(nuevo);
            System.out.println("Usuario creado: " + username + " (" + rol + ")");
        }
    }
}
