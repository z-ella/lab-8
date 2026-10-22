package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationServiceImpl;
import com.example.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.time.LocalDateTime;

public final class ConcurrentReservationSimulator {
    private ConcurrentReservationSimulator() {
    }

    public static void main(String[] args) {
        ReservationServiceImpl service = new ReservationServiceImpl();
        try {
            Reservation saved = service.creerReservation(new Reservation("Salle A", "Alice",
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1)));
            EntityManager first = JpaUtil.getEntityManagerFactory().createEntityManager();
            EntityManager second = JpaUtil.getEntityManagerFactory().createEntityManager();
            try {
                Reservation firstCopy = first.find(Reservation.class, saved.getId());
                Reservation secondCopy = second.find(Reservation.class, saved.getId());
                EntityTransaction firstTx = first.getTransaction();
                firstTx.begin();
                firstCopy.setReserveur("Alice (confirme)");
                firstTx.commit();

                EntityTransaction secondTx = second.getTransaction();
                secondTx.begin();
                secondCopy.setReserveur("Bob");
                try {
                    secondTx.commit();
                    throw new IllegalStateException("Le conflit optimiste attendu n'a pas ete detecte.");
                } catch (RollbackException | OptimisticLockException conflict) {
                    if (secondTx.isActive()) {
                        secondTx.rollback();
                    }
                    System.out.println("Conflit detecte: " + conflict.getClass().getSimpleName());
                    Reservation retry = new Reservation("Salle A", "Bob",
                            firstCopy.getDateDebut(), firstCopy.getDateFin());
                    Reservation updated = service.modifierAvecRetry(saved.getId(), retry, 3);
                    System.out.println("Mise a jour reessayee, version=" + updated.getVersion());
                }
            } finally {
                first.close();
                second.close();
            }
        } finally {
            JpaUtil.close();
        }
    }

}
