package com.synapse.repository;

import com.synapse.HibernateUtil;
import com.synapse.model.Patient;
import com.synapse.ui.SessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.UUID;

public class PatientRepository {

    public boolean existsByEmail(String email) {
        if (existsInFactory(HibernateUtil.getFileEmf(), email)) return true;
        try { if (existsInFactory(HibernateUtil.getSqlEmf(), email)) return true; } catch (Exception ignored) {}
        return false;
    }

    private boolean existsInFactory(jakarta.persistence.EntityManagerFactory emf, String email) {
        if (emf == null) return false;
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(p) FROM Patient p WHERE p.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public boolean existsByEmailForOtherPatient(String email, UUID patientId) {
        if (existsForOtherInFactory(HibernateUtil.getFileEmf(), email, patientId)) return true;
        try { if (existsForOtherInFactory(HibernateUtil.getSqlEmf(), email, patientId)) return true; } catch (Exception ignored) {}
        return false;
    }

    private boolean existsForOtherInFactory(jakarta.persistence.EntityManagerFactory emf, String email, UUID patientId) {
        if (emf == null) return false;
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(p) FROM Patient p WHERE lower(p.email) = lower(:email) AND p.patientId <> :patientId",
                            Long.class
                    )
                    .setParameter("email", email)
                    .setParameter("patientId", patientId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public void save(Patient patient) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(patient);
            em.flush();
            initializeCollections(patient);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Patient update(Patient patient) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Patient merged = em.merge(patient);
            initializeCollections(merged);
            tx.commit();
            SessionManager.setCurrentPatient(merged);
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Patient findById(UUID id) {
        EntityManager em = HibernateUtil.createEntityManager();
        try {
            Patient p = em.find(Patient.class, id);
            if (p != null) initializeCollections(p);
            return p;
        } finally {
            em.close();
        }
    }

    public Patient findByEmail(String email) {
        Patient p = findInFactory(HibernateUtil.getFileEmf(), email);
        if (p != null) {
            HibernateUtil.setActiveFactory(HibernateUtil.getFileEmf());
            return p;
        }
        try {
            p = findInFactory(HibernateUtil.getSqlEmf(), email);
            if (p != null) {
                HibernateUtil.setActiveFactory(HibernateUtil.getSqlEmf());
                return p;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Patient findInFactory(jakarta.persistence.EntityManagerFactory emf, String email) {
        if (emf == null) return null;
        EntityManager em = emf.createEntityManager();
        try {
            Patient p = em.createQuery("SELECT p FROM Patient p WHERE p.email = :email", Patient.class)
                    .setParameter("email", email)
                    .getResultStream().findFirst().orElse(null);
            if (p != null) initializeCollections(p);
            return p;
        } finally {
            em.close();
        }
    }

    /**
     * Forces Hibernate to load every lazy collection while the
     * EntityManager is still open, so they remain accessible after
     * the entity becomes detached.
     */
    private void initializeCollections(Patient p) {
        p.getVitalLogs().size();
        p.getSymptomLogs().size();
        p.getWeightLogs().size();
        p.getDietLogs().size();
        p.getHydrationLogs().size();
        p.getJournalEntries().size();
        p.getMedicines().size();
        p.getCalendarEvents().size();
        p.getMedicalRecords().size();
        p.getHealthReports().size();
        p.getDataBackups().size();
        // EmergencyProfile is EAGER, but touch it just in case
        if (p.getEmergencyProfile() != null) {
            p.getEmergencyProfile().getProfileId();
        }
    }

    public void delete(Patient patient) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // merge first to attach the entity to the session, then remove
            Patient merged = em.merge(patient);
            em.remove(merged);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Recursively nullifies all @Id fields on a deserialized entity and its
     * @OneToOne / @OneToMany children, so Hibernate treats them as brand-new
     * entities and generates fresh UUIDs on persist.
     */
    private void stripIds(Object entity) {
        if (entity == null) return;
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    // Null out the @Id field
                    if (f.isAnnotationPresent(Id.class)) {
                        f.set(entity, null);
                    }
                    // Recurse into @OneToOne children
                    if (f.isAnnotationPresent(OneToOne.class)) {
                        Object child = f.get(entity);
                        if (child != null) stripIds(child);
                    }
                    // Recurse into @OneToMany collection children
                    if (f.isAnnotationPresent(OneToMany.class)) {
                        Object col = f.get(entity);
                        if (col instanceof Collection<?>) {
                            for (Object child : (Collection<?>) col) {
                                stripIds(child);
                            }
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Replaces all patient data with restored backup data.
     * Uses a proper transactional approach:
     * 1. Load the managed entity
     * 2. Clear all collections (triggers orphan removal)
     * 3. Flush to delete old data from DB
     * 4. Strip pre-existing IDs from restored entities so Hibernate generates fresh ones
     * 5. Copy restored scalar fields and re-populate collections
     * 6. Commit and return the refreshed patient
     */
    public Patient restorePatient(Patient current, Patient restored) {
        EntityManager em = HibernateUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Patient managed = em.find(Patient.class, current.getPatientId());
            if (managed == null) {
                tx.rollback();
                return current;
            }

            // Clear all collections to trigger orphan removal
            managed.getVitalLogs().clear();
            managed.getSymptomLogs().clear();
            managed.getWeightLogs().clear();
            managed.getDietLogs().clear();
            managed.getHydrationLogs().clear();
            managed.getJournalEntries().clear();
            managed.getMedicines().clear();
            managed.getCalendarEvents().clear();
            managed.getMedicalRecords().clear();
            managed.getHealthReports().clear();
            managed.getDataBackups().clear();
            if (managed.getEmergencyProfile() != null) {
                managed.setEmergencyProfile(null);
            }
            em.flush(); // Delete old data from DB

            // Strip all pre-existing IDs from restored entities so Hibernate
            // generates fresh UUIDs and treats them as new (persist, not merge)
            if (restored.getVitalLogs() != null) restored.getVitalLogs().forEach(this::stripIds);
            if (restored.getSymptomLogs() != null) restored.getSymptomLogs().forEach(this::stripIds);
            if (restored.getWeightLogs() != null) restored.getWeightLogs().forEach(this::stripIds);
            if (restored.getDietLogs() != null) restored.getDietLogs().forEach(this::stripIds);
            if (restored.getHydrationLogs() != null) restored.getHydrationLogs().forEach(this::stripIds);
            if (restored.getJournalEntries() != null) restored.getJournalEntries().forEach(this::stripIds);
            if (restored.getMedicines() != null) restored.getMedicines().forEach(this::stripIds);
            if (restored.getCalendarEvents() != null) restored.getCalendarEvents().forEach(this::stripIds);
            if (restored.getMedicalRecords() != null) restored.getMedicalRecords().forEach(this::stripIds);
            if (restored.getHealthReports() != null) restored.getHealthReports().forEach(this::stripIds);
            if (restored.getDataBackups() != null) restored.getDataBackups().forEach(this::stripIds);
            if (restored.getEmergencyProfile() != null) stripIds(restored.getEmergencyProfile());

            // Copy scalar fields
            managed.setFullName(restored.getFullName());
            managed.setDateOfBirth(restored.getDateOfBirth());
            managed.setGender(restored.getGender());
            managed.setHeightCm(restored.getHeightCm());
            managed.setCurrentBMI(restored.getCurrentBMI());
            managed.setEmergencyProfile(restored.getEmergencyProfile());

            // Re-populate collections from restored data
            if (restored.getVitalLogs() != null) managed.getVitalLogs().addAll(restored.getVitalLogs());
            if (restored.getSymptomLogs() != null) managed.getSymptomLogs().addAll(restored.getSymptomLogs());
            if (restored.getWeightLogs() != null) managed.getWeightLogs().addAll(restored.getWeightLogs());
            if (restored.getDietLogs() != null) managed.getDietLogs().addAll(restored.getDietLogs());
            if (restored.getHydrationLogs() != null) managed.getHydrationLogs().addAll(restored.getHydrationLogs());
            if (restored.getJournalEntries() != null) managed.getJournalEntries().addAll(restored.getJournalEntries());
            if (restored.getMedicines() != null) managed.getMedicines().addAll(restored.getMedicines());
            if (restored.getCalendarEvents() != null) managed.getCalendarEvents().addAll(restored.getCalendarEvents());
            if (restored.getMedicalRecords() != null) managed.getMedicalRecords().addAll(restored.getMedicalRecords());
            if (restored.getHealthReports() != null) managed.getHealthReports().addAll(restored.getHealthReports());
            if (restored.getDataBackups() != null) managed.getDataBackups().addAll(restored.getDataBackups());

            initializeCollections(managed);
            tx.commit();
            SessionManager.setCurrentPatient(managed);
            return managed;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
