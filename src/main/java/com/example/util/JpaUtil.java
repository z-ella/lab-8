package com.example.util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public final class JpaUtil {
    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("reservationPU");

    private JpaUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }

    public static void close() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
