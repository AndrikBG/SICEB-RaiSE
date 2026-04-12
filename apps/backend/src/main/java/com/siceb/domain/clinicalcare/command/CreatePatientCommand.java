package com.siceb.domain.clinicalcare.command;

import com.siceb.domain.clinicalcare.model.Gender;
import com.siceb.domain.clinicalcare.model.PatientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePatientCommand(
        @NotNull UUID patientId,
        @NotBlank String firstName,
        @NotBlank String paternalSurname,
        String maternalSurname,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        String phone,
        String curp,
        @NotNull PatientType patientType,
        String credentialNumber,
        String guardianName,
        String guardianRelationship,
        String guardianPhone,
        boolean guardianIdConfirmed,
        boolean dataConsentGiven,
        boolean specialCase,
        String specialCaseNotes,
        @NotBlank String idempotencyKey
) {}
