package com.synapse.repository;

import com.synapse.HibernateUtil;
import com.synapse.model.SystemAlert;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.UUID;

/**
 * Generic repository for entities that share common CRUD patterns.
 * Maps to the "Repository" class in the class diagram.
 */
public class GenericRepository {

    public <T> void save(T entity) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public <T> T update(T entity) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T merged = em.merge(entity);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public <T> T findById(Class<T> entityClass, UUID id) {
        EntityManager em = HibernateUtil.createEntityManager();
        try {
            return em.find(entityClass, id);
        } finally {
            em.close();
        }
    }

    public SystemAlert findAlertById(UUID alertId) {
        return findById(SystemAlert.class, alertId);
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        EntityManager em = HibernateUtil.createEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void logEvent(String eventType) {
        // Logs an event as a SystemAlert of type "LOG"
        SystemAlert logAlert = new SystemAlert("LOG", eventType, java.time.LocalDateTime.now());
        logAlert.markAsAcknowledged();
        save(logAlert);
    }

    public <T> void delete(T entity) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T managed = em.merge(entity);
            em.remove(managed);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
