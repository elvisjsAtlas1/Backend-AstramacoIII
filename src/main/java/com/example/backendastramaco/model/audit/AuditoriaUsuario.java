package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_usuarios")
@Data
public class AuditoriaUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;           // ID del usuario afectado
    private String accion;            // CREATE, UPDATE, DELETE
    private String username;          // Usuario que realizó la acción
    private String usernameAnterior;  // Para UPDATE
    private String usernameNuevo;
    private String rolAnterior;
    private String rolNuevo;
    private String passwordAnterior;   // No guardar realmente, solo indicar si cambió
    private String passwordCambiada;   // "SI" o "NO"
    private LocalDateTime fechaHora;
    private String ipAddress;

    @Column(length = 5000)
    private String datosCompletosAnteriores; // JSON
    @Column(length = 5000)
    private String datosCompletosNuevos;     // JSON
}