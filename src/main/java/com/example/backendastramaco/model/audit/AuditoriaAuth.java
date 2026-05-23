package com.example.backendastramaco.model.audit;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_auth")
@Data
public class AuditoriaAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String accion;           // LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, TOKEN_REFRESH
    private String ipAddress;
    private String userAgent;
    private String mensajeError;     // Si falló el login
    private LocalDateTime fechaHora;
    private Boolean exito;
}