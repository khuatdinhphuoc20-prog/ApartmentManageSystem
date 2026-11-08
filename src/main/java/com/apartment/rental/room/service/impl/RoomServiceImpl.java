package com.apartment.rental.room.service.impl;

import com.apartment.rental.contract.port.ContractQueryPort;
import com.apartment.rental.room.dto.request.RoomRequest;
import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;
import com.apartment.rental.room.dto.response.RoomSummaryDto;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.exception.CannotChangeRentException;
import com.apartment.rental.room.exception.DuplicateRoomCodeException;
import com.apartment.rental.room.exception.InvalidCapacityException;
import com.apartment.rental.room.exception.InvalidRoomStateException;
import com.apartment.rental.room.exception.RoomNotAvailableException;
import com.apartment.rental.room.exception.RoomNotFoundException;
import com.apartment.rental.room.mapper.RoomMapper;
import com.apartment.rental.room.port.RoomOccupancyCommandPort;
import com.apartment.rental.room.port.RoomQueryPort;
import com.apartment.rental.room.repository.RoomRepository;
import com.apartment.rental.room.service.RoomService;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService, RoomQueryPort, RoomOccupancyCommandPort {

    RoomRepository roomRepository;
    RoomMapper roomMapper;
    ContractQueryPort contractQueryPort;

    @Override
    public List<RoomResponse> getAllAvailableRooms(String keyword) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return roomRepository.searchAvailableRooms(trimmedKeyword)
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    @Override
    public List<RoomResponse> filterAvailableRooms(Short floorNumber, BigDecimal minPrice, BigDecimal maxPrice,
                                                   Short minCapacity, BigDecimal minArea, BigDecimal maxArea) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa");
        }
        if (minArea != null && maxArea != null && minArea.compareTo(maxArea) > 0) {
            throw new IllegalArgumentException("Diện tích tối thiểu không được lớn hơn diện tích tối đa");
        }

        return roomRepository.filterAvailableRooms(floorNumber, minPrice, maxPrice, minCapacity, minArea, maxArea)
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    @Override
    public RoomResponse getRoomDetail(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RoomNotAvailableException("Phòng không khả dụng để xem chi tiết công khai");
        }
        return roomMapper.toRoomResponse(room);
    }

    @Override
    public List<RoomResponse> getAllRoomsForAdmin(String keyword, RoomStatus status) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return roomRepository.searchAllRoomsForAdmin(trimmedKeyword, status)
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return roomMapper.toRoomResponse(room);
    }

    @Override
    @Transactional
    public void createRoom(RoomRequest request) {
        String cleanCode = request.getRoomCode().trim();

        if (roomRepository.existsByRoomCode(cleanCode)) {
            throw new DuplicateRoomCodeException("Mã phòng '" + cleanCode + "' đã tồn tại!");
        }

        Room room = roomMapper.toRoom(request);
        room.setRoomCode(cleanCode);
        room.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void updateRoom(RoomRequest request, Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        String newCode = request.getRoomCode().trim();

        // Nếu Admin đổi mã phòng, kiểm tra mã mới có bị trùng với phòng khác không
        if (!room.getRoomCode().equalsIgnoreCase(newCode)) {
            if(roomRepository.existsByRoomCode(newCode)){
                throw new DuplicateRoomCodeException("Mã phòng '" + newCode + "' đã được sử dụng bởi phòng khác!");
            }
        }

        // Chỉ đổi giá niêm yết khi không có Contract SCHEDULED/ACTIVE
        boolean rentChanged = request.getListedMonthlyRent() != null
                && room.getListedMonthlyRent().compareTo(request.getListedMonthlyRent()) != 0;
        if (rentChanged && contractQueryPort.hasActiveOrScheduledContract(roomId)) {
            throw new CannotChangeRentException();
        }

        // Không giảm capacity xuống dưới số người đang ở thực tế
        if (request.getCapacity() != null && request.getCapacity() < room.getCapacity()) {
            int currentOccupants = contractQueryPort.getCurrentOccupantCount(roomId);
            if (request.getCapacity() < currentOccupants) {
                throw new InvalidCapacityException();
            }
        }

        roomMapper.updateRoomFromRequest(room, request);
        room.setRoomCode(newCode);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void toggleRoomStatus(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);

        // Không được tự ý đổi trạng thái RENTED sang INACTIVE
        if (room.getStatus() == RoomStatus.RENTED) {
            throw new InvalidRoomStateException("Không thể đóng trực tiếp phòng đang thuê. Phải thông qua quy trình kết thúc hợp đồng và quyết toán.");
        }

        if (room.getStatus() == RoomStatus.AVAILABLE) {
            if (contractQueryPort.hasActiveOrScheduledContract(roomId)) {
                throw new CannotChangeRentException("Không thể đóng phòng khi vẫn còn hợp đồng ràng buộc");
            }
            room.setStatus(RoomStatus.INACTIVE);
        } else {
            room.setStatus(RoomStatus.AVAILABLE);
        }

        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void reserveRoom(Long roomId) {
        // Khóa bi quan hàng trong DB, chống 2 Admin duyệt cùng 1 phòng (BR-ROOM-07)
        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(RoomNotFoundException::new);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RoomNotAvailableException("Phòng không còn ở trạng thái khả dụng để giữ chỗ");
        }
        room.setStatus(RoomStatus.RENTED);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void releaseRoom(Long roomId, RoomStatus targetStatus) {
        if (targetStatus != RoomStatus.AVAILABLE && targetStatus != RoomStatus.INACTIVE) {
            throw new InvalidRoomStateException("A released room must become AVAILABLE or INACTIVE");
        }

        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(RoomNotFoundException::new);

        // Chỉ phòng đang RENTED mới được giải phóng
        if (room.getStatus() != RoomStatus.RENTED) {
            throw new InvalidRoomStateException("Chỉ có thể giải phóng phòng đang ở trạng thái RENTED");
        }

        room.setStatus(targetStatus);
        roomRepository.save(room);
    }

    @Override
    public Map<RoomStatus, Long> countRoomsByStatus() {
        Map<RoomStatus, Long> counts = new EnumMap<>(RoomStatus.class);
        for (RoomStatus status : RoomStatus.values()) {
            counts.put(status, roomRepository.countByStatus(status));
        }
        return counts;
    }

    @Override
    public RoomSummaryResponse getRoomSummary(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return roomMapper.toRoomSummaryResponse(room);
    }

    @Override
    public BigDecimal getListedMonthlyRent(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return room.getListedMonthlyRent();
    }

    @Override
    public Short getRoomCapacity(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return room.getCapacity();
    }

    @Override
    public RoomCapacityForM04Response getRoomCapacityForM04(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return roomMapper.toCapacityResponse(room);
    }

    @Override
    public RoomRentalInfoForM04Response getRoomRentalInfoForM04(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        return roomMapper.toRentalInfoForM04Response(room);
    }

    @Override
    public List<RoomOptionForM04Response> getAvailableRooms() {
        return roomRepository.findAllByStatusOrderByRoomCodeAsc(RoomStatus.AVAILABLE)
                .stream()
                .map(roomMapper::toM04OptionResponse)
                .toList();
    }

    @Override
    public Map<Long, RoomSummaryDto> findByRoomIds(Collection<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }
        return roomRepository.findAllById(roomIds).stream()
                .map(room -> new RoomSummaryDto(room.getId(), room.getRoomCode()))
                .collect(Collectors.toMap(RoomSummaryDto::roomId, Function.identity()));
    }

    @Override
    public boolean isRoomAvailable(Long roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .orElse(false);
    }
}
