package org.example.backend_app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AppointmentMedicalStatistics(
        @JsonProperty("total_appointments") long totalAppointments,
        @JsonProperty("pending_appointments") long pendingAppointments,
        @JsonProperty("confirmed_appointments") long confirmedAppointments,
        @JsonProperty("completed_appointments") long completedAppointments,
        @JsonProperty("cancelled_appointments") long cancelledAppointments,
        @JsonProperty("total_appointment_value") BigDecimal totalAppointmentValue,
        @JsonProperty("total_medical_histories") long totalMedicalHistories,
        @JsonProperty("paid_medical_histories") long paidMedicalHistories,
        @JsonProperty("unpaid_medical_histories") long unpaidMedicalHistories,
        @JsonProperty("total_medical_cost") BigDecimal totalMedicalCost,
        @JsonProperty("paid_medical_cost") BigDecimal paidMedicalCost,
        @JsonProperty("unpaid_medical_cost") BigDecimal unpaidMedicalCost
) {
}
