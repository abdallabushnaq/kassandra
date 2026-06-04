/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.service.DatabaseDebugService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

public class AbstractTestUtil extends DTOAsserts {
    @Autowired
    private   DatabaseDebugService       databaseDebugService;
    @Autowired
    private   DefaultEntitiesInitializer defaultEntitiesInitializer;
    @Autowired
    protected EntityManager              entityManager;
    @Autowired
    protected PlatformTransactionManager transactionManager;

    @AfterEach
    protected void afterEach(TestInfo testInfo) throws Exception {
        logger.info("----------------------------------------------");
        logger.info("stop " + testInfo.getDisplayName());
        logger.info("==============================================");
    }

    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        logger.info("==============================================");
        logger.info("start " + testInfo.getDisplayName());
        logger.info("----------------------------------------------");

        // Manually create a transaction
        DefaultTransactionDefinition def    = new DefaultTransactionDefinition();
        TransactionStatus            status = transactionManager.getTransaction(def);

        try {
            List<String> tableNames = getAllTableNames();
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            entityManager.flush();

            // Commit the transaction
            transactionManager.commit(status);
            logger.info("truncated all tables.");
        } catch (Exception e) {
            // Rollback on error
            transactionManager.rollback(status);
            throw e;
        }
        //-- generate all missing entities.
        defaultEntitiesInitializer.run(null);
    }

    /**
     * Returns all user-table names in the current H2 schema, excluding H2 system tables.
     *
     * <p><em>Note:</em> {@link DatabaseDebugService} contains an identical private copy of this
     * query used solely for printing.  Keep both in sync if the filter list changes.
     */
    private List<String> getAllTableNames() {
        @SuppressWarnings("unchecked")
        List<String> tableNames = entityManager
                .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA()")
                .getResultList();

        return tableNames.stream()
                .filter(name -> !name.equals("SPATIAL_REF_SYS") && !name.equals("GEOMETRY_COLUMNS"))
                .toList();
    }

    /**
     * Prints all non-empty tables in the current schema to the test log.
     */
    protected void printTables() {
        databaseDebugService.printTables();
    }

    /**
     * Prints the specified tables (or all non-empty tables when {@code filterTableNames} is
     * {@code null}) to {@link System#out}.
     *
     * @param filterTableNames optional array of table names to include; pass {@code null} to print
     *                         every non-empty table
     */
    protected void printTables(String[] filterTableNames) {
        databaseDebugService.printTables(filterTableNames);
    }

}
