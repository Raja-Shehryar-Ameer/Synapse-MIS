package com.synapse.repository;

import com.synapse.HibernateUtil;
import com.synapse.model.VitalLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.UUID;

public class VitalLogRepository {

    public void save(VitalLog log) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(log);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<VitalLog> findByPatientId(UUID patientId) {
        EntityManager em = HibernateUtil.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM VitalLog v WHERE v IN (SELECT vl FROM Patient p JOIN p.vitalLogs vl WHERE p.patientId = :pid) ORDER BY v.timestamp DESC",
                    VitalLog.class)
                    .setParameter("pid", patientId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
