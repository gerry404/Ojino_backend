package com.schoolcopilot.engagement_service.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Le releve du jour.
 *
 * <p>Humeur et charge sont deux mesures distinctes : on peut aller bien et etre
 * debordee, ou aller mal sans surcharge de travail.
 */
public record CheckInRequest(

        @Min(value = 1, message = "L'humeur se note de 1 a 5.")
        @Max(value = 5, message = "L'humeur se note de 1 a 5.")
        int mood,

        @Min(value = 1, message = "La charge se note de 1 a 5.")
        @Max(value = 5, message = "La charge se note de 1 a 5.")
        int workload,

        @Size(max = 500, message = "La note est trop longue.")
        String note) {
}
