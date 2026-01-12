package com.talsa.rrhh.backend.repository;

import com.talsa.rrhh.backend.entity.Usuario;
import com.talsa.rrhh.backend.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 1. Método esencial para el Login (Spring Security lo usa)
    // Busca al usuario por su DNI o user de red
    Optional<Usuario> findByUsername(String username);

    // 2. Validación: Verifica si el usuario existe antes de crearlo
    // Devuelve true si ya hay alguien con ese username
    boolean existsByUsername(String username);

    // 3. Filtro por Roles
    // Útil para poblar listas desplegables (Ej: "Seleccionar Atendedor")
    // Te permitirá listar solo a PERSONAL_RRHH y excluir a los ADMINS
    List<Usuario> findByRol(Rol rol);
}
