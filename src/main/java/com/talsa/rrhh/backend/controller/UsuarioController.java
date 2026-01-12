package com.talsa.rrhh.backend.controller;

import com.talsa.rrhh.backend.dto.UsuarioInfoDTO;
import com.talsa.rrhh.backend.entity.Usuario;
import com.talsa.rrhh.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/info/{dni}")
    public ResponseEntity<UsuarioInfoDTO> obtenerInfoUsuario(@PathVariable String dni) {
        Usuario usuario = usuarioRepository.findByUsername(dni)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        return ResponseEntity.ok(new UsuarioInfoDTO(usuario.getNombre(), usuario.getApellidos()));
    }
}
