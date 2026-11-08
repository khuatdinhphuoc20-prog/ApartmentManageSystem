package com.apartment.rental.room.service;

import com.apartment.rental.room.dto.request.RoomRequest;
import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;
import com.apartment.rental.room.dto.response.RoomSummaryDto;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RoomService {

    // Khách xem và tìm kiếm phòng khả dụng
    List<RoomResponse> getAllAvailableRooms(String keyword);

    // Lọc chi tiết phòng khả dụng
    List<RoomResponse> filterAvailableRooms(Short floorNumber, BigDecimal minPrice, BigDecimal maxPrice,
                                           Short minCapacity, BigDecimal minArea, BigDecimal maxArea);

    // Khách xem chi tiết phòng AVAILABLE
    RoomResponse getRoomDetail(Long roomId);

    // Admin xem danh sách toàn bộ phòng (mọi trạng thái)
    List<RoomResponse> getAllRoomsForAdmin(String keyword, RoomStatus status);

    // Dùng chung cho Admin lấy thông tin phòng theo Id
    RoomResponse getRoomById(Long roomId);

    // Admin thêm phòng mới
    void createRoom(RoomRequest request);

    // Admin sửa thông tin phòng
    void updateRoom(RoomRequest request, Long roomId);

    // Admin đóng/mở phòng trống (AVAILABLE <-> INACTIVE)
    void toggleRoomStatus(Long roomId);

    // Inter-module - Giữ phòng (AVAILABLE -> RENTED) khi duyệt đơn / ký hợp đồng
    void reserveRoom(Long roomId);

    // Inter-module - Giải phóng phòng (RENTED -> AVAILABLE / INACTIVE) khi kết thúc hợp đồng
    void releaseRoom(Long roomId, RoomStatus targetStatus);

    // M4 - thông tin phòng phục vụ tạo hợp đồng và kiểm tra sức chứa.
    RoomCapacityForM04Response getRoomCapacityForM04(Long roomId);
    RoomRentalInfoForM04Response getRoomRentalInfoForM04(Long roomId);
    List<RoomOptionForM04Response> getAvailableRooms();

    // Tìm nhanh mã phòng theo danh sách ID.
    Map<Long, RoomSummaryDto> findByRoomIds(Collection<Long> roomIds);
}
