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

package de.bushnaq.abdalla.kassandra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

//@Component
//TODO: Remove this class if not needed
public class SecurityDataInitializer implements CommandLineRunner {

    @Autowired
    private PasswordEncoder        passwordEncoder;
    @Autowired
    private SecurityUserRepository securityUserRepository;

    @Override
    public void run(String... args) {
        // Create a default admin user if none exists
        Optional<SecurityUser> existingUser = securityUserRepository.findByUsername("admin");

        if (existingUser.isEmpty()) {
            SecurityUser adminUser = SecurityUser.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles("ADMIN,USER")
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            securityUserRepository.save(adminUser);

            System.out.println("Created default admin user with username: admin and password: admin123");
        }
    }
}
