package com.example.service;

import com.example.model.Reservation;
import com.example.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.util.List;
import java.util.Objects;

public class ReservationServiceImpl implements ReservationService {
    @Override
    public Reservation creerReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        return write(em -> {
            em.persist(reservation);
            return reservation;
        });
    }

    @Override
    public Reservation modifierReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        return write(em -> em.merge(reservation));
    }

    @Override
    public Reservation modifierAvecRetry(Long id, Reservation modifications, int maxTentatives) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modifications, "modifications");
        if (maxTentatives < 1) {
            throw new IllegalArgumentException("Le nombre de tentatives doit etre positif.");
        }

        RuntimeException lastConflict = null;
        for (int tentative = 0; tentative < maxTentatives; tentative++) {
            try {
                return write(em -> {
                    Reservation actuelle = em.find(Reservation.class, id);
                    if (actuelle == null) {
                        throw new IllegalArgumentException("Reservation introuvable: " + id);
                    }
                    actuelle.setSalle(modifications.getSalle());
                    actuelle.setReserveur(modifications.getReserveur());
                    actuelle.setDateDebut(modifications.getDateDebut());
                    actuelle.setDateFin(modifications.getDateFin());
                    return actuelle;
                });
            } catch (RuntimeException e) {
                if (!containsOptimisticLock(e)) {
                    throw e;
                }
                lastConflict = e;
            }
        }
        throw new IllegalStateException("Echec apres " + maxTentatives + " tentatives de mise a jour.", lastConflict);
    }

    @Override
    public Reservation trouverParId(Long id) {
        return read(em -> em.find(Reservation.class, id));
    }

    @Override
    public List<Reservation> trouverToutes() {
        return read(em -> em.createQuery("SELECT r FROM Reservation r ORDER BY r.id", Reservation.class)
                .getResultList());
    }

    @Override
    public void supprimer(Long id) {
        write(em -> {
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation != null) {
                em.remove(reservation);
            }
            return null;
        });
    }

    private <T> T read(Work<T> work) {
        EntityManager em = JpaUtil.getEntityManagerFactory().createEntityManager();
        try {
            return work.run(em);
        } finally {
            em.close();
        }
    }

    private <T> T write(Work<T> work) {
        EntityManager em = JpaUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.run(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private boolean containsOptimisticLock(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return error instanceof RollbackException && error.getCause() instanceof OptimisticLockException;
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(EntityManager em);
    }
}
