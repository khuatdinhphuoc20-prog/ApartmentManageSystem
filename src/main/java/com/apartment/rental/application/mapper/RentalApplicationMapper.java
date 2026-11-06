package com.apartment.rental.application.mapper;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.entity.RentalApplication;
import com.apartment.rental.room.entity.Room;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RentalApplicationMapper {

    public RentalApplicationResponse toResponse(RentalApplication entity) {
        if (entity == null) {
            return null;
        }

        User applicant = entity.getApplicant();
        Room room = entity.getRoom();
        User processedByUser = entity.getProcessedByUser();

        return new RentalApplicationResponse(
                entity.getId(),
                applicant != null ? applicant.getId() : null,
                applicant != null ? applicant.getFullName() : null,
                applicant != null ? applicant.getEmail() : null,
                applicant != null ? applicant.getPhone() : null,
                applicant != null ? applicant.getIdentityDocumentNo() : null,
                room != null ? room.getId() : null,
                room != null ? room.getRoomCode() : null,
                room != null ? Integer.valueOf(room.getFloorNumber()) : null,
                room != null ? room.getListedMonthlyRent() : null,
                entity.getRequestedStartDate(),
                entity.getRequestedEndDate(),
                entity.getStatus(),
                entity.getProcessedAt(),
                processedByUser != null ? processedByUser.getId() : null,
                processedByUser != null ? processedByUser.getFullName() : null,
                entity.getProcessingNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RentalApplicationResponse> toResponseList(List<RentalApplication> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }
}