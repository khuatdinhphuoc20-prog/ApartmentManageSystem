# BÀN GIAO 6 MODULE V2 — USE CASE, BUSINESS RULES, WORKFLOW OWNERSHIP VÀ KẾ HOẠCH TRIỂN KHAI

> **Dự án:** Quản lý phòng trọ một tòa nhà, một Admin/chủ trọ.  
> **Nhóm:** 6 thành viên, mục tiêu 10 ngày.  
> **Stack:** Spring Boot MVC, Thymeleaf, SQL Server, sandbox thanh toán.  
> **Trạng thái:** **Bản V2 để nhóm đánh giá**, cập nhật quyết định Foundation ngày 29/10/2026; không tự thay đổi nghiệp vụ đã chốt.  
> **Nguồn đối chiếu:** [BUSINESS_SPECIFICATION.md](BUSINESS_SPECIFICATION.md) và [database_design.md](database_design.md). Bản này thay cho tên file bàn giao cũ được nhắc trong hai tài liệu nguồn.  
> **Mục đích:** Một file duy nhất cho cả **chia việc** và **hướng dẫn phối hợp**. Thành viên có thể đọc phần module mình, bảng phụ thuộc, workflow liên quan và bắt tay làm ticket.

## 0. Cách đọc và ranh giới quyết định

1. **Business Rule (BR) và 40 UC** giữ theo tài liệu nghiệp vụ gốc. Nếu V2 tóm tắt chưa đầy đủ, `BUSINESS_SPECIFICATION.md` là nguồn chuẩn.
2. **Schema** giữ theo `database_design.md`: đúng **10 bảng**; không tự thêm `payments`, `payment_attempts`, người ở cùng có tài khoản, bảng thống kê, hay lịch sử người ở cùng.
3. **Module Owner (MO)** sở hữu nghiệp vụ và **mọi thao tác ghi** đối tượng thuộc module; module khác đi qua Service/Port, không tự viết Repository để sửa bảng của MO.
4. **Workflow Owner (WO)** chịu trách nhiệm **đưa một luồng end-to-end chạy được**: thiết kế điều phối, kết nối các Port, xử lý transaction, lỗi, tích hợp và integration test. WO không được tự sao chép quy tắc và ghi dữ liệu của MO khác.
5. Các WO trong V2 là **quy ước kỹ thuật để nhóm xác nhận**, không phải thay đổi phân công 40 UC. Riêng M04 điều phối một workflow kết thúc ACTIVE sớm/đóng Room RENTED đã được chốt; một WO có thể phối hợp nhiều MO.
6. **Thứ tự nghiệp vụ lúc chạy** là bắt buộc. **Thứ tự người code** linh hoạt: provider làm trước, consumer làm trước bằng mock, hoặc cả hai làm song song. **Thứ tự ghép/test thật** có mốc riêng.
7. P0 = làm trực tiếp không chờ module khác (sau foundation); P1 = xây được bằng DTO/Port/mock; P2 = ghép service thật, transaction, bảo mật và test liên module.

### 0.1. Cấu trúc công việc: hai lớp trách nhiệm

| Lớp | Câu hỏi chịu trách nhiệm | Ví dụ |
|---|---|---|
| Module Owner | Ai giữ invariants, Entity/Repository, nghiệp vụ ghi trạng thái? | M05 sở hữu `Invoice`; M06 không được tự update `invoices`. |
| Workflow Owner | Ai bảo đảm tất cả các bước từ đầu tới cuối cùng thành công/rollback, tổ chức phối hợp và viết integration test? | M03 chịu trách nhiệm luồng duyệt đơn gồm M03+M04+M02. |
| Người điều phối dự án | Ai phê duyệt thay đổi schema/contract giao tiếp chung, merge và xử lý xung đột? | Trưởng nhóm hoặc người được phân quyền. |

**Quy tắc code:** WO viết `*Workflow`/`*Coordinator` tại khu vực đã thống nhất, gọi các Port/Service của MO. Không đặt `@Transactional` độc lập ở mọi bước rồi kỳ vọng tự rollback toàn luồng; thao tác ghi cùng SQL Server phải tham gia một transaction nghiệp vụ phù hợp. Không thiết kế constructor injection vòng tròn; tách Query Service, Command Service và Workflow Coordinator.

### 0.2. Điều kiện foundation trước khi 6 người bắt đầu

- Chốt schema bằng một `schema.sql` tự tạo database phát triển cục bộ tên `RentalApartmentDev` khi chưa có và chạy thủ công một lần trên SQL Server của mỗi thành viên, không dùng Flyway; JPA dùng `ddl-auto=validate`. Mỗi máy đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` riêng qua biến môi trường, không đưa mật khẩu database vào Git. Foundation bổ sung starter Web MVC phù hợp Spring Boot 4.1.1 trong `pom.xml`. Giữ enum, tiền `BigDecimal`/`DECIMAL(19,0)`, ngày nghiệp vụ UTC+7, thời điểm UTC, chính sách `[from,to)`.
- Trưởng nhóm dựng **tất cả 10 bảng, constraint/index và JPA Entity/relationship** trên nhánh foundation rồi merge `main` trước khi bàn giao. `seed_admin.sql` chạy thủ công một lần để tạo Admin mẫu `admin@rental.local` với hash BCrypt; M01 dùng `BCryptPasswordEncoder` để kiểm tra mật khẩu, còn mật khẩu bản rõ được chia sẻ riêng ngoài Git. Filtered unique index giới hạn tối đa một ADMIN. Đây là **điều kiện triển khai**, chỉ đánh dấu `DONE` khi script đã chạy, Entity đã được kiểm tra và Admin seed thực sự tồn tại trên database của môi trường tương ứng; nếu chưa, thành viên vẫn có thể làm DTO/service thuần/Thymeleaf mock nhưng chưa được coi là đã có Entity chạy thật.
- Sau foundation, MO duy trì Repository/Service, đề xuất thay đổi Entity/schema qua PR trưởng nhóm review. Không giao sáu người tạo lại sáu nhóm Entity vốn đã có sẵn.
- Chốt `CurrentUser`/Role, skeleton Thymeleaf, tên route/feature package, tiêu chuẩn DTO/Port, cấu hình test, dữ liệu seed ADMIN có CCCD; chốt ai duyệt thay đổi code dùng chung (`pom.xml`, `SecurityConfig`, global layout/schema SQL).
- Đã chốt: `area_m2` có thể NULL nhưng nếu có phải > 0; `floor_number` bắt buộc > 0; giá niêm yết Room và giá tháng Contract đều > 0; không đặt thời hạn thuê tối thiểu mới ngoài `planned_end_date > start_date`; email đăng nhập phân biệt hoa/thường, chỉ bỏ khoảng trắng đầu/cuối. Những việc còn cần chọn cho demo/module: cách lưu/upload ảnh phòng, cổng sandbox và dữ liệu demo kỳ ngày 10. **Không tự biến giả định thành Business Rule**.

## 1. Phạm vi và trách nhiệm của 6 người

| Module/Người | Sở hữu bảng/đối tượng | UC chính thức | Số UC | Vai trò nổi bật |
|---|---|---|---:|---|
| **M01 — Account & Issue / TV1** | `users`, `issues` | UC01–03, UC36–38 | 6 | Cung cấp Auth/CurrentUser và quản lý Issue. |
| **M02 — Room / TV2** | `rooms` | UC04–09, UC22 | 7 | Cung cấp RoomQuery, giữ/giải phóng Room; giữ quy tắc Room status. |
| **M03 — Application / TV3** | `rental_applications` | UC10–15 | 6 | WO duyệt đơn online; điều khiển vòng đời Application. |
| **M04 — Contract / TV4** | `contracts`, `contract_co_occupants` | UC16–21, UC23, UC34 | 8 | WO cho thuê trực tiếp, kết thúc Contract; cung cấp Contract snapshot. |
| **M05 — Billing & Sandbox / TV5** | `billing_cycles`, `billing_cycle_items`, `invoices` | UC24–29, UC33 | 7 | Chịu trách nhiệm trọn luồng kỳ thu/Invoice/sandbox; **duy nhất viết Invoice**. |
| **M06 — Collection, Settlement & Notification / TV6** | `notifications` | UC30–32, UC35, UC39–40 | 6 | Quyết toán, công nợ, xác nhận thu tiền qua M05, thông báo; Dashboard query. |

**Cân bằng tải:** Không đánh giá dựa thuần số UC. M04 và M05 thường nặng hơn; WO và MO phải cam kết **bàn giao năng lực nhỏ, sớm** thay vì đợi toàn bộ module hoàn thành. M05 giữ trọn sandbox, không xé lẻ cho người thiếu chuyên môn.

---

## 2. M01 — Account & Issue Management (TV1)

### 2.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm và kết quả bắt buộc | Phụ thuộc khi triển khai |
|---|---|---|---|
| **UC01** | Đăng ký Guest | **BR-ACC-07/08/09:** email duy nhất và phân biệt hoa/thường (bỏ khoảng trắng đầu/cuối, không tự đổi chữ thường); email, mật khẩu, họ tên, điện thoại, CCCD đều bắt buộc; công khai chỉ tạo `USER`, không OTP/email verification. | **P0**. |
| **UC02** | Đăng nhập/đăng xuất, phân quyền | `ADMIN` duy nhất được seed; không đăng ký role Admin; mọi endpoint nhạy cảm kiểm tra server-side. | **P0**, là đầu ra nền cho M02–M06. |
| **UC03** | Xem/cập nhật hồ sơ User | **BR-ACC-10:** sửa họ tên/CCCD hợp lệ; các màn Contract đọc User **hiện tại**, không snapshot và không đổi tenantId/giá/điều khoản. | **P0**; M04 sử dụng User query. |
| **UC36** | User gửi phản ánh/sửa chữa | **BR-ISSUE-01:** chỉ người đại diện của `Contract ACTIVE` đúng Room mới tạo `Issue OPEN`; Contract ENDED không tạo Issue mới. | **P1/P2:** M04 ContractActivityQuery; M02 RoomSummary để hiển thị. |
| **UC37** | Xem danh sách/chi tiết Issue mình | **BR-ISSUE-02/03/04:** chỉ Issue của mình, vẫn xem sau Contract ENDED. | **P0**, phần hiển thị Room là P1. |
| **UC38** | Admin cập nhật Issue | `OPEN → IN_PROGRESS → RESOLVED`; Contract ENDED không tự đóng Issue; ghi phản hồi Admin. | **P0** logic chính, **P1/MỀM** thông báo M06. |

### 2.2. M01 cần nhận từ ai / ai cần nhận từ M01

| Chiều | UC/khả năng | Hợp đồng giao tiếp tối thiểu | Tác động nếu chưa có |
|---|---|---|---|
| **M01 ← M04** | UC36 | `hasActiveContract(tenantId, roomId)` hoặc query tương đương; dữ liệu nguồn từ Contract `ACTIVE`. | Chưa nghiệm thu tạo Issue thật; có thể mock true/false. |
| **M01 ← M02** | UC36–37 | `RoomSummary(roomId, roomCode, ...)`. | Chỉ chặn phần hiển thị phòng; không chặn CRUD Issue. |
| **M01 ← M06** | UC38 | `NotificationEvent(issueId, recipientUserId, eventKey, ...)`. | Thông báo có thể tích hợp sau; lỗi không rollback Issue. |
| **M01 → M02/M03/M04/M05/M06** | Các UC cần quyền | `CurrentUser(userId, role)` và Access Policy; `UserSummary`/UserDirectory. | Các owner khác dùng mock SecurityContext/unit test, nhưng không nghiệm thu bảo mật thật khi thiếu Auth. |
| **M01 → M06** | Dashboard | `countUnresolvedIssues()` = OPEN + IN_PROGRESS. | M06 mock số tổng hợp rồi thay query thật. |

### 2.3. Bắt đầu độc lập; giải quyết phụ thuộc

- **Làm ngay:** UC01, UC02, UC03; CRUD và truy vấn UC37/38; kiểm thử quyền và dữ liệu Issue mẫu. **Ưu tiên số 1:** công bố `CurrentUser` + Role để cả nhóm không chờ.
- **UC36 / tuần tự:** M04 cung cấp ContractActivityQuery thật → M01 làm rule tạo Issue. **Song song:** M01 viết `IssueCreationService` dựa trên Port + mock các trạng thái ACTIVE/không ACTIVE; M04 viết query, ghép ở P2.
- **UC38 / tuần tự:** M06 làm NotificationService → M01 gọi. **Song song:** M01 phát event sau khi Issue cập nhật thành công; M06 viết handler. Notification là phụ thuộc mềm.
- **Tránh đè file:** TV1 là owner `SecurityConfig`, đăng ký/login controllers và `UserRepository`. M05 cung cấp **chính sách** chặn giao diện Admin kỳ thu, TV1 tích hợp tại một điểm chung; không để cả hai tự sửa SecurityConfig mà không review. Quy tắc chặn phải không tạo redirect loop khi mở biểu mẫu điện nước.

### 2.4. Ticket và thứ tự gợi ý

| Ticket | Gói việc/UC | Làm trước được? | Bàn giao/tiêu chí xong |
|---|---|---|---|
| M01-T01 | UC01/02, CurrentUser/role | P0 | Login thật; email phân biệt hoa/thường nhất quán khi đăng ký/đăng nhập; Guest không truy cập trang Admin; interface quyền cho nhóm. |
| M01-T02 | UC03, UserSummary | P0 | Hồ sơ cập nhật, M04 nhìn thấy hồ sơ mới, không đổi giá/Contract. |
| M01-T03 | UC37/38, xử lý Issue | P0 | Đúng quyền; Issue cũ còn xử lý được sau Contract ENDED. |
| M01-T04 | UC36, Contract Activity mock → thật | P1→P2 | Tạo Issue đúng phòng/chủ ACTIVE; mọi trường hợp khác bị từ chối. |
| M01-T05 | Notification Issue + Dashboard query | P1→P2 | Handler M06 nhận được sự kiện; query unresolved dùng được. |

**Definition of Done:** Đăng ký yêu cầu CCCD; Auth/role dùng được toàn hệ thống; UC36 không chỉ kiểm tra bằng UI; lỗi Notification không phá Issue; có test quyền người dùng khác.

---

## 3. M02 — Room Management (TV2)

### 3.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm và kết quả bắt buộc | Phụ thuộc khi triển khai |
|---|---|---|---|
| **UC04** | Danh sách phòng công khai | **BR-ROOM-01/02/03:** chỉ `AVAILABLE`, không lộ `RENTED/INACTIVE`. | **P0**. |
| **UC05** | Lọc/tìm phòng | Chỉ phòng AVAILABLE, hỗ trợ diện tích và tầng theo **BR-ROOM-09**; `area_m2` NULL là chưa có dữ liệu, không dùng số 0 giả. | **P0**. |
| **UC06** | Chi tiết phòng | Mô tả, sức chứa, diện tích, tầng, giá, **một ảnh đại diện**; không gallery/Amenity Entity. | **P0**. |
| **UC07** | Admin thêm/sửa phòng | **BR-ROOM-08/09:** `floor_number` bắt buộc > 0, `area_m2` tùy chọn nhưng nếu có phải > 0, giá niêm yết > 0; chỉ đổi giá khi không có Contract SCHEDULED/ACTIVE; Contract giữ giá cũ; không giảm capacity dưới số người hiện tại. | **P0** trường cơ bản, **P1/P2** M04 ContractQuery và occupancy count. |
| **UC08** | Mở/đóng phòng | AVAILABLE↔INACTIVE khi trống; **BR-ROOM-04/06:** RENTED→INACTIVE phải hủy SCHEDULED hoặc kết thúc ACTIVE và quyết toán. | **P0** nhánh trống; **P1/P2 CỨNG** M04/M06/M05. |
| **UC09** | Danh sách phòng Admin | Admin thấy AVAILABLE/RENTED/INACTIVE. | **P0**, quyền do M01. |
| **UC22** | Đồng bộ giữ/giải phóng phòng | **BR-ROOM-02/07:** SCHEDULED đã giữ phòng RENTED; không hai Contract active/scheduled chiếm cùng Room; hết hợp đồng giải phóng đúng nhánh. | **P1/P2 CỨNG** M04, một transaction workflow. |

### 3.2. Phụ thuộc hai chiều

| Chiều | UC liên quan | Phải cung cấp/nhận | Chiến lược làm việc |
|---|---|---|---|
| **M02 ← M04** | UC07 | Kiểm tra Contract SCHEDULED/ACTIVE, số người Contract. | Mock rồi ghép query thật; kiểm tra lần cuối **trong transaction** khi sửa giá/capacity. |
| **M02 ← M04/M06/M05** | UC08 | Kết quả hủy/kết thúc Contract và khoản quyết toán. | Chạy nhánh INACTIVE khi trống trước; phần RENTED mock TerminationResult, chỉ nghiệm thu khi bốn module ghép thật. |
| **M02 → M03** | UC10/14 | RoomAvailabilityQuery; khi APPROVE phải kiểm tra lại Room, không dùng trạng thái lúc gửi đơn. | Cấp DTO/mẫu ngay cho M03; ghép khóa/phát hiện cạnh tranh. |
| **M02 → M04** | UC16/17/19/21/34 | Giá Room tại lúc tạo Contract; `reserveRoom/releaseRoom`; `closeRoom` có điều kiện. | Bàn giao Port + contract lỗi từ đầu; M04 mock để phát triển. |
| **M02 → M05** | UC24/25 | RoomSummary, roomCode/phân nhóm phòng hiển thị. | M05 mock danh sách trước. |
| **M02 → M01/M06** | UC36/37, Dashboard | RoomSummary và `countRoomsByStatus`. | Read DTO, không đưa RoomRepository cho bên ngoài. |

### 3.3. Công việc độc lập và hướng giải quyết

- **P0:** UC04, UC05, UC06, UC09; phần thêm/sửa Room của UC07, nhánh AVAILABLE↔INACTIVE của UC08; test query Room và trạng thái.
- **UC07 / tuần tự:** đợi M04 ContractQuery rồi bật điều kiện đổi giá/capacity. **Song song:** M02 viết form và validation với ContractQuery stub; M04 cung cấp read DTO sau; P2 kiểm tra dữ liệu thật và concurrency.
- **UC22 / tuần tự:** M02 cung cấp reserve/release thật → M04 tích hợp. **Song song:** hai bên cùng chốt Port + lỗi như `ROOM_NOT_AVAILABLE`, M02 test command thật, M04 mock command. M02 **không tạo/hủy Contract**.
- **UC08 / tuần tự:** M04/M06/M05 hoàn thiện kết thúc và quyết toán → M02 nối hành động đóng. **Song song:** M02 hoàn thành giao diện quản lý Room, mock `TerminationResult` và xây bộ test; P2 ghép WO kết thúc. Không cho `status=INACTIVE` như một UPDATE tự do khi RENTED.

### 3.4. Ticket và thứ tự gợi ý

| Ticket | Gói việc/UC | Làm trước? | Điều kiện xong |
|---|---|---|---|
| M02-T01 | UC07/09 CRUD Admin + RoomQuery | P0 | Room lưu/hiển thị; chưa cho phép quy tắc nhạy cảm bị bỏ qua. |
| M02-T02 | UC04–06 public/filters | P0 | Public chỉ AVAILABLE; một cover path có ảnh hợp lệ khi demo. |
| M02-T03 | UC22 Port và reserve/release | P1 | Có test cạnh tranh và status đồng bộ khi M04 gọi. |
| M02-T04 | UC07 đổi giá/capacity phụ thuộc M04 | P1→P2 | Chặn đổi giá khi có SCHEDULED/ACTIVE, không giảm sức chứa dưới occupancy. |
| M02-T05 | UC08 (trống), sau đó RENTED closure | P0→P2 | Sau kết thúc, Room AVAILABLE/INACTIVE đúng reason; không bỏ quyết toán. |
| M02-T06 | Room count cho Dashboard, integration tests | P1→P2 | M03/M04/M05/M06 dùng Query và Command thật, không đọc trực tiếp Repo. |

**Definition of Done:** không hai hợp đồng giữ cùng phòng; không `INACTIVE` cùng Contract SCHEDULED/ACTIVE; không public phòng không cho thuê; thay đổi giá đúng quy tắc.

---

## 4. M03 — Rental Application (TV3)

### 4.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm | Phụ thuộc |
|---|---|---|---|
| **UC10** | Tạo đơn PENDING đúng một Room | **BR-APP-01/02/08/09:** phải đăng nhập, Room AVAILABLE; một PENDING/user–room; ngày bắt đầu hôm nay..+10, đơn quá ngày bị REJECTED, không tự dời ngày. | **P1** M01/M02. |
| **UC11** | Xem đơn bản thân | Chỉ chính User thấy đơn; APPROVED là lịch sử kể cả Contract đã ENDED — **BR-APP-06**. | **P0** (Auth nền). |
| **UC12** | User rút đơn | **BR-APP-03:** chỉ người gửi, chỉ khi PENDING; chuyển WITHDRAWN. | **P0**. |
| **UC13** | Danh sách/filter Admin | Admin quyền duyệt/xem tất cả; xác định trạng thái PENDING rõ. | **P0**. |
| **UC14** | Admin REJECT/APPROVE | **BR-APP-04/07:** APPROVED chỉ khi Contract tạo/Room giữ thành công; giá Contract lấy Room hiện hành, đơn không chốt giá. | **P0** REJECT; **P1/P2 CỨNG** M04+M02 cho APPROVE. |
| **UC15** | Reject PENDING cạnh tranh | **BR-APP-05:** phải chạy sau cả duyệt online lẫn thuê trực tiếp; không REJECT đơn đã rời PENDING. | **P0** command, **P2** trigger từ M04. |

### 4.2. Trách nhiệm **Workflow Owner**: duyệt đơn online

- **WO: M03**, MO cung cấp: M04 tạo Contract, M02 giữ Room, M03 thay trạng thái Application.
- **Trình tự runtime:** recheck Application PENDING + requestedStartDate còn hợp lệ → lock/recheck Room AVAILABLE → M04 tạo Contract đúng User/Room/ngày/giá hiện hành → M02 giữ Room → M03 APPROVED và REJECT các PENDING cạnh tranh → commit. Lỗi bất kỳ bước nào rollback hoặc không ghi APPROVED.
- **Không tạo vòng dependency:** `ApproveApplicationWorkflow` ở M03 gọi `ContractCreationPort` M04; `ContractCreationPort` không inject ngược `ApproveApplicationWorkflow`. M04 dùng `ApplicationQuery` M03 ở service đọc độc lập nếu cần.
- **Chống đồng thời:** kiểm tra trạng thái lại trong transaction; một unique filtered index SQL Server trên `contracts(room_id)` cho cả `SCHEDULED`/`ACTIVE` và unique PENDING per User–Room là lớp bảo vệ cuối. Entity `Contract` không khai báo `room_id` UNIQUE toàn bảng vì phải giữ nhiều Contract lịch sử; nếu index từ chối do phòng đã được giữ, workflow rollback và trả lỗi phù hợp. Không tin dữ liệu đã render trên trang Admin.

### 4.3. Phụ thuộc hai chiều và chiến lược

| Chiều | UC | Cần trao đổi | Tuần tự | Song song |
|---|---|---|---|---|
| **M03 ← M01** | UC10–14 | CurrentUser/Role | M01 Auth trước. | M03 mock principal; test quyền thật khi ghép. |
| **M03 ← M02** | UC10/14 | Room AVAILABLE + reserve trong workflow | M02 Query/Command trước. | M03 mock RoomQuery/Occupancy; M02 làm Port thật. |
| **M03 ← M04** | UC14 | `createFromApplication` + Result/Failure, giá lấy Room hiện hành | M04 xong creation rồi M03 duyệt. | M03 mock Success/RoomBusy/InvalidDates, M04 làm service thật; ghép transaction P2. |
| **M03 → M04** | UC16/17 | ApplicationQuery + `rejectOtherPending(roomId, excludedApplicationId)` | M03 command trước. | M04 mock ApplicationCommand; M03 cung cấp implementation sau. |
| **M03 → M06** | UC14/15 | Event kết quả xử lý đơn | Notification sau. | Hai bên chốt payload, M06 handler độc lập. |

### 4.4. Ticket và thứ tự gợi ý

| Ticket | Gói việc | Giai đoạn | Tiêu chí bàn giao |
|---|---|---|---|
| M03-T01 | UC11/12/13: quyền và vòng đời đơn | P0 | Withdraw chỉ PENDING của mình; Admin xem danh sách. |
| M03-T02 | UC10: tạo đơn, lịch và chống PENDING trùng | P1 | Không gửi Room khác AVAILABLE; một PENDING/User–Room. |
| M03-T03 | UC14 REJECT + UC15 rejectOthers command | P0 | Command cho M04 dùng được; không sửa các đơn đã xử lý. |
| M03-T04 | UC14 APPROVE coordinator + mock M04/M02 | P1 | Unit test success/failure; không APPROVED giả. |
| M03-T05 | Ghép M04/M02, transaction và race tests | P2 | Một Contract/Room; REJECT cạnh tranh; callback UI đúng kết quả. |
| M03-T06 | Scheduler đơn quá ngày + thông báo M06 | P1→P2 | Ngày hôm qua bị REJECTED có note; không tự đổi ngày. |

**Definition of Done:** có hai luồng online APPROVE/REJECT; Direct Rental từ M04 cũng gọi UC15; không có Application APPROVED mồ côi.

---

## 5. M04 — Contract Management (TV4)

### 5.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm | Phụ thuộc |
|---|---|---|---|
| **UC16** | Tạo Contract từ Application | **BR-CON-01/02/08/09:** start hôm nay..+10, end ngày 10 tháng chọn, một người đại diện/Room, giá snapshot Room lúc tạo và > 0; chỉ APPROVE sau thành công. | **P1/P2 CỨNG** M03, M02. |
| **UC17** | Tạo Contract trực tiếp | **BR-CON-06/09:** tenant đã có User/CCCD, giá lấy từ Room hiện hành và > 0, không backdate, không Application giả; reject PENDING cạnh tranh. | **P1/P2 CỨNG** M01, M02, M03. |
| **UC18** | Xem Contract | Tenant chỉ thấy Contract của mình; giá và plannedEndDate bất biến; hồ sơ người đại diện là User hiện tại. | **P0**, UserQuery M01. |
| **UC19** | Hủy SCHEDULED | **BR-CON-05:** trước ngày bắt đầu, CANCELLED, không tiền thuê kỳ cuối, release Room. | **P1/P2** M02. |
| **UC20** | Scheduler SCHEDULED→ACTIVE | **BR-CON-03/11:** đúng startDate, kể cả no-show, không tự hủy; Room vẫn RENTED. | **P0/P1**, M02 nếu cần query đồng bộ. |
| **UC21** | Scheduler ACTIVE→ENDED | **BR-CON-04/12**, **BR-TERM-07:** hết hạn tự nhiên, Room AVAILABLE, không gia hạn; M05 vẫn thu được kỳ cuối. | **P1/P2** M05, M02. |
| **UC23** | Admin quản lý người ở cùng | **BR-ACC-03/04/05/06/08:** chỉ Admin; tên/CCCD bắt buộc, không trùng CCCD trong Contract, không vượt capacity; không sửa sau ENDED/CANCELLED, không User riêng. | **P1** M02 capacity. |
| **UC34** | Kết thúc ACTIVE trước hạn | **BR-CON-10/13, BR-TERM-01..09:** effectiveEndDate là hôm nay UTC+7; reason quyết định cách tính; biên ngày 10 có ngoại lệ; kết thúc không cần Invoice đã PAID. | **P1/P2 CỨNG** M06, M05, M02. |

### 5.2. Trách nhiệm **Workflow Owner**

1. **Direct Rental (UC17): M04**. Runtime: kiểm tra User/ngày → khóa và kiểm tra lại Room AVAILABLE, giá hiện hành > 0 → M04 tạo Contract SCHEDULED/ACTIVE phù hợp → M02 giữ Room → M03 reject PENDING cạnh tranh, trong một transaction; không tạo Application giả. Nếu unique filtered index chặn vì phòng đã được giữ, rollback và báo xung đột; không tạo logic/Entity mới để thay index.
2. **Cancel SCHEDULED (UC19): M04**. CANCELLED và M02 release/INACTIVE theo yêu cầu Admin trong cùng nghiệp vụ; không gọi M06 tính kỳ cuối.
3. **Natural Expiry (UC21): M04**. Contract ACTIVE→ENDED ngày 10 và Room AVAILABLE; phải bảo toàn dữ liệu/nhận diện nghĩa vụ REGULAR kỳ vừa ở để M05 không bỏ qua khi scheduler có thứ tự chạy khác.
4. **ACTIVE Early Termination (UC34): M04**. Lấy snapshot Contract, xác định reason/khung thời gian; M06 **chỉ tính chính sách quyết toán**, M05 **ghi Invoice hoặc NO_CHARGE**, M04 lưu kết quả và ENDED, M02 đổi Room. M04 là đầu mối điều phối end-to-end; không tự tính tiền, không tự ghi Invoice.
5. **Nếu M02 nhận yêu cầu đóng phòng RENTED (UC08):** UI/controller M02 gọi **cùng workflow termination của M04** với reason `ADMIN_STOP_SERVICE` và đích Room `INACTIVE`; không tự tạo luồng kết thúc thứ hai. Quy ước phối hợp này đã được chốt; chi tiết Port/DTO và thứ tự khóa cần thống nhất khi tích hợp module.

### 5.3. Phụ thuộc hai chiều

| Chiều | UC | Dữ liệu/command | Phương án triển khai |
|---|---|---|---|
| **M04 ← M01** | UC16/17/18 | UserSummary + role; CCCD đã bắt buộc từ đăng ký. | Mock UserSummary trước, Auth thật P2. |
| **M04 ← M02** | UC16/17/19/21/23/34 | Giá Room hiện hành, capacity, reserve/release/close. | M04 viết coordinator với RoomOccupancyPort mock; M02 làm command thật; ghép transaction. |
| **M04 ← M03** | UC16/17 | ApplicationQuery; reject pending sau direct. | M04 mock Application và command, M03 viết service thật. |
| **M04 ← M06** | UC34 | `SettlementCalculation(snapshot, reason, electricity, water)` → amount/rule. | M04 mock kết quả; M06 viết máy tính thuần/test ngày 10; P2 gắn thật. |
| **M04 ← M05** | UC21/34 | Query kỳ cuối, issueFinalInvoice/NO_CHARGE, result + conflict. | M04 mock InvoiceCommand; M05 cung cấp implementation chống trùng. |
| **M04 → M01** | UC36 | `hasActiveContract(userId, roomId)`. | Công bố Query sớm, có dữ liệu mẫu. |
| **M04 → M03** | UC14 | `createFromApplication`/Result. | Trả failure rõ ràng, không để M03 tự ghi Contract. |
| **M04 → M05** | UC24/26 | `ContractBillingSnapshot(contractId, tenantId, roomId, startDate, plannedEndDate, agreedMonthlyRent, status, ...)`. | Ưu tiên số 1 sau Contract basics; M05 mock theo cấu trúc này. |
| **M04 → M06/M02** | UC34, UC08 | Settlement snapshot, trạng thái và kết quả kết thúc. | DTO thuần, tránh inject ngược coordinator làm vòng phụ thuộc. |

### 5.4. Làm độc lập và chiến lược

- **P0:** validator start `today..today+10`, end ngày 10 và sau start; giá Contract lấy từ Room, > 0 và bất biến; ContractQuery; UC18; UC20; logic validation người ở cùng UC23. Giao `ContractBillingSnapshot` cho M05 ngay.
- **UC16/17 / tuần tự:** M02/M03 service thật trước → M04 tích hợp. **Song song:** M04 viết Contract creator + WO direct với Room/Application mock; M02/M03 viết Port; P2 transaction chung.
- **UC19/21 / tuần tự:** M02 release, M05 kỳ cuối sẵn sàng. **Song song:** M04 xây transition theo Clock giả và Result mock; M02/M05 xây query/command tương ứng; P2 test chạy scheduler đảo thứ tự.
- **UC34 / tuần tự:** M06 tính, M05 issue, M02 release trước; M04 điều phối cuối. **Song song:** M04 xây workflow step/result/rollback trên Port mock; M06 tập trung pure calculator, M05 issueFinalInvoice, M02 release; ghép cả bốn trước nghiệm thu.
- **Không được làm:** thay `planned_end_date` để gia hạn; cập nhật Invoice/RoomRepository trực tiếp; backdate; end ACTIVE mà bỏ khoản phải thu hoặc bỏ dấu NO_CHARGE.

### 5.5. Ticket và thứ tự gợi ý

| Ticket | Công việc | Giai đoạn | Bàn giao |
|---|---|---|---|
| M04-T01 | Validator, ContractQuery, UC18 | P0 | ContractBillingSnapshot cho M05; ContractActivityQuery cho M01. |
| M04-T02 | UC20 Scheduler và date test | P0 | SCHEDULED tự ACTIVE dù no-show; idempotent. |
| M04-T03 | UC23 + capacity query | P1 | Không trùng CCCD, không vượt room.capacity, không sửa Contract ENDED. |
| M04-T04 | UC16/17 create workflow với mocks | P1 | M03/M02 có thể tích hợp Port thật sau. |
| M04-T05 | UC19/21 cancel/expire | P1→P2 | Contract/Room nhất quán; kỳ cuối không bị bỏ sót. |
| M04-T06 | UC34 + nhánh đóng phòng UC08 | P1→P2 | Kết thúc sớm đúng reason, Invoice/NO_CHARGE, Room đúng và rollback. |
| M04-T07 | Kết nối tất cả provider + concurrency tests | P2 | Chống giữ phòng kép, không backdate/duplicate settlement. |

**Definition of Done:** các lần thuê độc lập, không hợp đồng active/scheduled đè nhau; tất cả nhánh kết thúc đúng và không làm mất công nợ; coordinator M04 được gọi từ đúng nơi, không logic Contract lặp ở M02/M03.

---

## 6. M05 — Billing & Online Payment (TV5, người biết sandbox)

### 6.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm | Phụ thuộc |
|---|---|---|---|
| **UC24** | Scheduler tạo/kiểm tra BillingCycle ngày 10 | **BR-BILL-01/02/08:** cycle10; thu sau khi ở; chạy lại/restart/login không tạo kỳ/item trùng. | **P1** M04/M02. |
| **UC25** | Admin nhập điện+nước | **BR-BILL-05/06/19:** nhập **số tiền** chứ không đọc công tơ/tự nhân giá; nhập đủ mọi nghĩa vụ kỳ; `NULL` chưa nhập, `0` đã nhập 0; khóa giao diện Admin thông thường đến khi đủ. | **P1/P2** M01 role/guard, M04/M02 tập phòng/hợp đồng. |
| **UC26** | Tính tiền/phát hành Invoice | **BR-BILL-03/04/07/18:** dưới 14 ngày kỳ đầu prorate `monthly/30*days` HALF_UP cuối; kỳ khác tháng đầy đủ; giá Contract; tổng 0→NO_CHARGE; không Invoice trùng. | **P1** M04 ContractBillingSnapshot; Notification M06 mềm. |
| **UC27** | Tenant xem Invoice mình | Chỉ người đại diện, gồm UNPAID/OVERDUE/PAID; lịch sử Contract đã kết thúc vẫn đọc được. | **P0/P1** M01. |
| **UC28** | Tenant bắt đầu sandbox | **BR-BILL-09/10/11:** đúng chủ, Invoice `UNPAID`, now `< dueAt`, trả toàn bộ, không mở sandbox quá hạn. | **P0/P1** với Invoice mẫu + gateway. |
| **UC29** | Xác minh callback/giao dịch | **BR-BILL-13/14/21/23:** xác minh server-side, chống lặp; thất bại giữ UNPAID; callback thành công muộn/xung đột để Admin đối chiếu, không ghi đè PAID. | **P1/P2** gateway, M06 Notification mềm. |
| **UC33** | Lịch sử thanh toán | **BR-BILL-16:** danh sách `Invoice PAID`, không có Payment/Attempt Entity; Tenant xem của mình, Admin được phép tra theo quyền. | **P0** với Invoice mẫu. |

### 6.2. Trách nhiệm **Workflow Owner**: ngày 10 → Invoice → sandbox

- **WO: M05**, không chia cho M06. M05 xử lý `BillingCycle`, `BillingCycleItem`, `Invoice`, form điện nước, xác minh sandbox và cập nhật PAID qua adapter riêng.
- **Trình tự runtime định kỳ:** Scheduler ngày 10 bảo đảm cycle/items duy nhất → Admin đăng nhập, hệ thống buộc hoàn thành điện+nước **tất cả nghĩa vụ** → M05 tính rent từ giá Contract → phát hành Invoice UNPAID hoặc ghi NO_CHARGE → commit → M06 nhận sự kiện thông báo → Tenant xem/trả Invoice bằng sandbox nếu hợp lệ → gateway xác minh và M05 cập nhật PAID một lần.
- **Dữ liệu M04 bắt buộc:** Contract đã ENDED đúng ngày 10 vẫn phải được tính vào nghĩa vụ kỳ vừa sử dụng, không chỉ query `status=ACTIVE` ở thời điểm scheduler chạy.
- **Nhánh Tenant trả sớm đúng ngày 10 sau kỳ sử dụng:** kỳ cũ do REGULAR xử lý; không tạo kỳ mới 0 ngày hoặc Invoice EARLY_SETTLEMENT; có thể chờ Admin chốt điện nước dù Contract đã ENDED; không sửa Invoice đã phát hành.
- **PAID** chứa `paidAt`, `paymentMethod` mới chỉ CASH/BANK_TRANSFER (VNPAY ghi BANK_TRANSFER), tham chiếu nếu có. Giữ SANDBOX chỉ để đọc Invoice cũ; không lưu lịch sử attempt riêng; callback cùng dữ liệu/điều kiện idempotent.
- **Overdue:** `dueAt = issuedAt + 48h`; M06 sở hữu UC31 nhưng **chỉ M05 ghi Invoice OVERDUE** qua command; thanh toán muộn cần Admin đối chiếu/thu ngoài đời theo rule.

### 6.3. Phụ thuộc hai chiều và giao tiếp M05–M06 (giao sớm)

| Chiều | UC | Nội dung giao tiếp | Làm song song / tuần tự |
|---|---|---|---|
| **M05 ← M04** | UC24/26 | `ContractBillingSnapshot` giá đã chốt + khoảng sử dụng, gồm Contract vừa kết thúc. | Song song bằng snapshot mock; tuần tự khi Query M04 có sớm. |
| **M05 ← M02** | UC24/25 | RoomSummary / roomCode; phân loại phòng cần hiển thị. | Mock danh sách phòng; ghép Query. |
| **M05 ← M01** | UC25/27/28 | Admin/Tenant/Invoice owner; điểm tích hợp mandatory billing gate. | M05 viết `MandatoryBillingGatePolicy`; M01/đầu mối config tích hợp guard, không cùng sửa SecurityConfig. |
| **M05 ← M06** | UC26/29 | Notification event listener; UC35 cung cấp số tiền quyết toán lý do kết thúc. | Notification làm sau; tính quyết toán mock khi viết issueFinalInvoice. |
| **M05 → M06** | UC30 | `confirmManualPayment(invoiceId, adminId, method, ...)`, chỉ UNPAID/OVERDUE→PAID đủ tiền. | M05 công bố Port sớm; M06 mock trước; M05 ghi thật. |
| **M05 → M06** | UC29/30 | `confirmVerifiedSandboxPayment` với chứng cứ gateway đã xác minh; dữ liệu mới dùng BANK_TRANSFER khi Admin đối chiếu. | Mock success/conflict rồi ghép M05 thực. |
| **M05 → M06** | UC31/32 | `markOverdueInvoices(now)` và `findOutstandingInvoices(filter)`. | M06 mock InvoiceSummary; M05 làm Query/Command. |
| **M05 → M06/M04** | UC35/34 | `issueFinalInvoice(request)`→ `INVOICED(invoiceId)` hoặc `NO_CHARGE`; khóa chống trùng Contract+mốc kỳ. | M04/M06 mock result; ghép thật khi test termination. |
| **M05 → M06** | Dashboard | `getInvoiceTotals` tổng PAID, số UNPAID/OVERDUE. | M06 mock aggregate. |
| **M05 → M06** | Events | InvoiceIssued/InvoicePaid/InvoiceOverdue sau commit. | Handler M06 làm độc lập; lỗi không rollback tiền. |

**Lưu ý quyết định kỹ thuật:** `issueFinalInvoice` phải phát hiện xung đột khi cùng Contract + `billingPeriodEndDate` nhưng số tiền/khoảng sử dụng khác; không ghi đè Invoice. M05 sở hữu một repository duy nhất của Invoice. M06 **không** triển khai `InvoiceRepository` riêng.

### 6.4. Công việc độc lập và ticket

| Ticket | Công việc | Giai đoạn | Tiêu chí bàn giao |
|---|---|---|---|
| M05-T01 | Invoice DTO/Query/Command ports công bố cho M06/M04 | P0 | M06 viết mock được, status/error rõ. |
| M05-T02 | UC27/33 với Invoice demo | P0 | Đúng quyền, lịch sử PAID không cần Payment entity. |
| M05-T03 | UC28/29 sandbox với Invoice giả | P0/P1 | Thành công, thất bại, callback lặp và xác minh server-side. |
| M05-T04 | UC24/25 cycle10 + Admin form | P1 | Kỳ/item duy nhất, hard gate và dữ liệu nhập bền khi phát hành lỗi. |
| M05-T05 | UC26 tính tiền/phát hành, NO_CHARGE | P1 | Test <14 ngày, >=14 ngày, giá Contract, HALF_UP, total=0, retry. |
| M05-T06 | InvoiceCommands cho M06 và M04 | P1→P2 | Manual paid, overdue, sandbox reconciliation, final invoice idempotent. |
| M05-T07 | Ghép M04/M02/Auth, sandbox Invoice thật | P2 | Kỳ→Invoice→sandbox→PAID; không trùng khi scheduler chạy lại. |
| M05-T08 | Biên ngày 10, cạnh tranh callback/thu thực, events M06 | P2 | Không tạo Invoice kỳ mới giả, callback không ghi PAID hai lần. |

**Definition of Done:** M05 chạy trọn luồng phát hành/sandbox; M06 được dùng Query/Command thật; dữ liệu Invoice không bị module khác ghi; không có mock gateway ở demo cuối.

---

## 7. M06 — Collection, Settlement & Notification (TV6)

### 7.1. UC → nghiệp vụ → phụ thuộc

| UC | Phải xây dựng | BR trọng tâm | Phụ thuộc |
|---|---|---|---|
| **UC30** | Admin xác nhận đã nhận tiền trực tiếp hoặc đối chiếu giao dịch VNPAY | **BR-BILL-12/20/21/23:** đúng số tiền trên Invoice; UNPAID/OVERDUE→PAID; đã PAID không ghi đè; dữ liệu mới chỉ CASH/BANK_TRANSFER. | **P1/P2 CỨNG** M05 InvoiceCommand. |
| **UC31** | Phát hiện quá hạn | **BR-BILL-11/17:** từ `dueAt` chưa trả trở thành OVERDUE; không giả PAID; chạy lại an toàn. | **P1/P2 CỨNG** M05. |
| **UC32** | Admin lọc công nợ | Bao gồm UNPAID/OVERDUE, cả hợp đồng ENDED; không cộng PAID vào công nợ. | **P1** M05 InvoiceQuery. |
| **UC35** | Tính quyết toán kỳ cuối | **BR-TERM-01..09:** Tenant trả trong kỳ thu nguyên kỳ + điện nước; Admin stop prorate theo số ngày + điện nước; Tenant trả đúng biên ngày 10 sau kỳ ở dùng REGULAR kỳ cũ; total=0→NO_CHARGE. | **P1/P2 CỨNG** M04/M05/M02. |
| **UC39** | Admin tạo thông báo thủ công | **BR-NOTI-01/05/06:** chỉ Admin gửi; một bản/recipient; thông báo trong website, không SMS/email. | **P0**, Auth M01. |
| **UC40** | Xem/đánh dấu đọc thông báo | **BR-NOTI-03/05:** chỉ người nhận, UNREAD→READ, không xem người khác. | **P0**, Auth M01. |

### 7.2. Vai trò của M06 trong các workflow

- **Collection UC30, Overdue UC31, Debt Query UC32:** M06 là owner giao diện/quy trình Admin/Scheduler, **M05 là owner ghi trạng thái Invoice**. Không cần giữ transaction ở M06 nếu chỉ gọi một InvoiceCommand đã có transaction nguyên tử ở M05.
- **Settlement UC35:** M06 sở hữu **máy tính quyết toán thuần**, nhận `ContractSettlementSnapshot`, reason và số tiền điện/nước; trả `SettlementCalculationResult` gồm rule/khoảng ngày/số tiền. **M04 là WO kết thúc ACTIVE**, gọi M06 tính rồi M05 issue, sau đó M04 end và M02 release trong transaction; cách này tránh M06 phải inject ngược chính TerminationWorkflow của M04.
- **Notification UC39/40 và event handlers:** M06 là owner `Notification`; các module gửi event sau commit; event có `eventKey`, recipient để chống trùng. Nếu lưu thông báo tự động lỗi, log/retry có giới hạn; **chưa có outbox nên không hứa bảo đảm giao 100% khi server dừng đột ngột**.
- **Dashboard Admin:** công việc tích hợp phụ thuộc đọc: M02 room counts, M05 invoice sum/count, M01 issues count; **không tạo UC/Entity Dashboard mới**.

### 7.3. Phụ thuộc hai chiều

| Chiều | UC | Cần/cung cấp | Tuần tự | Song song |
|---|---|---|---|---|
| **M06 ← M05** | UC30/31/32 | InvoiceQuery + InvoiceCommand | M05 làm Port thật trước. | M06 viết UI, scheduler/validation với fake Invoice DTO/commands; ghép thật. |
| **M06 ← M04** | UC35 | ContractSettlementSnapshot, reason, ngày kết thúc UTC+7. | M04 Query trước. | M06 viết pure calculator bằng fixture Contract; M04 build coordinator riêng. |
| **M06 ← M02** | UC35/Dashboard | roomId, mã phòng và count status. | M02 query trước. | Mock RoomSummary/Aggregates. |
| **M06 ← M01** | UC39/40 | CurrentUser, recipients và quyền ADMIN. | M01 Auth trước. | Test với mock principal, ghép role thật. |
| **M06 → M04** | UC34 | `calculateSettlement(...)` + Result/Failure. | M06 code calculator trước. | M04 mock calculator; M06 viết test rule, ghép sớm. |
| **M06 → M02** | UC08 | Kết quả quyết toán qua workflow Termination M04. | M04 gọi M06 khi đã kết nối. | M02 mock `TerminationResult`, M04/M06 ghép sau. |
| **M06 → M01/M03/M04/M05** | Event notification | `NotificationEvent(eventKey, recipient, data)` và handler. | M06 handler trước. | Producer phát event và test không phụ thuộc handler thật. |
| **M06 ← M01/M02/M05** | Dashboard | Issue count, Room count, PAID SUM/Outstanding count. | Các Query xong trước. | Dashboard mock aggregate rồi thay thật; không đọc Repo owner khác. |

### 7.4. Ticket và thứ tự gợi ý

| Ticket | Công việc | Giai đoạn | Tiêu chí bàn giao |
|---|---|---|---|
| M06-T01 | UC39/40 Notification manual & read | P0 | Chỉ Admin gửi, người nhận chỉ xem của mình. |
| M06-T02 | UC35 `SettlementCalculator` pure logic | P0/P1 | Test TENANT_EARLY_FULL, ADMIN_STOP_PRORATED, biên 10, tổng 0. |
| M06-T03 | UC32 Debt UI, Dashboard UI bằng mock | P1 | UNPAID/OVERDUE đúng, contract ENDED vẫn có nợ. |
| M06-T04 | UC30 manual paid và sandbox reconcile bằng mock | P1 | Không xác nhận trùng; method và quyền chính xác. |
| M06-T05 | UC31 scheduler overdue bằng mock | P1 | now>=dueAt, không ghi đè PAID, chạy lại an toàn. |
| M06-T06 | Ghép Invoice Ports M05 và Contract snapshot M04 | P2 | Các Query/Command thật, không Repo Invoice ở M06. |
| M06-T07 | Ghép UC35 vào TerminationWorkflow M04/M02/M05 | P2 | End→Invoice/NO_CHARGE→Room hợp lệ, không mất công nợ. |
| M06-T08 | Events và Dashboard thật, retry/dedupe | P2 | Notification lỗi không rollback Invoice/Contract; số liệu dashboard đúng. |

**Definition of Done:** M06 không tự ghi Invoice; tính đúng số tiền theo reason; Admin có thể tìm nợ khi Contract ENDED; thông báo có bảo vệ người nhận và không phá transaction tiền.

---

## 8. Sổ tay Workflow Owner — thứ tự **runtime** và trách nhiệm ghép code

> Các trình tự dưới đây mô tả điều kiện và kết quả **khi chương trình thực sự chạy**. Người code các bước được phép làm **khác thứ tự** bằng mock, miễn cùng thống nhất Port và khi nghiệm thu không còn mock ở nhánh nghiệp vụ thật.

### WF-01 — Duyệt đơn online (UC14 + UC16 + UC22 + UC15)

| Vai trò | Người | Nhiệm vụ cụ thể |
|---|---|---|
| **WO** | **M03** | Xây `ApproveApplicationWorkflow`, nút APPROVE, transaction, xử lý lỗi, test end-to-end. |
| **MO** | M03 | Kiểm tra/cập nhật Application và reject pending cạnh tranh. |
| **Provider** | M02 | Recheck/lock Room AVAILABLE, giữ Room thành RENTED. |
| **Provider** | M04 | Tạo Contract một Room–User, snapshot giá niêm yết hiện hành, đúng ngày. |

**Bước runtime:**

1. Admin có quyền, Application còn PENDING và `requested_start_date >= today` UTC+7, không tự sửa ngày.
2. Khóa/kiểm tra Room còn AVAILABLE, lấy **giá hiện tại > 0**, chống hai thao tác Admin cùng lúc.
3. M04 tạo Contract từ Application, startDate/endDate hợp lệ, status SCHEDULED hoặc ACTIVE.
4. M02 giữ Room RENTED, M03 đổi Application thành APPROVED và từ chối các PENDING cùng Room; ghi processedAt/by.
5. Commit nguyên tử; phát Notification sau commit. Nếu bất kỳ điều kiện/ghi nào thất bại, kể cả unique filtered index từ chối Contract thứ hai cùng giữ Room, rollback và không để lại APP APPROVED nhưng thiếu Contract/Room.

**Code trước–sau:** M03 có thể viết UI, state transition và coordinator dựa trên `ContractCreationPort`/`RoomOccupancyPort` fake. Hoặc M02/M04 bàn giao thật trước rồi M03 nối. **Điều kiện ghép:** E2E-03/04, hai Admin approve hai đơn cùng Room, giá Room thay đổi trước/sau duyệt, đơn đã WITHDRAWN/REJECTED, đơn quá ngày.

### WF-02 — Cho thuê trực tiếp (UC17 + UC22 + UC15)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO** | **M04** | Điều phối tạo Contract trực tiếp, transaction và kiểm thử. |
| **Provider** | M01 | User đại diện tồn tại, đã có CCCD, role hợp lệ. |
| **Provider** | M02 | Room AVAILABLE, giá niêm yết hiện tại, reserve. |
| **Provider** | M03 | `rejectOtherPending(roomId, null)` sau khi cho thuê thành công. |

**Bước runtime:** Admin chọn người đại diện đã có tài khoản + Room trống + thời gian hợp lệ (hôm nay..+10, end ngày 10) → khóa/recheck Room, lấy giá niêm yết hiện hành > 0 vào `agreed_monthly_rent` → tạo Contract không Application (`application_id=NULL`) và giữ Room → từ chối PENDING cạnh tranh → commit; gửi thông báo sau commit. Nếu phải rollback, Room không được RENTED giả. Unique filtered index ngăn Contract SCHEDULED/ACTIVE thứ hai cùng Room; M04 trả lỗi phòng bận khi đúng index này từ chối.

**Code trước–sau:** M04 có thể hoàn thiện validate, form, Contract creation với Room/Application mock; M02 và M03 độc lập xây commands rồi ghép. **Không** để M04 tự update `rental_applications` hoặc `rooms` qua Repo. **Kiểm thử:** E2E-05/09, sai CCCD, phòng bận, hai lệnh tạo gần đồng thời, Contract giá không bị đổi, đơn cạnh tranh REJECTED.

### WF-03 — Kích hoạt, hết hạn tự nhiên và kỳ cuối (UC20 + UC21 + UC22 + UC24–26)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO chuyển trạng thái Contract** | **M04** | `SCHEDULED→ACTIVE`, `ACTIVE→ENDED`, Room giải phóng đúng thời điểm. |
| **WO kỳ thu** | **M05** | Bảo đảm nghĩa vụ REGULAR của kỳ vừa kết thúc không bị bỏ sót/trùng, kể cả Contract đã ENDED. |
| **Provider** | M02 | Room giữ RENTED khi ACTIVE, về AVAILABLE khi kết thúc tự nhiên. |

**Không được hiểu nhầm:** Kết thúc tự nhiên đúng ngày 10 **không** bị tính như Tenant tự trả sớm. Hợp đồng ENDED không phải căn cứ bỏ kỳ tiền thuê cuối. Scheduler có thể chạy lại/đảo thứ tự: M04 kết thúc lúc 00:00, M05 tạo kỳ sau đó, hoặc M05 xử lý trước M04. Hai luồng phải nhìn ra cùng nghĩa vụ kỳ `[10 trước,10 hiện tại)`. Không gia hạn hoặc tự đổi plannedEndDate.

**Code trước–sau:** M04 viết scheduler trên Clock giả; M05 viết duty selection trên ContractBillingSnapshot giả; M02 viết release. P2 dùng dữ liệu thật và kiểm thử hai thứ tự Scheduler, restart server, chạy nhiều lần. **Kiểm thử:** E2E-06/07/08/13/23/24.

### WF-04 — Đến ngày 10, Admin nhập điện nước, Invoice và sandbox (UC24–29, UC33)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO, MO Invoice** | **M05** | Tạo cycle/item, form tiền điện nước, tính tiền, phát hành, sandbox, lịch sử PAID. |
| **Provider** | M04 | ContractBillingSnapshot, cả khoản đã sử dụng của Contract vừa ENDED. |
| **Provider** | M02 | RoomSummary phục vụ UI biểu mẫu. |
| **Provider** | M01 | Quyền Admin/Tenant và điểm tích hợp hard gate. |
| **Consumer mềm** | M06 | Notification sau InvoiceIssued/Paid. |

**Bước runtime:**

1. Vào ngày 10, M05 tạo/kiểm tra duy nhất `BillingCycle(collectionDate)` và một `BillingCycleItem` cho mỗi Contract thuộc kỳ, bảo toàn dữ liệu lần nhập cũ.
2. Admin đăng nhập khi cycle PENDING_INPUT bị chặn khỏi giao diện nghiệp vụ thông thường, vào form nhập **đủ tiền điện và nước cho mọi nghĩa vụ**. Không dùng công tơ/rate tự tính; `null ≠ 0`.
3. M05 tính rent: kỳ đầu dưới 14 ngày `agreedRent / 30 × days`, HALF_UP khoản cuối; kỳ đầu >=14/kỳ thông thường đủ tháng; tổng rent+điện+nước.
4. Tổng >0: tạo duy nhất Invoice REGULAR, `UNPAID`, `dueAt=issuedAt+48h`; tổng 0: item NO_CHARGE, không Invoice 0 đồng. Chỉ khi mọi item đã INVOICED/NO_CHARGE mới COMPLETED cycle.
5. Commit Invoice → M06 xử lý thông báo. Tenant chính chủ xem Invoice, nếu `UNPAID` và `now<dueAt` thì M05 tạo yêu cầu sandbox; xác minh server-side callback và ghi PAID đúng một lần; thất bại còn hợp lệ có thể thử lại.

**Code trước–sau:** TV5 **không chờ** Contract thật mới thử sandbox; dùng Invoice fixture. M05 và M04 thỏa thuận Snapshot ngày 1; M05 có thể viết công thức với mock. M01 tích hợp guard khi M05 trả policy trạng thái; M06 gắn Notification sau cùng. **Kiểm thử:** chạy scheduler nhiều lần, nhập thiếu 1 Room, zero, sai quyền, callback lặp, callback muộn, ngày 10 biên, 48h, Invoice không chỉnh tiền sau phát hành.

### WF-05 — Thu tiền ngoài hệ thống, quá hạn, công nợ (UC30/31/32 + UC27/33)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO thao tác Admin/Scheduler** | **M06** | Giao diện thu tiền/công nợ, cron overdue, kiểm tra quyền giao diện, test workflow. |
| **MO Invoice** | **M05** | Đọc Invoice, kiểm tra lại state, thực hiện `UNPAID/OVERDUE→PAID` hoặc `UNPAID→OVERDUE` nguyên tử. |

**Bước runtime:** Admin chọn Invoice chưa trả → chọn CASH/BANK_TRANSFER sau khi đã nhận đủ, hoặc đối chiếu VNPAY đã xác minh (cũng lưu BANK_TRANSFER) → M06 gửi lệnh cho M05 → M05 kiểm tra lại Invoice tồn tại, trạng thái, số tiền đã nhận đủ; ghi paidAt, đúng method, idempotent. Scheduler M06 yêu cầu M05 đổi UNPAID thực sự quá dueAt thành OVERDUE. Người thuê không tự online trả Invoice OVERDUE; Admin vẫn tra được nợ của Contract ENDED.

**Code trước–sau:** M06 làm form, filter, job với mock `InvoiceQuery/Command`, M05 làm command và gateway verification độc lập. **Kiểm thử:** Admin và gateway đến gần đồng thời; PAID giữ nguyên, callback trễ không biến phương thức thành CASH; OVERDUE không bị coi là miễn nợ.

### WF-06 — Kết thúc ACTIVE sớm, Admin đóng phòng RENTED (UC34 + UC35 + UC08 + UC22)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO chính đã chốt** | **M04** | Một `TerminationWorkflow` cho UC34 và nhánh RENTED→INACTIVE của UC08; kiểm tra reason/Contract/date, giữ transaction và kết quả end-to-end. |
| **Người nhận yêu cầu đóng phòng** | M02 | UI UC08 chuyển yêu cầu `RENTED→INACTIVE` sang WO M04 với reason ADMIN_STOP_SERVICE. |
| **MO tính quyết toán** | M06 | Tính rent policy theo reason/ngày, điện nước đã chốt. |
| **MO Invoice** | M05 | `issueFinalInvoice` hoặc `NO_CHARGE`, chống trùng với REGULAR. |
| **MO Room** | M02 | release sang AVAILABLE (Tenant tự rời) hoặc INACTIVE (Admin stop). |

**Bước runtime với Contract ACTIVE và kết thúc trong kỳ:**

1. M04 kiểm tra ACTIVE, `effectiveEndDate=today` UTC+7; xác định reason `TENANT_EARLY_EXIT` hoặc `ADMIN_STOP_SERVICE`, giá Contract và khoảng đang dùng.
2. M06 tính rent: Tenant tự kết thúc **nguyên kỳ đang sử dụng**, còn Admin stop **monthly/30 × ngày thực tế** (không tính ngày trả), cộng điện/nước nhập theo thực tế. Không lấy giá niêm yết Room.
3. M05 kiểm tra trùng kỳ, phát hành Invoice EARLY_SETTLEMENT nếu total>0, hoặc ghi NO_CHARGE nếu =0; trả kết quả phân biệt rõ hai nhánh. Không sửa Invoice đã phát hành.
4. M04 lưu dấu quyết toán trên Contract, chuyển ENDED; M02 giải phóng sang AVAILABLE/INACTIVE đúng reason; commit đồng bộ. **Không cần Invoice đã PAID** mới kết thúc.
5. Gửi Notification sau commit; công nợ UNPAID/OVERDUE vẫn tra cứu được.

**Hai ngoại lệ quan trọng:**

- Nếu **SCHEDULED** bị Admin hủy trước khi bắt đầu: UC19 → CANCELLED, không phải UC34, không phát hành tiền thuê kỳ cuối; M02 release/INACTIVE đúng lựa chọn Admin.
- Nếu Tenant tự kết thúc đúng **ngày 10 sau kỳ đã sử dụng**, chưa dùng ngày nào của kỳ mới: M04 end + M02 release nhưng **không** gọi `issueFinalInvoice`/không ghi dấu settlement sớm; M05 xử lý REGULAR kỳ vừa kết thúc, kể cả khi Admin chốt điện nước/phát hành sau khi Contract ENDED. Không tạo tiền kỳ mới 0 ngày/Invoice thứ hai.

**Code trước–sau:**

- **Song song:** M04 dựng `TerminationWorkflow` với fake SettlementResult, FinalInvoiceResult, RoomReleaseResult; M06 viết calculator thuần và unit test; M05 viết issueFinalInvoice từ fixture; M02 viết release/close command. Ghép P2 trong một DB transaction.
- **Tuần tự:** M06 calculator + M05 issue + M02 release sẵn trước → M04 điều phối sau. Cách này ít mock hơn nhưng có thể khiến M04 chờ lâu.
- **Mốc nghiệm thu bắt buộc:** E2E-07/21/22/23/24. Lỗi phát hành kỳ cuối không được để Contract ENDED nhưng thiếu dấu quyết toán, hoặc Room INACTIVE khi Contract còn ACTIVE.

### WF-07 — Issue và Notification (UC36–40)

| Vai trò | Người | Nhiệm vụ |
|---|---|---|
| **WO Issue** | **M01** | M04 Contract ACTIVE check → tạo Issue; Admin cập nhật; Tenant xem lịch sử. |
| **WO Notification** | **M06** | UC39 soạn gửi, UC40 xem đọc, event Issue/Invoice/Application. |

**Trình tự:** UC36 chỉ chấp nhận người đại diện có Contract ACTIVE đúng phòng; Issue cũ tồn tại và Admin vẫn xử lý sau khi Contract ENDED. Sau khi Issue cập nhật thành công, M01 phát event → M06 lưu Notification từng người nhận, khóa `eventKey + recipient`, trạng thái UNREAD/READ. Notification là phụ thuộc mềm; lỗi không rollback Issue/Invoice/Contract.

---

## 9. Dependency Registry — ai cần tìm ai, phụ thuộc nào làm trước/được mock

**Quy ước:** `A ← B` nghĩa là A cần năng lực do B sở hữu. Đây **không** phải mệnh lệnh B phải code xong trước A.

| ID | Bên cần ← bên cung cấp | UC/WF | Cấp độ | Giao tiếp tối thiểu phải thống nhất | Hướng song song | Khi nào bắt buộc dùng thật? |
|---|---|---|---|---|---|---|
| D01 | M03 ← M01 | UC10–14 | Nền tảng | CurrentUserId, Role | Mock principal | Khi test quyền gửi/duyệt đơn. |
| D02 | M03 ← M02 | UC10/14 | Đọc + cứng lúc duyệt | Room status, lock/availability, Price | RoomSummary mock | Trước online rental end-to-end. |
| D03 | M03 ← M04 | UC14/16 | **CỨNG** | ContractCreationRequest/Result, errors | M03 mock create, M04 code create | Trước APPROVE thật. |
| D04 | M04 ← M02 | UC16/17/19/21/34 | **CỨNG** | reserve/release/close, khóa và kiểm tra lại Room; không thêm cột version cho foundation | Mock RoomOccupancy | Trước Contract lifecycle end-to-end. |
| D05 | M04 ← M03 | UC17/15 | **CỨNG** | rejectOtherPending, ApplicationQuery | Mock ApplicationCommand | Trước Direct Rental thật. |
| D06 | M01 ← M04 | UC36 | Đọc | hasActiveContract(userId,roomId) | Mock true/false | Trước tạo Issue thật. |
| D07 | M01 ← M02 | UC36/37 | Đọc | RoomSummary | Room fixture | Trước trang Issue đầy đủ. |
| D08 | M02 ← M04 | UC07/08 | Đọc + cứng | active/scheduled Contract check, termination command | Mock ContractStatus/Result | Trước sửa giá/đóng RENTED thật. |
| D09 | M04 ← M06 | UC34/35 | **CỨNG** | SettlementRequest/Result, reason | Calculator mock | Trước kết thúc ACTIVE thật. |
| D10 | M04 ← M05 | UC21/34 | **CỨNG** | final invoice/no-charge, period query | Fake InvoiceCommand | Trước kỳ cuối/termination thật. |
| D11 | M05 ← M04 | UC24–26 | Đọc | ContractBillingSnapshot gồm cả kỳ của ENDED | Snapshot fixture | Trước phát hành kỳ thật. |
| D12 | M05 ← M02 | UC24/25 | Đọc | RoomSummary | Room fixture | Trước biểu mẫu tất cả phòng thật. |
| D13 | M05 ← M01 | UC25/27/28 | Nền tảng | Actor Admin/Tenant, Invoice owner policy, hard gate | SecurityContext/mock guard | Trước test bảo mật và Admin login thật. |
| D14 | M06 ← M05 | UC30 | **CỨNG** | confirmManualPayment + trạng thái kết quả | Fake InvoiceCommand | Trước thu trực tiếp thật. |
| D15 | M06 ← M05 | UC29/30 | **CỨNG** | confirmVerifiedSandboxPayment + bằng chứng gateway | Fake verified Result | Trước đối chiếu callback thật. |
| D16 | M06 ← M05 | UC31 | **CỨNG** | markOverdueInvoices(now) | Fake overdue command | Trước job overdue thật. |
| D17 | M06 ← M05 | UC32 | Đọc | findOutstandingInvoices(filter) | InvoiceSummary mock | Trước công nợ thật. |
| D18 | M06/M04 ← M05 | UC35 | **CỨNG** | issueFinalInvoice/NO_CHARGE | FinalInvoiceResult mock | Trước termination thật. |
| D19 | M06 ← M04 | UC35 | Đọc | ContractSettlementSnapshot | Contract fixture | Trước tính/chốt kỳ cuối thật. |
| D20 | M01–M05 → M06 | Event thông báo | **MỀM** | eventKey, recipient, sourceType, entity ref | Fake event handler | Sau khi luồng chính hoạt động, trước demo Notification. |
| D21 | M06 ← M01/M02/M05 | Dashboard | Đọc | unresolvedIssueCount, roomCountByStatus, Invoice totals | Aggregate DTO mock | Trước demo Dashboard. |

### 9.1. Các bên phải thống nhất tối thiểu những gì trước khi code mock?

Mỗi dependency cứng/đọc phải có một **Dependency Contract** ngắn với:

1. **Owner cung cấp** và người tiêu thụ; tên Port/DTO và phạm vi package được phép sử dụng.
2. **Input/output rõ kiểu**, ID, `BigDecimal`, `LocalDate`, `Instant`; enum trạng thái và ý nghĩa từng `null`.
3. **Precondition/postcondition:** role, status trước và status sau, những object nào được thay đổi.
4. **Failure contract:** not found, forbidden, invalid state, conflict/room busy, duplicate/idempotent, DB/gateway failure; không trả `null` mơ hồ.
5. **Transaction boundary:** coordinator nào mở transaction, command của MO có tham gia cùng transaction không; email/event chỉ chạy sau commit.
6. **Idempotency/concurrency:** Scheduler/callback gọi lặp, khóa unique, không ghi đè Invoice PAID.
7. **Sample fixtures và contract tests:** ít nhất success, invalid state và cạnh tranh/duplicate nếu có.

**Phụ thuộc hai chiều không có nghĩa Service phụ thuộc vòng tròn:** M03 `ApprovalWorkflow` có thể dùng M04 `ContractCreator`, còn M04 `DirectRentalWorkflow` dùng M03 `ApplicationRejector`. Hai coordinator khác nhau, hai port con độc lập, không inject coordinator vào nhau. Tương tự M05 BillingService dùng M04 ContractQuery nhưng M04 TerminationWorkflow dùng M05 InvoiceCommand — tách Query và Command service để không tạo vòng constructor.

### 9.2. Ai là người viết mock? Có cần chờ DTO/Port hoàn thiện toàn bộ không?

- **Mặc định:** consumer viết mock/stub trong **test của chính mình**, theo DTO/Port đã thỏa thuận với provider. Provider gửi mẫu input/output và mã lỗi. Không commit mock thay thế Bean production.
- **Provider-first:** nếu provider hoàn thiện command sớm, consumer ghép luôn, bỏ bước mock. Đây vẫn là lựa chọn hợp lệ.
- **Consumer-first:** nếu provider chưa sẵn, consumer có thể hoàn thành UI/orchestrator/test với mock, đánh dấu rõ **chưa nghiệm thu P2**.
- **Song song hợp đồng trước:** hai bên chốt tối thiểu dữ liệu + hành vi success/failure + transaction, rồi code riêng; không cần họp chốt toàn bộ class/method của ứng dụng.
- **Nếu interface phải đổi:** provider thông báo tất cả consumer, ghi thay đổi vào sổ phụ thuộc, lập PR riêng hoặc cùng PR cập nhật cả usage; không tự sửa bên kia rồi merge âm thầm.

---

## 10. Kế hoạch 10 ngày: bắt đầu song song, ghép theo workflow

**Điều kiện quan trọng:** lịch này dành cho **thứ tự tích hợp và các sản phẩm kiểm chứng**. Nếu foundation/Entity chưa hoàn thành, trưởng nhóm phải dành thời gian riêng để hoàn thành trước khi coi các giai đoạn dùng DB thật đã bắt đầu. Không yêu cầu sáu người code một hàng dọc.

| Ngày | Mục tiêu chính | Ai có thể làm độc lập | Đầu ra cần nghiệm thu |
|---|---|---|---|
| **D1** | Foundation chạy; chốt tên DTO/Port, owner các file chung và WO; tạo nhánh/ticket. | M01 Auth skeleton, M02 Room UI, M03 App state, M04 validation, M05 sandbox fixture, M06 Notification. | `schema.sql` và `seed_admin.sql` đã chạy một lần trên `RentalApartmentDev` cục bộ, 10 Entity/Enum map đúng và JPA `validate` đạt; kiểm tra thêm constraint bằng dữ liệu sai; `CurrentUser`, Room/Contract/Invoice ports tối thiểu và người phụ trách được công bố. Nếu foundation chưa xong, D1 chỉ là mục tiêu chứ chưa phải kết quả. |
| **D2** | Các phần **P0** và mock giao tiếp; cung cấp Query sớm. | M01 UC01–03; M02 UC04–07/09; M03 UC11–13; M04 UC18/20; M05 Invoice DTO, sandbox; M06 UC39/40, calculator. | Mỗi người có route/service/test tự chạy được, không sửa Repo người khác. |
| **D3** | Hoàn thành phần độc lập và viết workflow coordinator theo mock. | M03 UC14 mock; M04 UC16/17 mock; M05 UC24–26 snapshot mock; M06 UC30/31/32 mock. | Unit tests success/failure và sổ dependency cập nhật `MOCK_READY`. |
| **D4** | Đóng băng giao tiếp P1 để ghép; Auth/Room/Contract queries thật. | M01/M02 hoàn thiện provider; M04 ContractBillingQuery; M05/06 tiếp tục sandbox, calculator. | Build chung xanh, `CurrentUser`, RoomQuery, ContractQuery dùng được; không tồn tại vòng constructor. |
| **D5** | **Tích hợp WF-01 và WF-02** (M03/M04/M02). | M05 tiếp tục test sandbox & billing; M06 Notification + outstanding UI. | Approve online, Direct Rental, reject pending, Room RENTED, transaction/competing requests chạy thật. |
| **D6** | **Tích hợp WF-03/04** (Contract kỳ cuối và Billing). | M01 ghép Issue; M06 ghép InvoiceQuery/Command. | Ngày 10 → form Admin → Invoice/NO_CHARGE; sandbox với Invoice thật, lịch sử PAID. |
| **D7** | **Tích hợp WF-05/06** (manual/overdue/termination/inactive). | M01/M06 ghép Notification; M02 Dashboard count. | UC30–32 và UC34/35/08 đều chạy với DB thật, không mock ở nhánh end-to-end. |
| **D8** | Test cạnh tranh, Scheduler, biên ngày 10/48h, retry. | Các owner fix phần mình, WO chạy test workflow; leader review schema/PR. | Không lặp Invoice/Contract, không PAID hai lần, không mất kỳ cuối. |
| **D9** | Kiểm thử E2E toàn vai trò; chỉnh UI; dữ liệu demo. | Song song các module sửa bug của mình; WO đảm nhận luồng tích hợp. | Guest/Tenant/Admin/Scheduler/Gateway demo được, không dữ liệu mock/placeholder. |
| **D10** | Regression, khóa thay đổi nguy hiểm, chuẩn bị thuyết trình. | Mỗi người xác nhận Done và trình bày phần mình. | Nhánh main build ổn, script seed/demo, checklist test và bảng known limits. |

### 10.1. Thứ tự **tích hợp** không phải thứ tự **code**

1. M01 Auth + M02 RoomQuery/Occupancy + M04 Contract Query (mở khóa phần lớn consumer).
2. WF-01/WF-02: M03+M04+M02 (hai cách thuê).
3. WF-03/WF-04: M04+M05+M02, rồi sandbox (hợp đồng thật → kỳ → Invoice → PAID).
4. WF-05: M06+M05 (thu ngoài đời, quá hạn, công nợ).
5. WF-06: M04+M06+M05+M02 (kết thúc sớm và Admin INACTIVE).
6. Issue/Notification/Dashboard ghép dần, không chặn các luồng cứng nếu chưa có event handler.

**Không đợi cuối kỳ mới merge:** mọi thay đổi đi qua PR nhỏ theo ticket. WO phụ trách nhánh integration workflow có thể review các PR liên quan, nhưng không tự sửa trực tiếp code owner module khác trong PR của mình nếu chưa thống nhất.

### 10.2. Ranh giới file/package giảm conflict

| Khu vực code | Người chịu trách nhiệm chính | Ai được đề nghị sửa? |
|---|---|---|
| `schema.sql`, `seed_admin.sql`, các Entity/Enum ban đầu và common config | Trưởng nhóm/foundation trước handoff | MO gửi PR/đề xuất khi phát sinh thay đổi; leader duyệt. Script schema chạy thủ công một lần, không có Flyway. |
| `account/**`, `issue/**`, Auth `SecurityConfig` | M01 | Module khác gửi yêu cầu role/guard; không tự viết security riêng. |
| `room/**`, RoomRepository/RoomCommand | M02 | M03/M04/M05/M06 qua Room Port. |
| `application/**`, ApplicationRepository/Command | M03 | M04 gọi Application Port. |
| `contract/**`, ContractRepository/Command | M04 | M01/M03/M05/M06 qua Contract Port. |
| `billing/**`, `invoice/**`, Sandbox adapter | M05 | M06 chỉ qua Invoice Port, không tự `save()` Invoice. |
| `collection/**`, `settlement/**`, `notification/**`, Admin dashboard composition | M06 | Các owner cung cấp read DTO/events, không sửa NotificationRepository. |
| `workflow/approve/**` | WO M03 | M02/M04 chỉ cung cấp Port/service và review. |
| `workflow/directrental/**`, `workflow/termination/**` | WO M04 | M02/M03/M05/M06 cung cấp Port/service và review. |
| `workflow/billing/**` | WO M05 | M01/M02/M04/M06 cung cấp query, gate integration, events. |

Entity/Enum của foundation đặt trong package `entity` theo feature dưới `com.apartment.rental`: `account.entity`, `room.entity`, `application.entity`, `contract.entity`, `billing.entity`, `invoice.entity`, `issue.entity`, `notification.entity`; chỉ phần timestamp chung dùng `common.persistence`, không tạo bảng audit. Tên các package service/workflow trong bảng là ví dụ để nhóm chốt, không bắt buộc tạo đúng cấu trúc đó. Trong Spring Boot MVC một ứng dụng, không cần microservice, sáu cơ sở dữ liệu hoặc HTTP nội bộ giữa các module.

### 10.3. Trạng thái ticket và cách báo tiến độ

| Trạng thái | Ý nghĩa | Bằng chứng tối thiểu |
|---|---|---|
| `READY` | Đã có mô tả UC + BR + Port/mock cần thiết. | Ticket có actor, rules, input/output, dependency ID. |
| `LOCAL_DONE` | Code/service/UI hoạt động độc lập. | Unit tests, screenshot/route hoặc dữ liệu test. |
| `MOCK_READY` | Consumer hoàn thiện workflow với mock, chờ provider thật. | Test fake success/failure; link dependency. |
| `PROVIDER_READY` | Provider thật đã bàn giao query/command và tests. | Build xanh và code interface/implementation merge. |
| `INTEGRATING` | WO ghép các provider thật. | PR tích hợp, log issue, hướng xử lý. |
| `E2E_DONE` | Luồng nghiệp vụ thật chạy từ đầu đến cuối. | Integration tests/QA bằng SQL Server; **không mock** các bước cứng. |

Mỗi buổi họp ngắn hỏi đúng ba câu: (1) UC/ticket nào đã Local Done? (2) Dependency ID nào đang chờ người khác? (3) Có mock nào vẫn nằm trên đường chạy demo không?

---

## 11. Checklist nghiệm thu ưu tiên theo Workflow Owner

| ID | WO / Module | Kịch bản | Kết quả cần thấy |
|---|---|---|---|
| E2E-01 | M01 | Guest đăng ký thiếu CCCD/điện thoại, dùng hai email chỉ khác hoa/thường hoặc truy cập trang Admin | Thiếu dữ liệu/sai quyền bị từ chối đúng backend; hai email khác hoa/thường được phân biệt nhất quán khi đăng ký/đăng nhập; User không thể tự cấp role Admin. |
| E2E-02 | M02 | Public xem danh sách khi Room RENTED/INACTIVE; lọc theo diện tích/tầng | Chỉ AVAILABLE xuất hiện; tầng bắt buộc > 0, diện tích NULL không bị hiển thị thành 0 hoặc giả dữ liệu. |
| E2E-03 | M03 + M04 + M02 | APPROVE một trong hai đơn cùng phòng | Một Contract, Room RENTED, một APPROVED, các PENDING khác REJECTED. |
| E2E-04 | M03 + M04 + M02 | Lỗi tạo Contract/giữ Room giữa lúc APPROVE | Không còn APP APPROVED giả; transaction rollback nhất quán. |
| E2E-05 | M04 + M02 + M03 | Admin cho thuê trực tiếp | Contract `application_id=NULL`, giá snapshot Room hiện tại > 0, PENDING cạnh tranh REJECTED. |
| E2E-06 | M04 + M02 | SCHEDULED đến ngày thuê nhưng khách không đến | Contract ACTIVE, Room RENTED; không tự hủy no-show. |
| E2E-07 | M04 + M02 | Hủy SCHEDULED trước bắt đầu | CANCELLED, giải phóng/đóng phòng theo yêu cầu, không Invoice tiền kỳ cuối. |
| E2E-08 | M04 + M02 + M05 | ACTIVE hết hạn tự nhiên ngày 10, scheduler chạy trước/sau nhau | ENDED, Room AVAILABLE, kỳ REGULAR cuối không mất/trùng. |
| E2E-09 | M04 + M02 | Hai yêu cầu thuê đồng thời hoặc cố đổi giá khi còn Contract | Unique filtered index chặn hai Contract SCHEDULED/ACTIVE cùng Room; Contract ENDED lịch sử vẫn được giữ; giao dịch bị từ chối rollback, Room không RENTED giả; giá Contract cũ bất biến và đổi giá bị chặn. |
| E2E-10 | M04 + M02 | Admin tăng/giảm co-occupants | Không trùng CCCD trong Contract, không vượt capacity, không sửa sau ENDED. |
| E2E-11 | M05 | Kỳ đầu 7 ngày, giá 2.000.000 VND | Tiền thuê `2,000,000*7/30` HALF_UP = **466.667 VND** rồi cộng điện nước. |
| E2E-12 | M05 | Kỳ đầu >=14 ngày/kỳ sau | Tiền thuê bằng giá tháng Contract; không lấy giá Room đã đổi. |
| E2E-13 | M05 | Billing cycle tạo lại do restart/login | Một cycle/item/nghĩa vụ, không reset tiền điện đã nhập hoặc nhân đôi Invoice. |
| E2E-14 | M05/M01 | Admin còn điện nước chưa nhập đủ, một trường NULL và một trường 0 | Hard gate đúng; chỉ NULL tính là thiếu, 0 là dữ liệu đã nhập. |
| E2E-15 | M05 | Tổng 0 và phát hành lỗi giữa chừng | NO_CHARGE không có Invoice 0; dữ liệu nhập không mất; retry an toàn. |
| E2E-16 | M05 + Gateway | Tenant Invoice của mình UNPAID trước dueAt thanh toán thành công | PAID một lần; paymentMethod BANK_TRANSFER. |
| E2E-17 | M05 | Tenant khác/Invoice OVERDUE/now=dueAt bắt đầu sandbox | Server chặn; callback lặp không ghi PAID lần hai. |
| E2E-18 | M06 + M05 | Admin xác nhận CASH/BANK_TRANSFER | M05 ghi PAID đúng method, không ghi lại khi đã PAID. |
| E2E-19 | M06 + M05 | Quá dueAt 48h, Contract ENDED nhưng còn Invoice | OVERDUE; Admin vẫn tìm ra khoản nợ, không tự xóa nợ. |
| E2E-20 | M05 + M06 | Gateway thành công muộn / callback mất / PAID đã có | Admin đối chiếu giao dịch xác minh; chưa PAID thì M05 ghi BANK_TRANSFER, đã PAID không ghi đè. |
| E2E-21 | M04 + M06 + M05 + M02 | Tenant rời sớm trong kỳ | M06 tính nguyên kỳ + điện nước, M05 phát hành Invoice/NO_CHARGE, Contract ENDED, Room AVAILABLE. |
| E2E-22 | M02 + M04 + M06 + M05 | Admin ngừng cung cấp phòng ACTIVE | M06 tính rent theo ngày + điện nước, Contract ENDED, Room INACTIVE. |
| E2E-23 | M04 + M05 + M06 | Tenant rời đúng ngày 10 sau kỳ đã ở | REGULAR kỳ cũ duy nhất; không EARLY_SETTLEMENT/kỳ mới 0 ngày. |
| E2E-24 | M04 + M05 + M02 | Scheduler sau khi quyết toán sớm | Không tạo trùng kỳ cuối, Room không bị giữ lại sai trạng thái. |
| E2E-25 | M01 + M04 | User không có Contract ACTIVE cố tạo Issue; Issue cũ khi Contract ENDED | Tạo mới bị chặn; Issue cũ vẫn được Admin xử lý/Tenant xem lịch sử. |
| E2E-26 | M06 + M01/M03/M05 | Notification gửi trùng event hoặc handler lỗi | Không trùng recipient-event; lỗi Notification không rollback Contract/Invoice. |
| E2E-27 | M06 + M01/M02/M05 | Dashboard | Count Room theo status; SUM Invoice PAID; count UNPAID/OVERDUE; unresolved Issue từ Query owner. |

**Tiêu chuẩn nghiệm thu một workflow cứng:** tests đạt cả **success + failure/rollback + sai quyền + chạy lại/idempotency hoặc concurrency** nếu có; dùng Port thật, SQL Server thật và các Entity/schema chuẩn. Unit test với mock không đủ để đánh dấu E2E_DONE.

**Kiểm tra foundation trước E2E:** chạy `schema.sql` và `seed_admin.sql` một lần trên `RentalApartmentDev` của môi trường cần nghiệm thu; kiểm tra dữ liệu hợp lệ/sai cho PK, FK ghép, UNIQUE/filtered index, CHECK (đặc biệt `floor_number > 0`, giá tháng > 0, `area_m2` NULL hoặc > 0), email phân biệt hoa/thường, timestamp UTC và JPA `validate`. Không cần Docker, Testcontainers hoặc hạ tầng CI database riêng cho bước này. Tổng tiền kỳ bằng 0 vẫn phải theo NO_CHARGE dù giá tháng của Room/Contract > 0.

---

## 12. Checklist sử dụng khi bàn giao cho từng thành viên

Mỗi người phải xác nhận:

- [ ] Đọc mục module của mình; biết **chính xác UC, BR và trạng thái** cần bảo vệ.
- [ ] Biết mình là Module Owner của bảng/Repository/Service nào và Workflow Owner của workflow nào.
- [ ] Đã nhận `schema.sql`, `seed_admin.sql`, các Entity/Enum và biết schema đã được chạy trên `RentalApartmentDev` cục bộ với JPA `validate`; không tạo lại hoặc đổi schema âm thầm.
- [ ] Có danh sách P0 làm ngay và P1 cần Port/mock, không ngồi chờ cả module người khác xong.
- [ ] Biết mã Dependency Dxx của mình và người cần trao đổi ở đầu bên kia.
- [ ] Hai bên đã chốt DTO/Port: input/output, status, exception, transaction và idempotency.
- [ ] Nếu làm trước, mock nằm trong test hoặc stub được đánh dấu rõ, không phải production behavior.
- [ ] Đã bàn giao năng lực nhỏ sớm cho module đang phụ thuộc mình.
- [ ] Có Unit Test cho BR của module và Integration Test do WO chịu trách nhiệm.
- [ ] Không ghi Repository của module khác; không có Service injection vòng; không có workflow coordinator trùng logic.
- [ ] Đã thay mock bằng service thật và kiểm thử cùng SQL Server trước khi báo hoàn thành.
- [ ] PR nhỏ, mô tả ảnh hưởng tới Module Owner/Workflow Owner liên quan và người review.

### 12.1. Mẫu ticket ngắn để copy sang GitHub Issues

```md
### [Mxx-Tyy] Tên chức năng / UC

- Module Owner: Mxx / Thành viên ...
- Workflow Owner (nếu khác): Myy / Thành viên ...
- UC: UC..
- Business Rules: BR-...
- Scope: ...
- Input/Output: ...
- Entity/Service được quyền sửa: ...
- Dependency IDs: D..
- Provider cần liên hệ: M..
- Phương án chọn: Provider-first / Consumer-first+mock / Song song
- P0/P1: cách làm độc lập: ...
- P2: điểm ghép service thật: ...
- Test cases: success, failure, quyền, retry/concurrency (nếu áp dụng)
- Definition of Done: ...
```

## 13. Quyết định đã chốt và các điểm trưởng nhóm còn cần xác nhận trước khi đóng dấu V2

Các điểm sau là **quy ước tổ chức phát triển/kỹ thuật**, **không phải BR mới**:

1. **Hai lớp owner:** giữ Module Owner theo 10 bảng; nhóm xác nhận Workflow Owner của những luồng khác theo Bảng WF-01..WF-07 hoặc chỉ rõ điều chỉnh.
2. **Đã chốt ownership WO termination:** M04 điều phối **một** TerminationWorkflow để cả UC34 và nhánh RENTED→INACTIVE của UC08 dùng chung. M06 chỉ sở hữu máy tính settlement, M05 chỉ issue Invoice và M02 chỉ update Room.
3. **Xác nhận foundation thực sự đã có:** một `schema.sql` và một `seed_admin.sql` chạy thủ công một lần trên `RentalApartmentDev` cục bộ, toàn bộ 10 Entity/Enum/JPA relationship, kiểm tra constraint/index bằng dữ liệu mẫu hợp lệ/sai và JPA `validate` trước khi giao nhánh. Nếu chưa, dùng P0 thiết kế trước nhưng không tự đánh dấu hoàn tất DB. Không dùng Flyway.
4. **Chốt giao tiếp tối thiểu:** CurrentUser/RoomQuery/RoomOccupancy, ApplicationQuery/Command, ContractCreate/ContractBilling/Activity, InvoiceQuery/Command, SettlementCalculator, NotificationEvent. Chốt error/transaction/idempotency, không cần chốt mọi chi tiết class ngay từ đầu.
5. **Chốt một owner config chung:** M01 SecurityConfig và chỗ M05 yêu cầu Admin nhập điện nước; chủ dự án/một owner `schema.sql`, `seed_admin.sql`, `pom.xml`, layouts và CI. Không hai người tự sửa code chung độc lập.
6. **Chốt dữ liệu/thiết bị demo còn mở:** cách lưu/upload ảnh phòng, cổng sandbox cụ thể, cách tạo Invoice/demo kỳ ngày 10. Giá thuê > 0, diện tích nullable > 0 khi có, tầng bắt buộc > 0 và không thêm thời hạn thuê tối thiểu đã được chốt; không đưa chúng trở lại danh sách chờ. Mọi quyết định nghiệp vụ mới phải sửa `BUSINESS_SPECIFICATION.md` trước; V2 chỉ cập nhật cách làm.

---

## Kết luận giao việc

**Không thay sáu module hoặc 40 UC.** V2 thêm một lớp trách nhiệm **Workflow Owner** để người phụ trách có toàn quyền **điều phối và chịu trách nhiệm hoàn thành luồng**, chứ không có quyền tạo những phiên bản ghi dữ liệu/Business Rule trùng với module khác. Module Owner vẫn là nguồn duy nhất cho trạng thái/Entity/Repository của mình; WO ghép Port, transaction và integration tests. Song song/tuần tự là **hai phương án ngang hàng** ở mỗi dependency, được chọn theo tiến độ thực tế của hai thành viên.

Tài liệu V2 là bản phục vụ nhóm **đánh giá trước khi phát hành**. Khi đã xác nhận các điểm còn mở ở mục 13 và foundation chạy thật, có thể dùng trực tiếp các bảng Ticket/Dependency/E2E làm GitHub Epic và Issues để giao cho sáu người.
