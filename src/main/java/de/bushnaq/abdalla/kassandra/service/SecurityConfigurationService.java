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

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO;
import de.bushnaq.abdalla.kassandra.repository.SecurityConfigurationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Manages the singleton setup lifecycle and restricted local recovery credential.
 */
@Service
public class SecurityConfigurationService {

    @Value("${KASSANDRA_SETUP_TOKEN:}")
    private String                          bootstrapToken;
    @Autowired
    private PasswordEncoder                 passwordEncoder;
    @Autowired
    private SecurityConfigurationRepository securityConfigurationRepository;

    private boolean bootstrapTokenMatches(String presentedBootstrapToken) {
        if (bootstrapToken == null || bootstrapToken.isBlank()) {
            throw new IllegalStateException("KASSANDRA_SETUP_TOKEN must be configured before setup can be claimed");
        }
        return MessageDigest.isEqual(
                bootstrapToken.getBytes(StandardCharsets.UTF_8),
                presentedBootstrapToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Claims setup and stores the password for the restricted local recovery account.
     *
     * @param presentedBootstrapToken deployment-provided setup token
     * @param recoveryPassword        password for the restricted recovery account
     * @throws IllegalArgumentException when either required value is blank
     * @throws IllegalStateException    when the bootstrap token is invalid or setup is already claimed
     */
    @Transactional
    public void claimSetup(String presentedBootstrapToken, String recoveryPassword) {
        if (presentedBootstrapToken == null || presentedBootstrapToken.isBlank()) {
            throw new IllegalArgumentException("A setup token is required");
        }
        if (recoveryPassword == null || recoveryPassword.isBlank()) {
            throw new IllegalArgumentException("A recovery password is required");
        }
        if (!bootstrapTokenMatches(presentedBootstrapToken)) {
            throw new IllegalStateException("The setup token is invalid");
        }

        SecurityConfigurationDAO configuration = getConfiguration();
        if (configuration.getSetupState() != SecurityConfigurationDAO.SetupState.SETUP_REQUIRED) {
            throw new IllegalStateException("Setup has already been claimed");
        }
        configuration.setRecoveryPasswordHash(passwordEncoder.encode(recoveryPassword));
        configuration.setSetupState(SecurityConfigurationDAO.SetupState.SETUP_IN_PROGRESS);
        securityConfigurationRepository.saveAndFlush(configuration);
    }

    /**
     * Marks a successfully claimed setup flow as complete.
     *
     * @throws IllegalStateException when setup was not claimed first
     */
    @Transactional
    public void completeSetup() {
        SecurityConfigurationDAO configuration = getConfiguration();
        if (configuration.getSetupState() != SecurityConfigurationDAO.SetupState.SETUP_IN_PROGRESS) {
            throw new IllegalStateException("Setup must be claimed before it can be completed");
        }
        configuration.setSetupCompleted(true);
        configuration.setSetupState(SecurityConfigurationDAO.SetupState.READY);
        securityConfigurationRepository.save(configuration);
    }

    /**
     * Gets the singleton security configuration, creating it on a new installation.
     *
     * @return current security configuration
     */
    @Transactional
    public SecurityConfigurationDAO getConfiguration() {
        return securityConfigurationRepository.findById(SecurityConfigurationDAO.CONFIGURATION_ID)
                .orElseGet(() -> securityConfigurationRepository.save(new SecurityConfigurationDAO()));
    }

    /**
     * Determines whether the restricted local recovery credential is available.
     *
     * @return true when setup has created a recovery password
     */
    @Transactional
    public boolean hasRecoveryCredential() {
        return getConfiguration().getRecoveryPasswordHash() != null;
    }

    /**
     * Determines whether the initial setup wizard must be completed.
     *
     * @return true until setup reaches the ready state
     */
    @Transactional
    public boolean isSetupRequired() {
        return getConfiguration().getSetupState() != SecurityConfigurationDAO.SetupState.READY;
    }

    /**
     * Verifies a restricted local recovery password.
     *
     * @param recoveryPassword password to verify
     * @return true when the password matches the stored bcrypt hash
     */
    @Transactional
    public boolean matchesRecoveryPassword(String recoveryPassword) {
        String hash = getConfiguration().getRecoveryPasswordHash();
        return hash != null && recoveryPassword != null && passwordEncoder.matches(recoveryPassword, hash);
    }
}
