package com.synapse.repository;

import com.synapse.HibernateUtil;
import com.synapse.model.SymptomLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.UUID;

public class SymptomLogRepository {

    public void save(SymptomLog log) {
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

    public List<SymptomLog> findByPatientId(UUID patientId) {
        EntityManager em = HibernateUtil.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM SymptomLog s WHERE s IN (SELECT sl FROM Patient p JOIN p.symptomLogs sl WHERE p.patientId = :pid) ORDER BY s.timestamp DESC",
                    SymptomLog.class)
                    .setParameter("pid", patientId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
