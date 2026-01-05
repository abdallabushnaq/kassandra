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

package de.bushnaq.abdalla.kassandra.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dao.RelationDAO;
import de.bushnaq.abdalla.kassandra.dao.TaskDAO;
import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SerializationTest {


    JsonMapper jsonMapper;

    @Test
    public void deserializeColorBlack(TestInfo testInfo) throws Exception {
        String json  = "\"#FF000000\"";
        Color  color = jsonMapper.readValue(json, Color.class);
        assertEquals(new Color(0, 0, 0), color);
    }

    @Test
    public void deserializeColorWhite(TestInfo testInfo) throws Exception {
        String json  = "\"#FFFFFFFF\"";
        Color  color = jsonMapper.readValue(json, Color.class);
        assertEquals(new Color(255, 255, 255), color);
    }

    @Test
    public void deserializeLocalDateTime(TestInfo testInfo) throws Exception {
        String        json          = "\"2021-09-30T15:30:00\"";
        LocalDateTime localDateTime = jsonMapper.readValue(json, LocalDateTime.class);
        assertEquals(LocalDateTime.parse("2021-09-30T15:30:00"), localDateTime);
    }

    @Test
    public void deserializeOffsetDateTime(TestInfo testInfo) throws Exception {
        String         json           = "\"2021-09-30T15:30:00+01:00\"";
        OffsetDateTime offsetDateTime = jsonMapper.readValue(json, OffsetDateTime.class);
        assertEquals(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"), offsetDateTime);
    }

    @Test
    public void deserializeTask(TestInfo testInfo) throws Exception {

        TaskDAO t1 = new TaskDAO();
        t1.setId(2L);
        t1.setStart(ParameterOptions.getLocalNow());
        t1.setMinEstimate(Duration.ofDays(6));
        t1.setMaxEstimate(Duration.ofDays(0));
        RelationDAO relation = new RelationDAO();
        relation.setId(1L);
        relation.setPredecessorId(1L);
        relation.setVisible(true);
        t1.getPredecessors().add(relation);
        t1.setName("test-1");
        String s1   = jsonMapper.writeValueAsString(t1);
        Task   task = jsonMapper.readValue(s1, Task.class);


//        String json = "\"{critical:false,id:3,impactOnCost:true,maxEstimate:0s,milestone:false,minEstimate:6d,name:Implementation,orderId:2,parentTaskId:2,predecessors:[{id:1,predecessorId:1,visible:true}],progress:0,remainingEstimate:6d,resourceId:1,sprintId:1,start:2026-01-07T11:43:35.2766866,taskMode:AUTO_SCHEDULED,taskStatus:TODO,timeSpent:0s}\"";
//        Task   task = jsonMapper.readValue(json, Task.class);
//        Assertions.assertEquals(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"), offsetDateTime);
    }

    @BeforeEach
    public void init() {
        DateTimeFormatter formatter   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        SimpleModule      colorModule = new SimpleModule();
        colorModule.addSerializer(Color.class, new ColorSerializer());
        colorModule.addDeserializer(Color.class, new ColorDeserializer());
        jsonMapper = JsonMapper.builder()
                .addModule(colorModule)
                .addModule(new SimpleModule().addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer()))
                .addModule(new SimpleModule().addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer()))
                .addModule(new SimpleModule().addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter)))
                .addModule(new SimpleModule().addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter)))
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL).withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    @Test
    public void serializeLocalDateTime(TestInfo testInfo) throws Exception {
        {
            LocalDateTime localDateTime = LocalDateTime.parse("2021-09-30T15:30:00");
            String        json          = jsonMapper.writeValueAsString(localDateTime);
            assertEquals("\"2021-09-30T15:30:00\"", json);
        }
        {
            LocalDateTime localDateTime = LocalDateTime.now();
            String        json1         = jsonMapper.writeValueAsString(localDateTime);
            String        json2         = jsonMapper.writeValueAsString(localDateTime.truncatedTo(ChronoUnit.SECONDS));
            assertEquals(json2, json1);
        }
    }

    @Test
    public void serializeOffsetDateTime(TestInfo testInfo) throws Exception {
        {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
            String         json           = jsonMapper.writeValueAsString(offsetDateTime);
            assertEquals("\"2021-09-30T15:30:00+01:00\"", json);
        }
        {
            OffsetDateTime offsetDateTime = OffsetDateTime.now();
            String         json1          = jsonMapper.writeValueAsString(offsetDateTime);
            String         json2          = jsonMapper.writeValueAsString(offsetDateTime.truncatedTo(ChronoUnit.SECONDS));
            Assertions.assertNotEquals(json2, json1);
        }
    }

    @Test
    public void userTest(TestInfo testInfo) throws Exception {
        UserDAO u1 = new UserDAO();
        u1.setId(1L);
        u1.setName("test");
        u1.setEmail("test");
        u1.setColor(Color.RED);
        u1.setFirstWorkingDay(LocalDateTime.now().toLocalDate());
        u1.setRoles("USER,ADMIN");
        String json = jsonMapper.writeValueAsString(u1);
        User   user = jsonMapper.readValue(json, User.class);

        assertEquals(u1.getId(), user.getId());
        assertEquals(u1.getName(), user.getName());
        assertEquals(u1.getEmail(), user.getEmail());
        assertEquals(u1.getColor(), user.getColor());
        assertEquals(u1.getFirstWorkingDay(), user.getFirstWorkingDay());
        assertEquals(2, user.getRoleList().size());
        {
            String json2 = "{\"availabilities\":[{\"availability\":0.5,\"id\":1,\"start\":\"2023-05-05\"}],\"avatarHash\":\"bfedee7b59c89684\",\"color\":\"#FFFF0000\",\"email\":\"christopher.paul@kassandra.org\",\"firstWorkingDay\":\"2023-05-05\",\"id\":1,\"locations\":[{\"country\":\"de\",\"id\":1,\"start\":\"2023-05-05\",\"state\":\"nw\"}],\"name\":\"Christopher Paul\",\"roles\":\"ADMIN,USER\"}";
            User   user2 = jsonMapper.readValue(json2, User.class);
        }

    }

}
