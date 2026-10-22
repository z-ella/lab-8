package com.example;

import com.example.model.Reservation;
import com.example.service.ReservationServiceImpl;
import com.example.util.JpaUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReservationLockingTest {
    private final ReservationServiceImpl service = new ReservationServiceImpl();

    @Before
    public void clearReservations() {
        EntityManager em = JpaUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM Reservation").executeUpdate();
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    @AfterClass
    public static void closeFactory() {
        JpaUtil.close();
    }

    @Test
    public void concurrentUpdatesRejectStaleVersion() {
        Reservation created = service.creerReservation(newReservation("Alice"));
        EntityManager first = JpaUtil.getEntityManagerFactory().createEntityManager();
        EntityManager second = JpaUtil.getEntityManagerFactory().createEntityManager();
        try {
            Reservation firstCopy = first.find(Reservation.class, created.getId());
            Reservation staleCopy = second.find(Reservation.class, created.getId());
            first.getTransaction().begin();
            firstCopy.setReserveur("Alice updated");
            first.getTransaction().commit();

            second.getTransaction().begin();
            staleCopy.setReserveur("Bob");
            try {
                second.getTransaction().commit();
                fail("A stale update must fail with optimistic locking.");
            } catch (RollbackException | OptimisticLockException expected) {
                assertTrue(hasOptimisticLockCause(expected));
                if (second.getTransaction().isActive()) {
                    second.getTransaction().rollback();
                }
            }
        } finally {
            first.close();
            second.close();
        }
        assertEquals("Alice updated", service.trouverParId(created.getId()).getReserveur());
    }

    @Test
    public void retryReloadsCurrentEntityAndAppliesRequestedValues() {
        Reservation created = service.creerReservation(newReservation("Alice"));
        Reservation requested = newReservation("Bob");
        Reservation updated = service.modifierAvecRetry(created.getId(), requested, 3);
        assertEquals("Bob", updated.getReserveur());
        assertNotNull(updated.getVersion());
        assertTrue(updated.getVersion() > created.getVersion());
    }

    private Reservation newReservation(String reserveur) {
        LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
        return new Reservation("Salle A", reserveur, start, start.plusHours(1));
    }

    private boolean hasOptimisticLockCause(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof OptimisticLockException) {
                return true;
            }
        }
        return false;
    }
}
