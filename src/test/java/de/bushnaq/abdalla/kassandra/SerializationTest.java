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

package de.bushnaq.abdalla.kassandra;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SerializationTest {
    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void deserializeColorBlack(TestInfo testInfo) throws Exception {
        String json  = "\"#FF000000\"";
        Color  color = objectMapper.readValue(json, Color.class);
        Assertions.assertEquals(new Color(0, 0, 0), color);
    }

    @Test
    public void deserializeColorWhite(TestInfo testInfo) throws Exception {
        String json  = "\"#FFFFFFFF\"";
        Color  color = objectMapper.readValue(json, Color.class);
        Assertions.assertEquals(new Color(255, 255, 255), color);
    }

    @Test
    public void deserializeLocalDateTime(TestInfo testInfo) throws Exception {
        String        json          = "\"2021-09-30T15:30:00\"";
        LocalDateTime localDateTime = objectMapper.readValue(json, LocalDateTime.class);
        Assertions.assertEquals(LocalDateTime.parse("2021-09-30T15:30:00"), localDateTime);
    }

    @Test
    public void deserializeOffsetDateTime(TestInfo testInfo) throws Exception {
        String         json           = "\"2021-09-30T15:30:00+01:00\"";
        OffsetDateTime offsetDateTime = objectMapper.readValue(json, OffsetDateTime.class);
        Assertions.assertEquals(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"), offsetDateTime);
    }

    @Test
    public void serializeLocalDateTime(TestInfo testInfo) throws Exception {
        {
            LocalDateTime localDateTime = LocalDateTime.parse("2021-09-30T15:30:00");
            String        json          = objectMapper.writeValueAsString(localDateTime);
            Assertions.assertEquals("\"2021-09-30T15:30:00\"", json);
        }
        {
            LocalDateTime localDateTime = LocalDateTime.now();
            String        json1         = objectMapper.writeValueAsString(localDateTime);
            String        json2         = objectMapper.writeValueAsString(localDateTime.truncatedTo(ChronoUnit.SECONDS));
            Assertions.assertEquals(json2, json1);
        }
    }

    @Test
    public void serializeOffsetDateTime(TestInfo testInfo) throws Exception {
        {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
            String         json           = objectMapper.writeValueAsString(offsetDateTime);
            Assertions.assertEquals("\"2021-09-30T15:30:00+01:00\"", json);
        }
        {
            OffsetDateTime offsetDateTime = OffsetDateTime.now();
            String         json1          = objectMapper.writeValueAsString(offsetDateTime);
            String         json2          = objectMapper.writeValueAsString(offsetDateTime.truncatedTo(ChronoUnit.SECONDS));
            Assertions.assertNotEquals(json2, json1);
        }
    }

}
