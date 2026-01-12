package com.talsa.rrhh.backend.enums;

public enum EstadoTicket {
    PENDIENTE,      // Recién creado por el trabajador en la tablet
    EN_ATENCION,    // Un personal de RRHH lo está atendiendo
    FINALIZADO,      // Atención concluida
    AUSENTE,        // El trabajador no se presentó cuando fue llamado
    CANCELADO       // Cancelado por el trabajador o RRHH
}
