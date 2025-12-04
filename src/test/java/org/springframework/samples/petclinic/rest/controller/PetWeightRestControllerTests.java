/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.dto.OwnerDto;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for Pet weight functionality in REST controllers
 */

@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class PetWeightRestControllerTests {

    @MockitoBean
    protected ClinicService clinicService;
    @Autowired
    private PetRestController petRestController;
    @Autowired
    private OwnerRestController ownerRestController;
    @Autowired
    private PetMapper petMapper;
    private MockMvc mockMvc;

    private PetDto petWithWeight;
    private PetDto petWithoutWeight;
    private PetFieldsDto petFieldsWithWeight;
    private PetFieldsDto petFieldsWithoutWeight;

    @BeforeEach
    void initPets() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(petRestController, ownerRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();

        PetTypeDto petType = new PetTypeDto();
        petType.id(2).name("dog");

        // Pet with weight
        petWithWeight = new PetDto();
        petWithWeight.id(1)
            .name("Buddy")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petType)
            .weight(new BigDecimal("15.75"));

        // Pet without weight
        petWithoutWeight = new PetDto();
        petWithoutWeight.id(2)
            .name("Max")
            .birthDate(LocalDate.of(2021, 6, 15))
            .type(petType)
            .weight(null);

        // PetFields with weight
        petFieldsWithWeight = new PetFieldsDto();
        petFieldsWithWeight.name("Charlie")
            .birthDate(LocalDate.of(2019, 3, 10))
            .type(petType)
            .weight(new BigDecimal("22.50"));

        // PetFields without weight
        petFieldsWithoutWeight = new PetFieldsDto();
        petFieldsWithoutWeight.name("Luna")
            .birthDate(LocalDate.of(2022, 8, 20))
            .type(petType)
            .weight(null);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetWithWeight() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(petWithWeight));
        this.mockMvc.perform(get("/api/pets/1")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Buddy"))
            .andExpect(jsonPath("$.weight").value(15.75));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetPetWithoutWeight() throws Exception {
        given(this.clinicService.findPetById(2)).willReturn(petMapper.toPet(petWithoutWeight));
        this.mockMvc.perform(get("/api/pets/2")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("Max"))
            .andExpect(jsonPath("$.weight").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetWithWeight() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(petWithWeight));
        
        PetDto updatedPet = new PetDto();
        updatedPet.id(1)
            .name("Buddy Updated")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petWithWeight.getType())
            .weight(new BigDecimal("18.25"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String updatedPetAsJSON = mapper.writeValueAsString(updatedPet);
        this.mockMvc.perform(put("/api/pets/1")
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdatePetRemoveWeight() throws Exception {
        given(this.clinicService.findPetById(1)).willReturn(petMapper.toPet(petWithWeight));
        
        PetDto updatedPet = new PetDto();
        updatedPet.id(1)
            .name("Buddy Updated")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petWithWeight.getType())
            .weight(null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String updatedPetAsJSON = mapper.writeValueAsString(updatedPet);
        this.mockMvc.perform(put("/api/pets/1")
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithWeight() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String petFieldsAsJSON = mapper.writeValueAsString(petFieldsWithWeight);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(petFieldsAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithoutWeight() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String petFieldsAsJSON = mapper.writeValueAsString(petFieldsWithoutWeight);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(petFieldsAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnersPetWithWeight() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String petFieldsAsJSON = mapper.writeValueAsString(petFieldsWithWeight);
        this.mockMvc.perform(put("/api/owners/1/pets/1")
                .content(petFieldsAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationMinimum() throws Exception {
        PetFieldsDto invalidPet = new PetFieldsDto();
        invalidPet.name("Invalid Pet")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petFieldsWithWeight.getType())
            .weight(new BigDecimal("-1.0")); // Invalid negative weight

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String invalidPetAsJSON = mapper.writeValueAsString(invalidPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(invalidPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationMaximum() throws Exception {
        PetFieldsDto invalidPet = new PetFieldsDto();
        invalidPet.name("Invalid Pet")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petFieldsWithWeight.getType())
            .weight(new BigDecimal("1000.0")); // Invalid weight exceeding maximum

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String invalidPetAsJSON = mapper.writeValueAsString(invalidPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(invalidPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testWeightValidationValidRange() throws Exception {
        PetFieldsDto validPet = new PetFieldsDto();
        validPet.name("Valid Pet")
            .birthDate(LocalDate.of(2020, 1, 1))
            .type(petFieldsWithWeight.getType())
            .weight(new BigDecimal("50.25")); // Valid weight within range

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String validPetAsJSON = mapper.writeValueAsString(validPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(validPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());
    }
}