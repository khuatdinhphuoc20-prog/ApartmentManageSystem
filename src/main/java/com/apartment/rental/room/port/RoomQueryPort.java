package com.apartment.rental.room.port;

import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Port cung cấp dữ liệu đọc về Room cho các module khác:
 * - M01: Lấy RoomSummary cho màn hình Issue (UC36, UC37)
 * - M03: Kiểm tra phòng khả dụng và lấy giá niêm yết khi nộp/duyệt đơn (UC10, UC14)
 * - M04: Lấy sức chứa phòng để kiểm tra số người ở cùng (UC23, BR-ACC-05)
 * - M05: Lấy thông tin phòng cho biểu mẫu điện nước (UC24, UC25)
 * - M06: Đếm số lượng phòng theo trạng thái cho Dashboard
 */
public interface RoomQueryPort {

    RoomResponse getRoomDetail(Long roomId);

    RoomSummaryResponse getRoomSummary(Long roomId);

    BigDecimal getListedMonthlyRent(Long roomId);

    Short getRoomCapacity(Long roomId);

    boolean isRoomAvailable(Long roomId);

    Map<RoomStatus, Long> countRoomsByStatus();
}
