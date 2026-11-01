package com.miapp.auth.domain.model;

/**
 * Estado operativo de una cuenta de acceso.
 */
public enum AccountStatus {
    /** La cuenta puede utilizarse cuando exista el caso de uso de acceso. */
    ACTIVE,
    /** La cuenta está temporalmente bloqueada. */
    LOCKED,
    /** La cuenta fue deshabilitada permanentemente. */
    DISABLED
}
