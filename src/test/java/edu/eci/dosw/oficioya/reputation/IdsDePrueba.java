package edu.eci.dosw.oficioya.reputation;

import java.util.UUID;

/** Ids de prueba legibles: id(101) es 00000000-0000-0000-0000-000000000101, igual que en los datos de demo. */
public final class IdsDePrueba {

    private IdsDePrueba() {
    }

    public static UUID id(long numero) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(numero));
    }
}
