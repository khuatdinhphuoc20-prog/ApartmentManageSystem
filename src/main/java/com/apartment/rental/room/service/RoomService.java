package com.apartment.rental.room.service;

import com.apartment.rental.room.dto.request.RoomRequest;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RoomService {

    // UC04 & UC05: Khách xem và tìm kiếm phòng khả dụng
    List<RoomResponse> getAllAvailableRooms(String keyword);

    // UC05: Lọc chi tiết phòng khả dụng
    List<RoomResponse> filterAvailableRooms(Short floorNumber, BigDecimal minPrice, BigDecimal maxPrice,
                                           Short minCapacity, BigDecimal minArea, BigDecimal maxArea);

    // UC06: Khách xem chi tiết phòng AVAILABLE
    RoomResponse getRoomDetail(Long roomId);

    // UC09: Admin xem danh sách toàn bộ phòng (mọi trạng thái)
    List<RoomResponse> getAllRoomsForAdmin(String keyword, RoomStatus status);

    // Dùng chung cho Admin lấy thông tin phòng theo Id
    RoomResponse getRoomById(Long roomId);

    // UC07: Admin thêm phòng mới
    void createRoom(RoomRequest request);

    // UC07: Admin sửa thông tin phòng
    void updateRoom(RoomRequest request, Long roomId);

    // UC08: Admin đóng/mở phòng trống (AVAILABLE <-> INACTIVE)
    void toggleRoomStatus(Long roomId);

    // UC22: Inter-module - Giữ phòng (AVAILABLE -> RENTED) khi duyệt đơn / ký hợp đồng
    void reserveRoom(Long roomId);

    // UC22: Inter-module - Giải phóng phòng (RENTED -> AVAILABLE / INACTIVE) khi kết thúc hợp đồng
    void releaseRoom(Long roomId, RoomStatus targetStatus);

    // Cung cấp số liệu Dashboard cho M06
    Map<RoomStatus, Long> countRoomsByStatus();

    // Cung cấp thông tin tóm tắt phòng cho M01, M05
    RoomSummaryResponse getRoomSummary(Long roomId);

    // Lấy giá niêm yết hiện hành
    BigDecimal getListedMonthlyRent(Long roomId);

    // Lấy sức chứa tối đa của phòng cho M04 kiểm tra người ở cùng (UC23)
    Short getRoomCapacity(Long roomId);

    // Kiểm tra phòng có đang AVAILABLE không
    boolean isRoomAvailable(Long roomId);
}
