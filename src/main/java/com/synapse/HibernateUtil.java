package com.synapse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Singleton holder for the JPA EntityManagerFactory.
 * All database access goes through EntityManager instances obtained here.
 *
 * To switch from H2 to Oracle/SQL Server, change only persistence.xml — no code changes needed.
 */
public class HibernateUtil {

    private static EntityManagerFactory fileEmf;
    private static EntityManagerFactory sqlEmf;
    private static EntityManagerFactory activeEmf;

    static {
        try {
            fileEmf = Persistence.createEntityManagerFactory("synapse-file-pu");
        } catch (Exception e) {
            System.err.println("File EMF creation failed: " + e.getMessage());
        }
        activeEmf = fileEmf;
    }

    public static EntityManagerFactory getFileEmf() {
        return fileEmf;
    }

    public static EntityManagerFactory getSqlEmf() {
        if (sqlEmf == null) {
            sqlEmf = Persistence.createEntityManagerFactory("synapse-sql-pu");
        }
        return sqlEmf;
    }

    public static void setActiveFactory(EntityManagerFactory emf) {
        activeEmf = emf;
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return activeEmf;
    }

    public static EntityManager createEntityManager() {
        if (activeEmf == null) {
            throw new IllegalStateException("No active EntityManagerFactory available.");
        }
        return activeEmf.createEntityManager();
    }

    public static void shutdown() {
        if (fileEmf != null && fileEmf.isOpen()) fileEmf.close();
        if (sqlEmf != null && sqlEmf.isOpen()) sqlEmf.close();
    }
}
