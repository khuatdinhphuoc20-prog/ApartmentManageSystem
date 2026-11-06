package com.apartment.rental.room.port;

import com.apartment.rental.room.entity.RoomStatus;

/**
 * Port cung cấp thao tác giữ và giải phóng phòng (UC22) cho các Workflow Owner:
 * - M03: Giữ phòng khi duyệt đơn online (WF-01)
 * - M04: Giữ phòng khi cho thuê trực tiếp (WF-02)
 * - M04: Giải phóng phòng khi hết hạn hợp đồng tự nhiên (WF-03)
 * - M04: Giải phóng phòng khi kết thúc sớm hoặc Admin ngừng dịch vụ (WF-06)
 */
public interface RoomOccupancyCommandPort {

    void reserveRoom(Long roomId);

    void releaseRoom(Long roomId, RoomStatus targetStatus);
}
