/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.config;

import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO;
import de.bushnaq.abdalla.kassandra.repository.OidcProviderRepository;
import de.bushnaq.abdalla.kassandra.repository.SecurityConfigurationRepository;
import de.bushnaq.abdalla.kassandra.security.DatabaseClientRegistrationRepository;
import de.bushnaq.abdalla.kassandra.security.OidcAuthenticationManagerResolver;
import de.bushnaq.abdalla.kassandra.security.SecuritySecretService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Restores the curated H2 data snapshot for a new demo deployment.
 */
@Component
@Order(0)
@Slf4j
@ConditionalOnProperty(prefix = "kassandra", name = "mode", havingValue = "demo")
public class DemoSnapshotInitializer implements ApplicationRunner {

    private static final String CLIENT_ID             = "kassandra-demo";
    private static final String CLIENT_SECRET         = "demo-client-secret";
    private static final String DEFAULT_SNAPSHOT_PATH = "/opt/kassandra/demo/Demo-1.zip";
    private static final String DISCOVERY_URI         = "http://keycloak:8080/realms/kassandra-demo";
    private static final String ISSUER_URI            = "http://localhost:8180/realms/kassandra-demo";
    private static final String PROVIDER_DISPLAY_NAME = "Kassandra Demo";
    @Autowired
    private DatabaseClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private JdbcTemplate                         jdbcTemplate;
    @Autowired
    private OidcAuthenticationManagerResolver    oidcAuthenticationManagerResolver;
    @Autowired
    private OidcProviderRepository               oidcProviderRepository;
    @Autowired
    private SecurityConfigurationRepository      securityConfigurationRepository;
    @Autowired
    private SecuritySecretService                securitySecretService;
    @Value("${kassandra.demo.snapshot-path:" + DEFAULT_SNAPSHOT_PATH + "}")
    private String                               snapshotPath;

    private void configureDemoProvider() {
        OidcProviderDAO provider = oidcProviderRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The demo snapshot does not contain an OIDC provider"));
        String previousIssuerUri = provider.getIssuerUri();
        provider.setClientId(CLIENT_ID);
        provider.setClientSecretEncrypted(securitySecretService.encrypt(CLIENT_SECRET));
        provider.setDiscoveryUri(DISCOVERY_URI);
        provider.setDisplayName(PROVIDER_DISPLAY_NAME);
        provider.setEnabled(true);
        provider.setIssuerUri(ISSUER_URI);
        provider.setScopes("openid,profile,email");
        oidcProviderRepository.saveAndFlush(provider);
        clientRegistrationRepository.invalidate(provider.getRegistrationId());
        oidcAuthenticationManagerResolver.invalidate(previousIssuerUri);
        oidcAuthenticationManagerResolver.invalidate(ISSUER_URI);
    }

    private void markSetupReady() {
        SecurityConfigurationDAO configuration = securityConfigurationRepository
                .findById(SecurityConfigurationDAO.CONFIGURATION_ID)
                .orElseGet(SecurityConfigurationDAO::new);
        configuration.setSetupCompleted(true);
        configuration.setSetupState(SecurityConfigurationDAO.SetupState.READY);
        securityConfigurationRepository.saveAndFlush(configuration);
    }

    private void restoreSnapshot() {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("RUNSCRIPT FROM '" + snapshotPath.replace("\\", "/") + "' COMPRESSION ZIP");
        jdbcTemplate.execute("ALTER TABLE oidc_providers ADD COLUMN IF NOT EXISTS discovery_uri VARCHAR(2048)");
    }

    /**
     * Restores the snapshot and configures the bundled Keycloak provider for a new demo database.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (securityConfigurationRepository.existsById(SecurityConfigurationDAO.CONFIGURATION_ID)) {
            return;
        }

        restoreSnapshot();
        configureDemoProvider();
        markSetupReady();
        log.info("Restored the Kassandra demo database snapshot");
    }
}
