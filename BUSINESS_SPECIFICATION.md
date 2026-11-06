# ĐẶC TẢ NGHIỆP VỤ TỔNG THỂ — HỆ THỐNG QUẢN LÝ PHÒNG TRỌ

> **Tên tài liệu:** Business Specification / Business Context & Rules  
> **Phiên bản:** 1.0 — bản tổng hợp để bàn giao nhóm  
> **Ngày cập nhật:** 29/10/2026  
> **Trạng thái:** Bản 1.0 chưa phát hành, đã hợp nhất các quyết định nghiệp vụ và Database đến ngày 29/10/2026. Các mục còn phải đặc tả triển khai không được tự hiểu là thay đổi nghiệp vụ.  
> **Phạm vi:** Một tòa nhà phòng trọ, một chủ trọ quản trị, bài tập nhóm kết thúc môn Spring Boot, sáu thành viên, thời gian triển khai dự kiến 10 ngày.

## 0. Cách sử dụng tài liệu

Tài liệu này là **nguồn tham chiếu nghiệp vụ chung** trước khi các thành viên thiết kế database, giao diện, controller, service hoặc chia công việc. Nó ghi nhận **hệ thống phải làm gì** và **các ràng buộc phải giữ đúng**, không ấn định sẵn tên bảng, tên class, endpoint hay chi tiết cấu trúc code.

- Khi thiết kế hoặc code một Use Case, trước tiên tìm UC tương ứng tại mục 9, sau đó kiểm tra Business Rules và Workflow liên quan.
- Không tự tạo quy tắc mới chỉ vì một màn hình hoặc database đang thiếu trường.
- Nếu phải chọn một chi tiết kỹ thuật chưa được quy định, ghi quyết định vào mục 12 trước khi nhiều người cùng triển khai.
- Các bản phác thảo/đề xuất ban đầu chỉ có giá trị tham khảo; những quyết định trong tài liệu tổng hợp này được ưu tiên.
- Trước lần phát hành đầu tiên, cập nhật trực tiếp bản 1.0 này; sau khi phát hành, thay đổi được chấp thuận phải cập nhật phiên bản.
- Đọc cùng [database_design.md](database_design.md) về ERD, cột và constraint, và [MODULE_HANDOFF_AND_IMPLEMENTATION_V2.md](MODULE_HANDOFF_AND_IMPLEMENTATION_V2.md) về phân công. Ba tài liệu này thay thế các bản phác thảo trước trong hội thoại.

## 1. Business Context và phạm vi

### 1.1. Bài toán

Xây dựng website phục vụ **chủ trọ trực tiếp quản lý hoạt động cho thuê phòng trong một tòa nhà của mình**. Đây không phải nền tảng trung gian kết nối nhiều chủ nhà. Chủ trọ đồng thời là Admin của hệ thống.

Khách có thể xem phòng còn cho thuê, tự tạo tài khoản và đăng ký thuê. Admin xét duyệt đơn, quản lý hợp đồng và phòng, ghi nhận nghiệp vụ đã làm trực tiếp ngoài đời, thu tiền, xử lý yêu cầu hỗ trợ và gửi thông báo. Người thuê đứng tên hợp đồng sử dụng tài khoản để theo dõi đơn, hợp đồng, hóa đơn, thanh toán online qua sandbox và gửi phản ánh.

### 1.2. Phạm vi đã xác nhận

| Khía cạnh | Quyết định |
|---|---|
| Đơn vị cho thuê | **Phòng (Room)**, không phải từng giường/chỗ ở |
| Đơn vị quản lý | Một tòa nhà, một chủ trọ/Admin |
| Số người ở | Một phòng có thể có nhiều người, theo giới hạn sức chứa |
| Người đại diện | Chỉ người đứng tên hợp đồng có tài khoản và quyền thanh toán cho phòng |
| Cho thuê | Có luồng đăng ký online và luồng Admin tạo hợp đồng trực tiếp |
| Hợp đồng | Một phòng chỉ có một hợp đồng còn hiệu lực/được đặt trước tại một thời điểm; một người có thể đứng tên nhiều hợp đồng cho nhiều phòng |
| Thu tiền | Theo phòng, thu sau thời gian sử dụng, ngày 10 dương lịch hằng tháng |
| Điện nước | Quản lý khoản tiền điện và nước riêng theo từng phòng, Admin nhập số tiền thực tế |
| Thanh toán | Sandbox online hoặc Admin ghi nhận khoản đã thu ngoài hệ thống |
| Dashboard Admin | Đếm Room theo trạng thái; cộng totalAmount của Invoice PAID; đếm Invoice UNPAID/OVERDUE và Issue chưa RESOLVED từ bảng hiện có, không tạo Entity/module mới |
| Công nghệ dự kiến | Spring Boot MVC, Thymeleaf, SQL Server, cổng thanh toán sandbox |

### 1.3. Ngoài phạm vi / không triển khai theo quyết định đã chốt

- Không tạo nền tảng đa chủ trọ, không có actor bên thứ ba quản lý việc cho thuê.
- Không có đặt cọc; không xử lý hoàn cọc.
- Không thanh toán một phần hoặc trả góp một hóa đơn.
- Không tự suy ra số tiền điện nước từ chỉ số công tơ và bảng giá; Admin nhập **số tiền**.
- Không tạo tài khoản, hợp đồng hoặc quyền thanh toán cho người ở cùng không đứng tên.
- Không quản lý lịch sử ra/vào của từng người ở cùng qua các giai đoạn thời gian.
- Không cần một Payment entity hoặc bảng lịch sử từng lần thử sandbox thành công/thất bại; lịch sử đã thanh toán được truy xuất từ Invoice.
- Không tự động chỉnh sửa các dữ kiện thực tế để hợp thức hóa trường hợp nhập hợp đồng trễ so với ngày bắt đầu cho phép.
- Không dùng xóa vật lý thay cho kết thúc Contract hoặc chuyển Room INACTIVE; không có UC xóa mới. Bảo toàn lịch sử Contract/Invoice, xem xét từng Entity và quan hệ khi có nhu cầu xóa dữ liệu chưa dùng.
- Không sửa tiền/hủy/thay thế Invoice đã phát hành. Sai sót làm việc trực tiếp ngoài hệ thống, giữ nguyên số tiền; không có luồng điều chỉnh hoặc hoàn tiền trong bản đầu.
- Chưa có outbox cho Notification; retry có giới hạn không bảo đảm tự gửi bù khi ứng dụng dừng đột ngột.
- Không gia hạn hoặc sửa ngày kết thúc đã chốt của Contract; nếu tiếp tục thuê thì tạo lần thuê/Contract mới theo điều kiện tạo mới.
- Không xác minh email/OTP, không đính kèm file/ảnh cho Issue, không triển khai gallery ảnh phòng hay kênh thông báo khác. Đổi/đặt lại mật khẩu là NICE TO HAVE, không thuộc 40 UC bắt buộc.

## 2. Actor và quyền nghiệp vụ

| Actor | Bản chất | Hành động chính |
|---|---|---|
| **Guest** | Khách chưa đăng nhập | Xem phòng AVAILABLE, tìm kiếm, xem chi tiết, đăng ký tài khoản |
| **User / Tenant Representative** | Người dùng đã đăng ký; khi có hợp đồng là người đại diện thuê | Gửi/rút/theo dõi đơn; xem hợp đồng, hóa đơn; thanh toán; xem lịch sử; tạo/theo dõi Issue; xem thông báo |
| **Admin / Landlord** | Một chủ trọ duy nhất | Quản lý phòng, xét duyệt đơn, tạo/kết thúc hợp đồng, quản lý người ở cùng, nhập điện nước, ghi nhận thu tiền thực tế, xử lý Issue và gửi thông báo |
| **Scheduler / System** | Tác nhân tự động, không phải tài khoản người thật | Kích hoạt/kết thúc hợp đồng theo ngày; tạo/kiểm tra kỳ thu; kiểm tra hóa đơn quá hạn; sinh sự kiện và thông báo hệ thống |
| **Sandbox Payment Gateway** | Hệ thống thanh toán ngoài ứng dụng | Tiếp nhận yêu cầu thanh toán và cung cấp kết quả để hệ thống xác minh |

**Không phải actor:** Người ở cùng không đứng tên. Họ chỉ là thông tin đi kèm do Admin quản lý; không đăng nhập, không gửi đơn và không được thanh toán hóa đơn qua tài khoản riêng.

### 2.1. Quyền lực của Admin

Admin có thể **ghi nhận lại nghiệp vụ đã diễn ra ngoài đời**: cho thuê trực tiếp, thu tiền trực tiếp, kết thúc hợp đồng, ngừng cung cấp phòng. Tuy nhiên Admin **không có quyền bỏ qua quan hệ và quy tắc dữ liệu** bằng cách sửa status tùy tiện.

Ví dụ:

- Muốn đưa phòng RENTED thành INACTIVE phải hủy/kết thúc hợp đồng liên quan và quyết toán theo đúng lý do.
- Muốn ghi nhận hóa đơn PAID phải thực hiện thao tác xác nhận thu tiền hợp lệ.
- Khi Admin cho thuê trực tiếp thành công, các đơn PENDING khác của phòng phải được từ chối.
- Dữ liệu hợp đồng nhập trên web phải tuân theo giới hạn ngày được phép; nghiệp vụ thực tế tiến hành muộn mà không ghi nhận đúng là sai sót vận hành, không phải lý do tự động bỏ kiểm tra.

## 3. Business Objects và quan hệ

| Business Object | Ý nghĩa | Quy tắc quan trọng |
|---|---|---|
| **User** | Tài khoản đăng nhập của Tenant/Admin | Người dùng tự đăng ký; một người có thể đứng tên nhiều hợp đồng |
| **Room** | Một phòng vật lý trong tòa nhà | Có sức chứa, giá niêm yết dương, tầng dương bắt buộc, diện tích dương nếu đã nhập, một ảnh đại diện và trạng thái; mỗi lúc tối đa một hợp đồng SCHEDULED/ACTIVE |
| **RentalApplication** | Đơn đề nghị thuê một phòng | Một đơn cho một phòng; duyệt thành công phải tạo được hợp đồng |
| **Contract** | Thỏa thuận thuê phòng có thời hạn | Một đại diện, một phòng, giá thuê đã chốt, ngày bắt đầu và ngày kết thúc |
| **Co-occupant information** | Danh sách người ở cùng (họ tên, CCCD…) | Chỉ Admin chỉnh sửa; không có tài khoản/quyền thanh toán/lịch sử thời gian riêng |
| **BillingCycle** | Một kỳ thu tiền định kỳ ngày 10 | Điều phối danh sách phòng/hợp đồng cần thu và việc nhập đủ điện nước |
| **BillingCycleItem** | Một nghĩa vụ của Contract trong kỳ, kèm dữ liệu chuẩn bị | Giữ điện nước đã nhập, phân biệt PENDING với INVOICED/NO_CHARGE; không phải hóa đơn chưa trả |
| **Invoice** | Khoản phải thu của một hợp đồng/phòng trong kỳ hoặc quyết toán | Gồm tiền thuê, điện, nước, tổng tiền, trạng thái thanh toán; đồng thời là căn cứ lịch sử thanh toán |
| **Issue** | Yêu cầu sửa chữa/phản ánh của người thuê | Gắn với người thuê và phòng/hợp đồng; Admin xử lý |
| **Notification** | Thông báo đến người dùng | Admin gửi thông báo thủ công; hệ thống phát sinh thông báo tự động; người nhận có trạng thái đọc |

### 3.1. Quan hệ và bất biến cốt lõi

1. Một Room có thể có nhiều Contract **qua lịch sử**, nhưng không được đồng thời có hai Contract mang trạng thái SCHEDULED hoặc ACTIVE.
2. Một Contract gắn đúng **một Room và một Tenant Representative**.
3. Một người đại diện có thể thuê nhiều phòng, nhưng mỗi phòng phải có **đơn/hợp đồng riêng**.
4. Khi tạo hợp đồng, **giá thuê chốt bằng giá niêm yết Room tại đúng thời điểm tạo**. Sau đó Contract giữ nguyên giá dù giá Room có thể thay đổi khi không còn hợp đồng SCHEDULED/ACTIVE.
5. Một Contract có thể phát sinh nhiều Invoice qua các kỳ; Invoice quyết toán sớm không được trùng khoản đã lập cho cùng khoảng thời gian.
6. Người ở cùng không phải người đứng tên hợp đồng. Nếu người đại diện trả phòng/chấm dứt hợp đồng thì quyền ở theo hợp đồng của toàn bộ người ở cùng cũng chấm dứt.
7. Invoice có thể tiếp tục UNPAID/OVERDUE sau khi Contract đã ENDED và Room đã AVAILABLE/INACTIVE.

## 4. Business Rules đã chốt

### 4.1. Tài khoản, người thuê và người ở cùng

| ID | Quy tắc |
|---|---|
| BR-ACC-01 | Guest được xem phòng AVAILABLE và tự đăng ký tài khoản. |
| BR-ACC-02 | Chỉ người đại diện đứng tên hợp đồng được thanh toán hóa đơn của hợp đồng đó. |
| BR-ACC-03 | Người ở cùng không có tài khoản riêng và không có quyền thanh toán. |
| BR-ACC-04 | Chỉ Admin được thêm/chỉnh sửa/xóa danh sách người ở cùng khi Contract chưa ENDED/CANCELLED; không khai báo trùng CCCD trong cùng Contract. |
| BR-ACC-05 | Số người khai báo trên web không được vượt quá sức chứa phòng. Trường hợp thực tế đặc biệt ngoài giới hạn được chủ trọ giải quyết trực tiếp, không làm hệ thống tự bỏ giới hạn. |
| BR-ACC-06 | Danh sách người ở cùng chỉ phản ánh thông tin đi kèm hiện tại, không quản lý biến động theo mốc thời gian. |
| BR-ACC-07 | Email duy nhất là định danh đăng nhập và **phân biệt chữ hoa/thường trên toàn địa chỉ**: `An@example.com` khác `an@example.com`. Bỏ khoảng trắng đầu/cuối trước khi lưu và tìm kiếm, không chuyển email thành chữ thường. Đăng ký bắt buộc email, mật khẩu, họ tên, điện thoại và CCCD; chỉ tạo USER. |
| BR-ACC-08 | CCCD bắt buộc ngay khi đăng ký tài khoản User; dữ liệu seed Admin cũng phải có CCCD. M04 vẫn kiểm tra danh tính hợp lệ trước khi tạo Contract, nhưng không có luồng chờ bổ sung CCCD sau đăng ký. Người ở cùng bắt buộc họ tên và CCCD; điện thoại tùy chọn. Không xác thực CCCD qua dịch vụ bên thứ ba. |
| BR-ACC-09 | Đúng một tài khoản ADMIN được tạo sẵn; không đăng ký Admin công khai, User không tự sửa role. |
| BR-ACC-10 | User cập nhật họ tên/CCCD qua hồ sơ của mình; màn hình hợp đồng kể cả lịch sử hiển thị dữ liệu User hiện tại qua tenant_id. Không lưu snapshot họ tên/CCCD trên Contract; cập nhật hồ sơ không đổi người đứng tên, giá hoặc điều khoản Contract. |

### 4.2. Phòng và trạng thái

| ID | Quy tắc |
|---|---|
| BR-ROOM-01 | AVAILABLE: phòng chưa có hợp đồng SCHEDULED/ACTIVE, sẵn sàng cho thuê và được hiển thị công khai. |
| BR-ROOM-02 | RENTED: phòng đang được giữ cho hợp đồng SCHEDULED hoặc đang có hợp đồng ACTIVE; không xuất hiện trong đề xuất phòng trống và không nhận hợp đồng mới. |
| BR-ROOM-03 | INACTIVE: phòng ngừng cung cấp cho thuê (bảo trì hoặc chủ trọ chủ động đóng), không hiển thị công khai và không có hợp đồng SCHEDULED/ACTIVE. |
| BR-ROOM-04 | Chuyển RENTED → INACTIVE bắt buộc đi cùng hủy SCHEDULED hoặc kết thúc ACTIVE, không được chỉ sửa Room.status. |
| BR-ROOM-05 | Khi hợp đồng hết hạn tự nhiên, hệ thống tự đưa phòng về AVAILABLE; trạng thái INACTIVE chỉ được Admin chủ động thiết lập và phải được Admin mở lại. |
| BR-ROOM-06 | Khi hợp đồng ACTIVE kết thúc sớm do Admin ngừng cung cấp, phải quyết toán theo số ngày thực tế trước khi hoàn tất workflow ngừng cho thuê. |
| BR-ROOM-07 | Thao tác giữ/giải phóng phòng phải ngăn hai hợp đồng cùng chiếm một phòng, kể cả hai yêu cầu xử lý gần như đồng thời. |
| BR-ROOM-08 | Giá niêm yết phải **> 0** và chỉ được đổi khi Room không có Contract SCHEDULED/ACTIVE. Giá Contract đã tạo không đổi; khi duyệt đơn hoặc thuê trực tiếp lấy giá Room tại thời điểm tạo Contract, không lấy giá cũ lúc gửi đơn. |
| BR-ROOM-09 | Mỗi Room có một ảnh đại diện; lưu thông tin tham chiếu ảnh, không yêu cầu gallery. Tầng bắt buộc và **> 0**; diện tích m² có thể chưa được nhập, nhưng nếu có thì phải **> 0**. UC05–UC07 hiển thị/lọc theo dữ liệu thực tế; không tạo giá trị diện tích giả khi chưa có dữ liệu. Tiện ích có thể mô tả bằng description, không có Amenity Entity. |

### 4.3. Đơn đăng ký thuê

| ID | Quy tắc |
|---|---|
| BR-APP-01 | Chỉ User đã đăng nhập mới được gửi đơn thuê phòng. |
| BR-APP-02 | Mỗi đơn ứng với đúng một phòng. Có thể gửi các đơn khác nhau cho nhiều phòng. |
| BR-APP-03 | Người gửi được tự rút đơn khi còn PENDING; sau khi được xử lý thì không rút theo luồng này. |
| BR-APP-04 | Admin duyệt hoặc từ chối đơn PENDING. Duyệt chỉ được coi là thành công nếu tạo hợp đồng và giữ phòng thành công. |
| BR-APP-05 | Sau khi một phòng được cho thuê thành công, hệ thống tự REJECT các đơn PENDING cạnh tranh của phòng, kể cả trường hợp Admin tạo hợp đồng trực tiếp. |
| BR-APP-06 | Đơn APPROVED là bản ghi lịch sử việc xét duyệt; hợp đồng về sau ENDED/CANCELLED không tự đổi đơn APPROVED trở lại PENDING. |
| BR-APP-07 | Nếu không thể tạo hợp đồng/giữ phòng, không được để lại đơn APPROVED mà thiếu hợp đồng hợp lệ. |
| BR-APP-08 | Mỗi User–Room có tối đa một đơn PENDING; được gửi lại sau khi đơn cũ đã xử lý nếu Room vẫn AVAILABLE. |
| BR-APP-09 | Đơn PENDING có ngày bắt đầu trước ngày nghiệp vụ hiện tại tự REJECTED, ghi rõ lý do; User gửi đơn mới. Không tự dịch ngày. Ngày bắt đầu bằng hôm nay vẫn hợp lệ; khi duyệt phải kiểm tra lại dù Scheduler chưa xử lý. |

### 4.4. Hợp đồng

| ID | Quy tắc |
|---|---|
| BR-CON-01 | Ngày bắt đầu thuê chỉ được chọn từ **ngày hiện tại đến tối đa 10 ngày trong tương lai**, không cho chọn ngày quá khứ. Quy tắc áp dụng cả khi Admin ghi nhận thuê trực tiếp. |
| BR-CON-02 | Người dùng/Admin chọn **tháng và năm kết thúc**, hệ thống đặt ngày kết thúc thành **ngày 10** của tháng đã chọn. |
| BR-CON-03 | Khi tạo hợp đồng có ngày bắt đầu trong tương lai, trạng thái là SCHEDULED; đến ngày bắt đầu hệ thống chuyển ACTIVE. |
| BR-CON-04 | Hợp đồng ACTIVE tự chuyển ENDED khi đến hạn tự nhiên, đồng thời cập nhật phòng. |
| BR-CON-05 | Hợp đồng SCHEDULED được Admin hủy trước ngày bắt đầu: chuyển CANCELLED, không phát sinh tiền thuê do chưa vào ở và giải phóng phòng theo nghiệp vụ. |
| BR-CON-06 | Admin được tạo hợp đồng trực tiếp không qua RentalApplication để ghi nhận việc cho thuê đã thỏa thuận ngoài đời, nhưng người đại diện phải có tài khoản và dữ liệu phải tuân theo BR-CON-01. |
| BR-CON-07 | Không có tiền cọc. |
| BR-CON-08 | Một hợp đồng gắn một người đại diện và một phòng; nhiều người ở cùng không trở thành bên thanh toán. |
| BR-CON-09 | Khi tạo Contract, `contracts.agreed_monthly_rent` được sao từ `rooms.listed_monthly_rent` hiện hành và phải **> 0**; Admin không nhập giá khác. Giá này dùng cho mọi kỳ và không đổi theo giá Room về sau. Application không lưu giá cố định khi gửi đơn. Giá tháng dương không ngăn khoản quyết toán của một kỳ có tổng bằng 0 và ghi NO_CHARGE theo các quy tắc thu tiền. |
| BR-CON-10 | Admin kết thúc ACTIVE trước hạn phải chọn/ghi nhận đúng lý do vì chính sách tiền kỳ cuối phụ thuộc vào lý do. |
| BR-CON-11 | Contract SCHEDULED tự ACTIVE khi tới ngày bắt đầu, không phụ thuộc khách có đến nhận phòng. Không tự hủy nếu khách không đến; Admin xử lý chủ động qua workflow hiện có, không thêm status/lý do kết thúc/chính sách miễn tiền mới. |
| BR-CON-12 | Không gia hạn Contract và không sửa `planned_end_date` đã chốt. Đến hạn thì ENDED và Room được giải phóng; nếu tiếp tục thuê sau đó thì tạo lần thuê/Contract mới theo điều kiện tạo mới. |
| BR-CON-13 | Admin chỉ kết thúc ACTIVE trước hạn với `effective_end_date` bằng ngày nghiệp vụ hiện tại theo UTC+7; không backdate hoặc dùng luồng này để đặt lịch kết thúc tương lai. |

Không áp thêm thời hạn thuê tối thiểu ngoài điều kiện `planned_end_date > start_date` và các giới hạn ngày tạo Contract đã nêu.

### 4.5. Kỳ thu, điện nước và Invoice

| ID | Quy tắc |
|---|---|
| BR-BILL-01 | Thu tiền thuê **sau thời gian sử dụng**, vào **ngày 10 dương lịch hằng tháng**. Kỳ thu dựa trên chu kỳ ngày 10 (cycle10). |
| BR-BILL-02 | Kỳ thông thường là [ngày 10 kỳ trước, ngày 10 kỳ tiếp theo): tính ngày đầu, không tính ngày cuối. Ngày nghiệp vụ theo UTC+7; timestamp lưu UTC. Trả phòng đúng ngày 10 không phát sinh tiền thuê kỳ mới. |
| BR-BILL-03 | Với **kỳ đầu tiên dưới 14 ngày sử dụng**, tiền thuê = giá thuê tháng / 30 × số ngày đã ở. Từ 14 ngày trở lên thì áp dụng giá thuê tháng đầy đủ. |
| BR-BILL-04 | Sau kỳ đầu tiên, tiền thuê định kỳ sử dụng giá thuê tháng của hợp đồng; cộng tiền điện và nước của phòng. |
| BR-BILL-05 | Admin tính điện nước bên ngoài rồi nhập số tiền cuối cùng. Nếu phòng đổi hợp đồng trong kỳ, Admin chốt riêng tiền của từng hợp đồng; hệ thống không lưu công tơ và không tự chia theo số ngày. |
| BR-BILL-06 | Đến kỳ thu, hệ thống tạo/kiểm tra kỳ và yêu cầu Admin nhập đủ điện nước cho **toàn bộ phòng thuộc kỳ**. Khi Admin đăng nhập còn kỳ chưa hoàn thành, giao diện phải khóa chặt vào bước nhập bắt buộc trước khi vào các chức năng thông thường. |
| BR-BILL-07 | Sau khi nhập đủ, hệ thống tính tiền thuê, cộng điện nước, phát hành Invoice UNPAID cho khoản tổng lớn hơn 0 và thông báo người thuê. Tổng bằng 0 được ghi nhận NO_CHARGE, không phát hành Invoice. |
| BR-BILL-08 | Không tạo trùng BillingCycle/Invoice nếu Scheduler chạy lại, server khởi động lại hoặc Admin đăng nhập nhiều lần. |
| BR-BILL-09 | Một Invoice thu toàn bộ số tiền của nó; không cho thanh toán một phần hoặc chia đợt. |
| BR-BILL-10 | Chỉ người đại diện được bắt đầu sandbox; Invoice phải tồn tại, đúng chủ, UNPAID và thời điểm hiện tại phải trước dueAt. Không chỉ dựa vào Scheduler đổi status để chặn quá hạn. |
| BR-BILL-11 | dueAt = issuedAt + đúng 48 giờ. Từ dueAt mà chưa trả, Invoice chuyển OVERDUE; xử lý trực tiếp với Admin, không mở sandbox thông thường. Notification chậm/retry không dịch dueAt. |
| BR-BILL-12 | Admin có thể ghi nhận khoản tiền đã thu ngoài đời để chuyển Invoice từ UNPAID hoặc OVERDUE sang PAID. |
| BR-BILL-13 | Sandbox thất bại không khiến hóa đơn PAID; nếu còn điều kiện thanh toán, người thuê có thể thử lại. Không lưu từng lần thử như một Payment entity riêng. |
| BR-BILL-14 | Kết quả sandbox phải được hệ thống xác minh; callback lặp hoặc thao tác thu tiền đồng thời không được ghi nhận PAID hai lần. |
| BR-BILL-15 | Invoice lưu trạng thái, tổng tiền và thông tin xác nhận thanh toán tối thiểu như paidAt, phương thức thanh toán và mã tham chiếu nếu cần. |
| BR-BILL-16 | Lịch sử thanh toán của người thuê là **lịch sử Invoice PAID**; không phải nhật ký tất cả yêu cầu sandbox thất bại. |
| BR-BILL-17 | Chính sách “không cho nợ” được triển khai bằng việc **theo dõi và tra cứu UNPAID/OVERDUE để chủ trọ làm việc trực tiếp** (track_unpaid), không tự xóa nợ hay giả định đã thanh toán. |
| BR-BILL-18 | Tiền là đồng nguyên. Giữ chính xác trung gian, làm tròn khoản tiền thuê cuối theo HALF_UP (từ 0,5 đồng lên), không làm tròn giá một ngày trước khi nhân. Điện nước nhập đồng nguyên; tổng bằng tổng các khoản đã chốt. |
| BR-BILL-19 | Admin xem lại/sửa điện nước trước phát hành. Sau phát hành không sửa tiền Invoice; sai sót giải quyết trực tiếp, giữ nguyên số tiền trên hệ thống. Trạng thái vẫn cập nhật theo nghiệp vụ thanh toán. |
| BR-BILL-20 | PAID vẫn nghĩa là nhận đủ số tiền trên Invoice. Nếu thỏa thuận ngoài đời khác số tiền này, hệ thống không tự đánh dấu PAID/xóa nợ; công nợ trên hệ thống có thể khác thỏa thuận. Đây là giới hạn bản đầu. |
| BR-BILL-21 | Callback lặp không tạo hiệu ứng lần hai. Callback thành công muộn khi Invoice đã OVERDUE, hoặc xung đột với khoản đã xác nhận, được xác minh để Admin đối chiếu qua nguồn gateway/log vận hành; không tự ghi đè PAID hoặc mở ngoại lệ OVERDUE → PAID qua callback. Không coi kết quả thành công muộn là thất bại rồi bỏ dấu giao dịch. |
| BR-BILL-22 | Chỉ một paymentMethod: SANDBOX, CASH hoặc BANK_TRANSFER (chuyển khoản trực tiếp Admin xác nhận). Lưu tham chiếu thành công nếu có; không lưu mọi lần thử sandbox. |
| BR-BILL-23 | Khi gateway xác nhận đã thu đủ nhưng callback trễ/thất lạc hoặc xung đột Invoice, Admin được xem và đối chiếu giao dịch đã xác minh. Nếu Invoice chưa PAID, M05 được ghi nhận PAID đúng nguồn SANDBOX theo xác nhận của Admin; nếu đã PAID, không ghi đè dữ liệu cũ. Hai khoản tiền thực tế được xử lý ngoài hệ thống. Không tạo Payment/PaymentAttempt hoặc lịch sử mọi lần thử. |

### 4.6. Kết thúc hợp đồng và quyết toán

| ID | Quy tắc |
|---|---|
| BR-TERM-01 | Nếu **Tenant chủ động trả phòng trước hạn** trong kỳ đang sử dụng, tiền thuê kỳ đó **thu nguyên kỳ**, cộng tiền điện nước thực tế đã chốt; trường hợp đúng ranh giới ngày 10 áp dụng ngoại lệ BR-TERM-09. |
| BR-TERM-02 | Nếu **Admin chủ động ngừng cung cấp phòng** và kết thúc ACTIVE, tiền thuê kỳ cuối = giá thuê tháng / 30 × số ngày thực tế sử dụng trong kỳ, cộng tiền điện nước thực tế. Quy tắc này được ưu tiên so với công thức nguyên kỳ. |
| BR-TERM-03 | Nếu hủy SCHEDULED trước ngày bắt đầu, không có tiền thuê đã phát sinh nên không tạo hóa đơn tiền thuê kỳ cuối. |
| BR-TERM-04 | Kết thúc sớm phải quyết toán ngay: phát hành Invoice nếu tổng > 0, ghi NO_CHARGE nếu tổng = 0; không đợi kỳ thu sau để tạo thêm khoản trùng. Ngoại lệ Tenant trả đúng ngày 10 tại BR-TERM-09 do kỳ thu REGULAR vừa kết thúc xử lý, có thể phát hành sau khi Contract ENDED nếu điện nước chưa chốt. |
| BR-TERM-05 | Do hệ thống thu tiền sau khi ở, không có tình huống hoàn lại tiền thuê trả trước cho kỳ đang sử dụng; trọng tâm là **thu đủ khoản kỳ cuối**. |
| BR-TERM-06 | Hợp đồng có thể ENDED và phòng có thể AVAILABLE/INACTIVE dù Invoice quyết toán còn UNPAID/OVERDUE; khoản phải thu tiếp tục tồn tại để Admin theo dõi. |
| BR-TERM-07 | Hết hạn tự nhiên đúng lịch đã chọn không bị xem là Tenant chủ động kết thúc sớm; áp dụng xử lý kết thúc bình thường và kỳ thu phù hợp. |
| BR-TERM-08 | Không tính ngày trả phòng. Khoảng 0 ngày không tự miễn chính sách nguyên kỳ nếu Tenant bắt đầu thuê rồi trả sớm **trong cùng kỳ**; ngoại lệ ngày 10 sau kỳ đã sử dụng theo BR-TERM-09. Admin ngừng cung cấp tính ngày thực tế, cộng điện nước. Tổng thực sự bằng 0 thì không tạo Invoice nhưng phải ghi kết quả quyết toán để chạy lại an toàn. |
| BR-TERM-09 | **Ngoại lệ tại ranh giới kỳ:** Tenant chủ động kết thúc trước `planned_end_date` đúng ngày 10, sau khi đã sử dụng kỳ vừa kết thúc nhưng chưa dùng ngày nào của kỳ mới, không phát sinh tiền thuê kỳ mới. Kỳ vừa kết thúc do xử lý REGULAR tương ứng (Invoice nếu tổng > 0, NO_CHARGE nếu tổng = 0), kể cả khi Invoice đã phát hành. Không tính lại theo quy tắc nguyên kỳ, không sửa Invoice đã phát hành, không tạo Invoice EARLY_SETTLEMENT thứ hai cùng Contract/mốc kỳ. Điện nước kỳ cũ phải được nhập/chốt và tính đúng một lần trước phát hành; nếu phát hiện sai sót sau phát hành, áp dụng BR-BILL-19/20. |

### 4.7. Issue và Notification

| ID | Quy tắc |
|---|---|
| BR-ISSUE-01 | Người đại diện có hợp đồng ACTIVE có thể gửi Issue liên quan đến phòng đang thuê. |
| BR-ISSUE-02 | Người thuê được theo dõi Issue của mình; Admin được xem và chuyển trạng thái xử lý. |
| BR-ISSUE-03 | Issue đã tạo vẫn được lưu/xem trong lịch sử sau khi hợp đồng kết thúc. |
| BR-ISSUE-04 | Contract kết thúc không tự đóng Issue đã tạo hợp lệ. Admin tiếp tục xử lý Issue OPEN/IN_PROGRESS đến RESOLVED; Tenant vẫn xem lịch sử của mình. Không thêm trạng thái Issue hoặc file/ảnh đính kèm. |
| BR-NOTI-01 | Chỉ Admin có quyền tự soạn và phát thông báo thủ công cho người thuê. |
| BR-NOTI-02 | Hệ thống được tự động tạo thông báo từ sự kiện nghiệp vụ như duyệt đơn, phát hành hóa đơn, quá hạn, thay đổi xử lý Issue. |
| BR-NOTI-03 | Mỗi người nhận có trạng thái thông báo UNREAD hoặc READ. |
| BR-NOTI-04 | Lỗi gửi/lưu thông báo tự động không được làm biến một Invoice/hợp đồng đã tạo hợp lệ thành thất bại; cần ghi nhận để xử lý lại. |
| BR-NOTI-05 | Một bản thông báo thuộc một người nhận. Tạo tự động sau commit, lỗi ghi log và retry có giới hạn; chống trùng sự kiện–người nhận. Chưa có outbox, chấp nhận giới hạn phục hồi khi ứng dụng dừng; người thuê vẫn xem được dữ liệu gốc ở trang nghiệp vụ. |
| BR-NOTI-06 | Giữ UC39/UC40 và thông báo nội bộ website. Không thêm email/SMS/push hoặc nghiệp vụ thông báo khác trong bản này. |

## 5. Trạng thái các đối tượng

### 5.1. Room

| Status | Ý nghĩa | Chuyển trạng thái hợp lệ tiêu biểu |
|---|---|---|
| AVAILABLE | Được công khai, có thể tiếp nhận đăng ký/cho thuê | → RENTED khi hợp đồng được tạo; → INACTIVE khi Admin đóng phòng trống |
| RENTED | Có hợp đồng SCHEDULED/ACTIVE, không còn được chào thuê | → AVAILABLE khi hợp đồng hủy/kết thúc bình thường; → INACTIVE qua workflow Admin ngừng cung cấp |
| INACTIVE | Ngừng cung cấp cho thuê, không có hợp đồng còn hiệu lực/được đặt trước | → AVAILABLE khi Admin chủ động mở lại |

**Bất biến:** Không bao giờ có Room INACTIVE và Contract SCHEDULED/ACTIVE cùng tồn tại cho phòng đó.

### 5.2. RentalApplication

| Status | Ai/tác nhân đưa đến trạng thái này |
|---|---|
| PENDING | Người dùng gửi đơn |
| APPROVED | Admin duyệt và hệ thống tạo hợp đồng/giữ phòng thành công |
| REJECTED | Admin từ chối hoặc hệ thống từ chối đơn cạnh tranh |
| WITHDRAWN | Người gửi tự rút khi còn PENDING |

Chuyển trạng thái từ PENDING sang một trạng thái kết thúc. Không tự sửa APPROVED chỉ vì hợp đồng sau này kết thúc.

### 5.3. Contract

| Status | Ý nghĩa | Luồng chính |
|---|---|---|
| SCHEDULED | Đã ký/được tạo nhưng chưa tới ngày bắt đầu | → ACTIVE vào ngày bắt đầu; → CANCELLED nếu hủy trước ngày |
| ACTIVE | Đang trong thời gian thuê | → ENDED khi đúng hạn hoặc kết thúc sớm |
| ENDED | Đã kết thúc, giữ lịch sử | Trạng thái kết thúc |
| CANCELLED | Hủy trước khi bắt đầu | Trạng thái kết thúc |

Lý do kết thúc sớm (**TENANT_EARLY_EXIT**, **ADMIN_STOP_SERVICE**) là thông tin nghiệp vụ riêng, không cần tạo thêm trạng thái Contract.

### 5.4. Invoice

| Status | Ý nghĩa | Luồng chính |
|---|---|---|
| UNPAID | Đã phát hành, còn được thanh toán online trong hạn | → PAID khi thanh toán hợp lệ; → OVERDUE khi quá hạn |
| OVERDUE | Chưa thanh toán sau hạn; làm việc trực tiếp với Admin | → PAID khi Admin xác nhận thu tiền thực tế |
| PAID | Đã ghi nhận thanh toán toàn bộ | Trạng thái kết thúc thông thường |

Thiết kế 1.0 không lưu Invoice DRAFT: dữ liệu chuẩn bị nằm ở BillingCycleItem; khi đủ điều kiện và tổng lớn hơn 0 thì tạo Invoice UNPAID.

### 5.5. BillingCycle, Issue và Notification

- **BillingCycle:** PENDING_INPUT → COMPLETED khi mọi nghĩa vụ có Invoice hoặc kết quả NO_CHARGE. COMPLETED không có nghĩa đã thu hết tiền. BillingCycleItem lưu dữ liệu nhập và kết quả PENDING / INVOICED / NO_CHARGE; phát hành lỗi giữ dữ liệu nhập để thử lại an toàn.
- **Issue:** OPEN → IN_PROGRESS → RESOLVED; Admin xử lý, người thuê theo dõi.
- **Notification:** UNREAD → READ, theo từng người nhận.

## 6. Workflow nghiệp vụ chính

### WF-01. Khách đăng ký thuê online

1. Guest xem/tìm kiếm chi tiết các Room AVAILABLE, gồm tầng, ảnh đại diện và diện tích nếu đã có dữ liệu; không hiển thị diện tích giả.
2. Guest đăng ký bằng email, mật khẩu, họ tên, điện thoại và CCCD rồi đăng nhập để trở thành User; không có email/OTP verification.
3. User chọn một phòng và ngày bắt đầu hợp lệ, chọn tháng/năm kết thúc theo quy định.
4. Hệ thống ghi RentalApplication PENDING; User xem tình trạng và có thể rút trước khi được xử lý.
   - Chặn đơn PENDING thứ hai cùng User–Room; đơn quá ngày bắt đầu tự REJECTED, không tự sửa ngày.
5. Admin xem và duyệt/từ chối. Nếu duyệt: kiểm tra phòng và điều kiện hợp đồng tại thời điểm xử lý, lấy giá niêm yết Room **ngay lúc tạo Contract**, rồi tạo Contract và giữ Room trong cùng workflow nhất quán; không lấy một giá đã lưu ở Application.
6. Đơn duyệt thành APPROVED; Room RENTED; các đơn PENDING cạnh tranh tự REJECT.
7. Contract ACTIVE nếu bắt đầu ngay, hoặc SCHEDULED nếu bắt đầu trong tương lai. Scheduler chuyển SCHEDULED → ACTIVE đúng ngày.
8. Hệ thống phát sinh thông báo phù hợp.

**Trường hợp lỗi:** Tạo hợp đồng/giữ phòng thất bại phải không để đơn APPROVED giả; xử lý đồng thời hai đơn cùng phòng không được tạo hai hợp đồng chiếm phòng.

### WF-02. Admin cho thuê trực tiếp ngoài đời

1. Khách trao đổi trực tiếp với chủ trọ và có tài khoản User.
2. Admin chọn Room AVAILABLE và User đại diện.
3. Admin nhập thông tin Contract, tuân thủ ngày bắt đầu hiện tại đến +10 ngày và quy định ngày kết thúc; giá thuê được lấy tự động từ Room.listedMonthlyRent, không có ô nhập giá khác.
4. Hệ thống tạo Contract, chuyển Room RENTED, từ chối các đơn PENDING cạnh tranh.
5. SCHEDULED/ACTIVE và quá trình kích hoạt về sau giống luồng online.

**Không có yêu cầu** phải tạo một RentalApplication giả cho nghiệp vụ trực tiếp. Không cho phép backdate chỉ vì thực tế Admin nhập muộn.

### WF-03. Kích hoạt và kết thúc tự nhiên

1. Scheduler kiểm tra hợp đồng SCHEDULED đến ngày bắt đầu → ACTIVE, dù khách chưa đến nhận phòng; không tự hủy vì khách vắng mặt. Nếu không tiếp tục thuê, Admin chủ động dùng workflow hủy/kết thúc phù hợp với trạng thái thực tế.
2. Khi đến ngày kết thúc đã xác định (ngày 10 tháng/năm kết thúc), Contract ACTIVE → ENDED.
3. Room được giải phóng về AVAILABLE theo quy tắc kết thúc thông thường. Không gia hạn hay sửa ngày kết thúc của Contract cũ; việc thuê tiếp là lần thuê mới theo điều kiện tạo mới.
4. Các khoản tiền chưa thu vẫn nằm trong Invoice và được theo dõi độc lập; kỳ thu cuối phải được xử lý mà không bỏ sót.

### WF-04. Tạo kỳ thu và phát hành Invoice

1. Đến ngày 10, Scheduler kiểm tra/tạo BillingCycle **một lần theo khóa kỳ**, kể cả khi thực thi lại.
2. Xác định các hợp đồng/phòng phải thu tiền của kỳ.
3. Admin đăng nhập gặp biểu mẫu nhập số tiền điện, nước **cho toàn bộ phòng cần thu**; chưa hoàn thành thì không đi tiếp đến giao diện quản trị thông thường.
4. Khi biểu mẫu hoàn tất, hệ thống áp dụng công thức tiền thuê (đặc biệt kỳ đầu dưới 14 ngày), cộng tiền điện và nước theo phòng.
5. Phát hành Invoice UNPAID đúng một lần cho nghĩa vụ tổng lớn hơn 0; tổng 0 ghi NO_CHARGE. Lưu điện nước trước bằng giao dịch riêng; giao dịch phát hành cập nhật Invoice/kết quả từng dòng và trạng thái kỳ nhất quán.
6. Gửi Notification tới người đại diện của hợp đồng.

**Ví dụ minh họa:** Giá hợp đồng 3.000.000đ/tháng, bắt đầu 01/11, kỳ đầu [01/11, 10/11) có 9 ngày: tiền thuê 3.000.000 / 30 × 9 = 900.000đ, cộng điện nước. Đây là quy ước biên ngày đã chốt.

### WF-05. Thanh toán sandbox

1. Tenant đăng nhập, xem Invoice của mình (UC27).
2. Khi yêu cầu online: chỉ Invoice UNPAID và thuộc hợp đồng mà Tenant đứng tên mới được thanh toán.
3. M05 tạo yêu cầu sandbox bằng số tiền và tham chiếu Invoice đã lưu.
4. Gateway phản hồi; M05 xác minh tính hợp lệ, mã hóa đơn, số tiền và kết quả.
5. Thành công: Invoice → PAID, lưu thời điểm/phương thức/tham chiếu tối thiểu. Thất bại: giữ UNPAID nếu còn trong hạn, cho phép thử lại.
6. Callback lặp không được ghi nhận lại khoản đã PAID.
7. Khi callback thành công trễ/thất lạc hoặc xung đột, Admin đối chiếu giao dịch đã xác minh theo BR-BILL-21/23. Nếu Invoice chưa PAID và đã nhận đủ, M05 ghi PAID với paymentMethod = SANDBOX qua thao tác xác nhận của Admin; nếu đã PAID thì giữ nguyên bản ghi. Khoản thu thực tế trùng được xử lý ngoài hệ thống.

### WF-06. Quá hạn và Admin thu tiền ngoài hệ thống

1. M06 kiểm tra thời hạn thanh toán; Invoice UNPAID quá 48 giờ được yêu cầu M05 chuyển OVERDUE.
2. Invoice OVERDUE không tiếp tục thanh toán sandbox theo luồng thông thường.
3. Admin tra cứu UNPAID/OVERDUE, trao đổi và thu tiền thực tế.
4. Sau khi nhận đủ tiền, Admin dùng UC30; M06 yêu cầu M05 ghi nhận Invoice PAID.
5. Invoice đã PAID xuất hiện trong lịch sử của Tenant và Admin.

### WF-07. Tenant chủ động kết thúc hợp đồng ACTIVE sớm

1. Admin ghi nhận yêu cầu trả phòng ACTIVE với reason = TENANT_EARLY_EXIT; `effective_end_date` chỉ bằng ngày nghiệp vụ hiện tại UTC+7, không nhập ngày quá khứ/tương lai.
2. Nếu kết thúc **trong kỳ đang sử dụng**, chốt tiền điện nước thực tế; M06 áp dụng **tiền thuê nguyên kỳ** và tính khoản quyết toán. M05 phát hành Invoice EARLY_SETTLEMENT kỳ cuối hoặc trả NO_CHARGE khi tổng thực sự bằng 0; không xử lý trùng vào ngày 10 sau đó.
3. Nếu kết thúc **đúng ngày 10 sau một kỳ đã sử dụng và chưa dùng ngày nào của kỳ mới**, không thu tiền thuê kỳ mới, không tạo EARLY_SETTLEMENT. Invoice REGULAR của kỳ vừa kết thúc xử lý tiền thuê và điện nước đúng một lần: nếu chưa phát hành thì Admin chốt đủ điện nước trước khi M05 phát hành, kể cả sau khi Contract đã ENDED; nếu đã phát hành thì giữ nguyên số tiền và áp dụng chính sách sai sót ngoài hệ thống khi cần.
4. M04 kết thúc Contract và M02 giải phóng Room theo workflow đồng bộ; nếu kỳ cũ chưa sẵn sàng phát hành, nghĩa vụ REGULAR vẫn được giữ để M05 xử lý sau. Nhánh ngày 10 không ghi dấu quyết toán sớm thay cho nghĩa vụ Invoice REGULAR.
5. Khoản chưa thu tiếp tục tồn tại để Admin theo dõi.

### WF-08. Admin ngừng cung cấp phòng đang RENTED

1. Admin yêu cầu Room RENTED → INACTIVE.
2. Nếu Contract SCHEDULED: hủy hợp đồng trước khi bắt đầu, không thu tiền thuê, giải phóng đặt trước và đưa phòng vào INACTIVE.
3. Nếu Contract ACTIVE: reason = ADMIN_STOP_SERVICE; chốt điện nước; M06 tính tiền thuê theo **số ngày thực tế trong kỳ**, cộng điện nước; M05 phát hành Invoice kỳ cuối hoặc trả NO_CHARGE khi tổng bằng 0.
4. M04 kết thúc Contract, M02 chuyển Room INACTIVE.
5. Room tiếp tục INACTIVE cho đến khi Admin chủ động mở lại; Invoice chưa thanh toán không bị xóa.

### WF-09. Issue và Notification

1. Tenant đang có Contract ACTIVE gửi Issue cho phòng liên quan.
2. Admin xem Issue, chuyển OPEN → IN_PROGRESS → RESOLVED và phản hồi theo chức năng triển khai; Contract kết thúc không tự đóng Issue còn mở, Admin vẫn xử lý tiếp.
3. Người thuê theo dõi và xem lại lịch sử Issue.
4. Admin có thể tạo thông báo thủ công (chỉ Admin).
5. Hệ thống có thể tự phát thông báo từ các sự kiện hợp lệ như duyệt đơn, phát hành hóa đơn, quá hạn, xử lý Issue.
6. Người nhận xem thông báo, chuyển UNREAD → READ.

## 7. Ma trận quyền Actor – Function

| Chức năng | Guest | Tenant Representative | Admin | Scheduler/Gateway |
|---|:---:|:---:|:---:|:---:|
| Xem phòng AVAILABLE, tìm kiếm, chi tiết | ✓ | ✓ | ✓ | — |
| Đăng ký tài khoản | ✓ | — | — | — |
| Gửi/rút đơn của mình | — | ✓ | — | — |
| Duyệt/từ chối đơn | — | — | ✓ | Tự từ chối đơn cạnh tranh |
| Tạo hợp đồng trực tiếp | — | — | ✓ | — |
| Xem hợp đồng của mình | — | ✓ | ✓ (toàn bộ) | — |
| Quản lý phòng, người ở cùng | — | — | ✓ | Đồng bộ theo hợp đồng |
| Nhập tiền điện nước | — | — | ✓ | Nhắc/tạo kỳ thu |
| Xem hóa đơn của mình | — | ✓ | ✓ (toàn bộ) | — |
| Thanh toán sandbox | — | ✓ (đúng hợp đồng) | — | Gateway phản hồi |
| Xem lịch sử Invoice PAID | — | ✓ (của mình) | ✓ (toàn bộ) | — |
| Xác nhận thu tiền ngoài hệ thống | — | — | ✓ | — |
| Tìm công nợ | — | — | ✓ | Quá hạn tự động |
| Xem Dashboard tổng hợp | — | — | ✓ | Đọc dữ liệu hiện có |
| Tạo/theo dõi Issue | — | ✓ (của mình) | ✓ (toàn bộ) | — |
| Xử lý Issue | — | — | ✓ | — |
| Gửi thông báo thủ công | — | — | ✓ | — |
| Xem/đánh dấu đã đọc Notification | — | ✓ | ✓ | Sinh thông báo tự động |

## 8. Quy định các trường hợp biên quan trọng

| Tình huống | Kết quả bắt buộc |
|---|---|
| Hai đơn PENDING của cùng phòng, Admin duyệt một đơn | Chỉ một Contract được tạo; đơn còn lại REJECTED; phòng RENTED |
| Admin cho thuê trực tiếp phòng đang có đơn PENDING | Contract trực tiếp hợp lệ; các đơn cạnh tranh REJECTED |
| Người gửi rút đơn PENDING | WITHDRAWN; không tạo Contract |
| Người thuê đổi ý khi Contract SCHEDULED chưa bắt đầu | Admin thực hiện hủy SCHEDULED; không phát sinh tiền thuê; Room được giải phóng |
| Admin muốn đưa phòng RENTED thành INACTIVE | Không sửa Room độc lập; phải xử lý Contract và quyết toán tương ứng |
| Admin sửa giá niêm yết phòng khi có Contract SCHEDULED/ACTIVE | Bị chặn; chỉ sửa khi không còn Contract giữ phòng. Contract lịch sử vẫn dùng giá đã chốt |
| Khách không đến nhận phòng đúng ngày bắt đầu | SCHEDULED vẫn → ACTIVE; không tự hủy, Admin xử lý chủ động theo workflow hiện có |
| Server/Scheduler chạy tạo kỳ thu nhiều lần | Không sinh trùng kỳ và Invoice |
| Admin đã nhập điện nước nhưng phát hành Invoice bị lỗi | Giữ dữ liệu đã nhập, có thể phát hành lại an toàn, không buộc nhập lại hoặc tạo trùng |
| Tenant chưa trả sau 48 giờ | UNPAID → OVERDUE, Admin thu trực tiếp |
| Sandbox trả thất bại | Không chuyển PAID, có thể thử lại khi còn UNPAID |
| Sandbox callback thành công bị gửi trùng | Không ghi nhận PAID hai lần |
| Sandbox thành công gần thời điểm Admin xác nhận đã thu | Chỉ một thao tác ghi nhận PAID trên Invoice; nếu thực tế hai khoản đã được thu, Admin đối chiếu ngoài hệ thống |
| Tenant hết hợp đồng nhưng còn khoản chưa trả | Contract ENDED; Invoice vẫn UNPAID/OVERDUE để Admin tra cứu |
| Tenant có nhiều hợp đồng/phòng | Xem được đơn, hợp đồng và Invoice thuộc tất cả hợp đồng mình đứng tên |
| Tenant cố xem Invoice của người khác | Từ chối truy cập |
| Người ở cùng muốn tự trả tiền trên web | Không được phép |
| User gửi đơn PENDING thứ hai cùng Room | Bị chặn; vẫn được gửi đơn cho phòng khác hoặc gửi lại sau khi đơn cũ đã xử lý |
| Đơn PENDING có ngày bắt đầu đã qua | Tự REJECTED, ghi lý do; không backdate Contract |
| Tenant trả sớm đúng ngày 10 sau kỳ đã sử dụng | Không phát sinh kỳ mới; kỳ trước do REGULAR Invoice xử lý đúng một lần, không tạo EARLY_SETTLEMENT hoặc sửa Invoice đã phát hành |
| Tổng khoản thu bằng 0 | Ghi NO_CHARGE, không tạo Invoice, không xử lý lại vô hạn |
| Sai tiền sau phát hành | Giữ nguyên tiền; xử lý ngoài đời không tự xóa công nợ/đánh dấu PAID |
| Notification chậm/retry | dueAt không đổi; chống trùng sự kiện–người nhận |
| Issue đang mở khi Contract kết thúc | Issue không tự đóng; Admin tiếp tục xử lý đến RESOLVED; Tenant còn xem được lịch sử |

## 9. Danh mục 40 Use Case chính thức

Danh mục này là **bản mục tiêu nghiệp vụ**, không tự ấn định số lượng trang giao diện: nhiều UC có thể nằm chung một màn hình nhưng phải đáp ứng đầy đủ chức năng.

| UC ID | Use Case | Actor chính | Kết quả nghiệp vụ |
|---|---|---|---|
| UC01 | Đăng ký tài khoản | Guest | Tạo User với email, mật khẩu, họ tên, điện thoại, CCCD bắt buộc; không email/OTP verification |
| UC02 | Đăng nhập/đăng xuất | User/Admin | Xác thực, kết thúc phiên |
| UC03 | Cập nhật hồ sơ cá nhân | User | Lưu họ tên/CCCD hợp lệ; hợp đồng hiển thị hồ sơ hiện tại, không đổi bên thuê/giá/điều khoản |
| UC04 | Xem danh sách phòng công khai | Guest/User | Chỉ hiển thị phòng AVAILABLE |
| UC05 | Tìm kiếm, lọc phòng | Guest/User | Lọc Room AVAILABLE theo tầng và diện tích m² khi phòng đã có dữ liệu diện tích |
| UC06 | Xem chi tiết phòng | Guest/User | Xem tầng, giá, sức chứa, mô tả, một ảnh đại diện và diện tích nếu đã có dữ liệu |
| UC07 | Thêm/chỉnh sửa phòng | Admin | Cập nhật dữ liệu phòng với tầng và giá niêm yết dương, diện tích dương nếu có; chỉ đổi giá khi không có Contract SCHEDULED/ACTIVE |
| UC08 | Quản lý trạng thái phòng | Admin | Chuyển trạng thái theo workflow hợp lệ |
| UC09 | Xem toàn bộ phòng quản trị | Admin | Xem AVAILABLE/RENTED/INACTIVE |
| UC10 | Gửi đơn đăng ký thuê | User | Tạo Application PENDING |
| UC11 | Xem/theo dõi đơn | User | Biết trạng thái đơn của mình |
| UC12 | Rút đơn PENDING | User | Application WITHDRAWN |
| UC13 | Xem danh sách đơn | Admin | Truy vấn và kiểm tra đơn |
| UC14 | Duyệt/từ chối đơn | Admin | APPROVED kèm Contract hợp lệ lấy giá Room lúc tạo, hoặc REJECTED |
| UC15 | Từ chối đơn cạnh tranh | System | Các PENDING cùng phòng REJECTED sau khi đã cho thuê |
| UC16 | Tạo hợp đồng từ đơn duyệt | Admin/System | Contract lấy giá niêm yết Room hiện hành, phòng được giữ; Admin không nhập giá khác |
| UC17 | Admin tạo hợp đồng trực tiếp | Admin | Contract không cần Application, lấy giá niêm yết Room hiện hành |
| UC18 | Xem hợp đồng | Admin/Tenant | Truy vấn danh sách, chi tiết trong phạm vi quyền |
| UC19 | Hủy hợp đồng SCHEDULED | Admin | CANCELLED, không thu tiền thuê, giải phóng phòng |
| UC20 | Tự kích hoạt hợp đồng | Scheduler | SCHEDULED → ACTIVE đúng ngày dù khách không đến nhận phòng |
| UC21 | Tự kết thúc hợp đồng đúng hạn | Scheduler | ACTIVE → ENDED, giải phóng Room; không gia hạn/sửa ngày kết thúc |
| UC22 | Đồng bộ trạng thái phòng theo hợp đồng | System | Room đúng với vòng đời Contract; giá chỉ sửa khi không còn SCHEDULED/ACTIVE |
| UC23 | Quản lý thông tin người ở cùng | Admin | Lưu danh sách hiện tại trong sức chứa, không trùng CCCD/cùng Contract, không sửa sau ENDED/CANCELLED |
| UC24 | Tạo/kiểm tra kỳ thu ngày 10 | Scheduler | BillingCycle đúng, không trùng |
| UC25 | Admin nhập tiền điện nước | Admin | Chốt đủ điện nước của kỳ |
| UC26 | Tính toán, phát hành Invoice | System | Invoice UNPAID với khoản phải thu đúng hoặc NO_CHARGE khi tổng 0 |
| UC27 | Người thuê xem Invoice | Tenant | Xem các Invoice của mình, cả chưa thanh toán và đã trả |
| UC28 | Thanh toán sandbox | Tenant | Khởi tạo thanh toán online hợp lệ |
| UC29 | Xác minh kết quả sandbox | Gateway/System | Xác minh kết quả, ghi PAID một lần; kết quả trễ/thất lạc/xung đột đưa Admin đối chiếu |
| UC30 | Admin xác nhận thanh toán thực tế | Admin | Xác nhận tiền ngoài hệ thống hoặc giao dịch SANDBOX đã xác minh qua M05; không ghi đè PAID |
| UC31 | Phát hiện hóa đơn quá hạn | Scheduler | UNPAID quá hạn → OVERDUE |
| UC32 | Admin tìm hóa đơn chưa thanh toán | Admin | Truy vấn UNPAID/OVERDUE |
| UC33 | Xem lịch sử hóa đơn đã thanh toán | Tenant/Admin | Truy vấn Invoice PAID, giới hạn quyền xem |
| UC34 | Kết thúc hợp đồng trước hạn | Admin | ACTIVE → ENDED với effectiveEndDate là hôm nay UTC+7; áp dụng ngoại lệ ngày 10 |
| UC35 | Quyết toán kỳ cuối | Admin/System | Trong kỳ: Invoice hoặc NO_CHARGE; ngày 10 sau kỳ đã ở: REGULAR xử lý, không thêm Invoice |
| UC36 | Tạo yêu cầu sửa chữa/phản ánh | Tenant | Issue OPEN của hợp đồng ACTIVE |
| UC37 | Theo dõi yêu cầu của mình | Tenant | Xem trạng thái và lịch sử Issue |
| UC38 | Xử lý yêu cầu | Admin | Cập nhật Issue đến RESOLVED kể cả khi Contract đã kết thúc |
| UC39 | Admin gửi thông báo thủ công | Admin | Notification tới người nhận |
| UC40 | Xem/đánh dấu đã đọc thông báo | Tenant/Admin | Notification UNREAD → READ |

## 10. Giao quyền sở hữu Use Case theo sáu gói công việc đã chốt

Đây là **bản phân công trách nhiệm nghiệp vụ**, không đồng nghĩa bắt buộc có đúng sáu package Java.

| Gói | Tên | Use Cases | Số lượng | Đối tượng chính |
|---|---|---|---:|---|
| M01 | Account & Issue | UC01–03, UC36–38 | 6 | User, Issue |
| M02 | Room Management | UC04–09, UC22 | 7 | Room |
| M03 | Rental Application | UC10–15 | 6 | RentalApplication |
| M04 | Contract Management | UC16–21, UC23, UC34 | 8 | Contract, người ở cùng |
| M05 | Billing & Online Payment | UC24–29, UC33 | 7 | BillingCycle, BillingCycleItem, Invoice, sandbox |
| M06 | Collection, Settlement & Notification | UC30–32, UC35, UC39–40 | 6 | Notification; điều phối công nợ và quyết toán |
| **Tổng** | | **UC01–UC40** | **40** | |

### 10.1. Ranh giới M05 – M06 đã chốt

- **M05 là nơi sở hữu Invoice Entity, trạng thái Invoice và mọi thao tác ghi Invoice.**
- M06 thực hiện UC30 (thu tiền trực tiếp), UC31 (quá hạn), UC32 (truy vấn công nợ), UC35 (quyết toán) thông qua giao diện mà M05 cung cấp. M06 không tự cập nhật InvoiceRepository.
- Dashboard Admin là phần tổng hợp trong phạm vi quản trị, không tạo UC thứ 41: M06 tích hợp màn hình; M02 cung cấp số Room theo status, M05 cung cấp tổng tiền Invoice PAID và số UNPAID/OVERDUE, M01 cung cấp số Issue chưa RESOLVED. Mỗi owner giữ quyền truy vấn dữ liệu của mình.
- M06 sở hữu Notification. M05 phát sự kiện khi phát hành/thanh toán hóa đơn; M06 tạo thông báo tương ứng.
- M06 tính khoản quyết toán theo lý do kết thúc trong UC35; M05 phát hành Invoice hoặc trả NO_CHARGE. M04 lưu kết quả quyết toán sớm trong Contract và phối hợp M02 kết thúc/giải phóng phòng.
- Phụ thuộc phát triển được giảm bằng **interface + DTO + mock**; phụ thuộc nghiệp vụ cứng khi chạy thật vẫn phải được kiểm thử đầy đủ.

### 10.2. Những workflow liên module phải có người điều phối

| Workflow | Các module | Điều kiện thành công chung |
|---|---|---|
| Duyệt đơn online | M03 + M04 + M02 | Application APPROVED chỉ khi Contract/Room cập nhật hợp lệ |
| Cho thuê trực tiếp | M04 + M02 + M03 | Có Contract, Room RENTED, đơn PENDING cạnh tranh REJECTED |
| Admin đưa phòng RENTED → INACTIVE | M02 + M04 + M06 + M05 | Hợp đồng kết thúc/hủy, quyết toán đúng, Room INACTIVE |
| Phát hành Invoice định kỳ | M05 + M04 + M02 | Có dữ liệu hợp đồng, phòng và điện nước đủ; Invoice không trùng |
| Thu tiền trực tiếp/quá hạn | M06 + M05 | Invoice được đọc và đổi trạng thái qua M05 |
| Thanh toán online | M05 + Gateway | Xác minh kết quả, ghi nhận PAID an toàn |
| Thông báo phát sinh | Module nguồn + M06 | Notification được tạo sau sự kiện nghiệp vụ thành công |

## 11. Tiêu chí nghiệm thu nghiệp vụ (Business Acceptance)

Một bản demo đạt yêu cầu tối thiểu cần chứng minh:

1. Guest chỉ nhìn thấy Room AVAILABLE; các Room RENTED/INACTIVE không nhận đăng ký mới.
2. User tự đăng ký, gửi đơn, rút đơn PENDING và theo dõi trạng thái.
3. Duyệt đơn tạo được Contract và giữ phòng; không duyệt giả khi tạo Contract lỗi.
4. Admin cho thuê trực tiếp và tự từ chối các đơn cạnh tranh.
5. Contract SCHEDULED → ACTIVE đúng ngày dù khách chưa đến; ACTIVE → ENDED đúng hạn; Room được đồng bộ; không gia hạn/sửa plannedEndDate.
6. Người ở cùng được Admin quản lý, không vượt sức chứa, không trùng CCCD trong Contract, không sửa sau ENDED/CANCELLED và không có quyền thanh toán.
7. Kỳ thu ngày 10 yêu cầu nhập đủ tiền điện nước; hóa đơn kỳ đầu và kỳ thường tính đúng.
8. Cùng một kỳ không thể phát hành hóa đơn trùng vì Scheduler chạy lại.
9. Tenant xem đúng hóa đơn của mình, thanh toán sandbox thành công/thất bại; chỉ một lần ghi nhận PAID.
10. Hóa đơn quá 48 giờ chuyển OVERDUE; Admin tìm và xác nhận thanh toán ngoài hệ thống.
11. Tenant và Admin xem được lịch sử Invoice PAID; kể cả hóa đơn của hợp đồng đã kết thúc.
12. Trả phòng sớm trong kỳ do Tenant thu nguyên kỳ; đúng ngày 10 sau kỳ đã ở thì REGULAR xử lý kỳ cũ, không có tiền kỳ mới; Admin ngừng cung cấp thì tính theo ngày thực tế.
13. Không tạo trùng kỳ cuối sau khi đã có hóa đơn quyết toán; nợ chưa trả vẫn được theo dõi.
14. Admin đưa phòng đang thuê về INACTIVE thông qua kết thúc/hủy hợp đồng hợp lệ; Admin phải tự mở lại phòng.
15. Tenant chỉ gửi Issue khi hợp đồng ACTIVE; Admin xử lý Issue đang mở tiếp sau khi Contract kết thúc, Tenant xem được lịch sử.
16. Admin gửi được thông báo thủ công; hệ thống sinh thông báo theo các sự kiện đã triển khai.
17. Email đăng nhập duy nhất; đăng ký bắt buộc CCCD và không email/OTP verification; hồ sơ User cập nhật hiển thị trên Contract lịch sử; không đăng ký Admin công khai.
18. Chặn đơn PENDING trùng; xử lý đơn quá ngày mà không tự thay ngày.
19. Tiền đồng nguyên, HALF_UP ở kết quả cuối; dueAt cố định kể cả Notification lỗi.
20. Khoản tổng 0 được ghi nhận NO_CHARGE, chạy lại không tạo nghĩa vụ mới.
21. Không sửa tiền sau phát hành; callback muộn/xung đột được đối chiếu, không ghi đè PAID.
22. Room có tầng dương bắt buộc, diện tích dương nếu được nhập và một ảnh đại diện; giá niêm yết/giá Contract dương, đổi giá bị chặn khi SCHEDULED/ACTIVE; Contract dùng giá Room tại lúc tạo.
23. Dashboard Admin tổng hợp đúng bốn nhóm chỉ số từ dữ liệu hiện có, không ghi thêm Entity thống kê.

## 12. Quyết định kỹ thuật đã thống nhất và việc còn phải đặc tả

### 12.1. Đã thống nhất cho bản 1.0

1. Ngày dùng khoảng [from, to); ngày nghiệp vụ UTC+7; kỳ mới bắt đầu 00:00 ngày 10; timestamp lưu UTC.
2. Tiền DECIMAL(19,0)/BigDecimal; HALF_UP trên khoản tiền thuê cuối, giữ chính xác trung gian, không dùng float/double.
3. dueAt = issuedAt + 48 giờ, không phụ thuộc Notification.
4. Mô hình 10 bảng trong database_design.md; không Payment, không bảng người nhận riêng, không outbox, không Entity Dashboard.
5. Một kỳ theo collectionDate; một Invoice theo Contract + mốc ngày 10; một PENDING/User–Room; một SCHEDULED/ACTIVE/Room.
6. Điện nước lưu trước ở BillingCycleItem; ghi cả kết quả NO_CHARGE để chạy lại an toàn.
7. Trạng thái Room, Application, Contract, Invoice độc lập. Invoice không lưu DRAFT; REGULAR/EARLY_SETTLEMENT là loại, không phải trạng thái thanh toán.
8. Notification một dòng/người nhận, sau commit, retry có giới hạn và chống trùng; chấp nhận giới hạn phục hồi khi tiến trình dừng.
9. CCCD bắt buộc khi đăng ký và seed Admin; Room có tầng dương bắt buộc, diện tích dương nếu được nhập và một ảnh đại diện; giá niêm yết/giá Contract dương, giá Contract sao từ Room khi tạo và bất biến; người ở cùng không trùng CCCD trong cùng Contract.
10. Tenant trả sớm đúng ngày 10 sau kỳ đã sử dụng không phát sinh tiền kỳ mới; Invoice REGULAR xử lý kỳ cũ. Không gia hạn Contract; Issue đã tạo không tự đóng khi Contract kết thúc.
11. Email đăng nhập phân biệt chữ hoa/thường trên toàn địa chỉ; bỏ khoảng trắng đầu/cuối nhưng không chuyển chữ thường. Không áp thêm thời hạn thuê tối thiểu ngoài `planned_end_date > start_date`.
12. Foundation giữ `created_at`/`updated_at` trên cả 10 bảng và thời điểm UTC; quản lý schema bằng một SQL script chạy thủ công một lần, kiểm tra JPA mapping bằng `validate`, seed một ADMIN bằng script riêng. Không yêu cầu Flyway hoặc hệ thống audit lịch sử.

### 12.2. Còn phải đặc tả triển khai, không tự đổi nghiệp vụ

- Chọn gateway, xác minh chữ ký/tham chiếu, phản hồi callback và lệnh Admin đối chiếu; giữ đúng nguồn tiền.
- Chốt DTO, hành vi lỗi, coordinator, thứ tự khóa và quyền server cho các workflow; mọi ghi Invoice qua M05.
- Chốt cấu hình retry/log, định dạng giấy tờ, lịch chạy và cách mapping timestamp UTC; khi triển khai chọn collation SQL Server phân biệt hoa/thường phù hợp với quy tắc email đã chốt.
- Kiểm thử Scheduler ngày 10: Contract đã ENDED vẫn được truy ra nghĩa vụ REGULAR; khi Tenant trả sớm đúng biên không tạo EARLY_SETTLEMENT, điện nước kỳ cũ được chốt đúng một lần.
- Đặc tả cách lấy thông tin giao dịch sandbox đã xác minh cho Admin đối chiếu; không tạo lịch sử từng lần thử hoặc ghi đè Invoice PAID.
- Xác nhận kết nối tới SQL Server dùng cho kiểm thử Foundation; đối chiếu schema, Entity, constraint và seed bằng dữ liệu mẫu trước khi bàn giao cho sáu module.
- Cơ chế lưu kết quả NO_CHARGE và các constraint cụ thể nằm trong database_design.md; thay đổi phải đồng bộ trước khi nhiều người triển khai.

### Những điều KHÔNG nên tự thay đổi khi giải quyết kỹ thuật

- Không đổi ngày thu tiền từ 10 sang ngày khác.
- Không chuyển sang thu tiền trả trước.
- Không tự thêm deposit, trả góp, Payment entity, hoặc theo dõi tất cả lần thử thanh toán.
- Không cho Tenant thanh toán hóa đơn OVERDUE bằng sandbox nếu chưa có quyết định nghiệp vụ mới.
- Không cho Admin đổi Room RENTED sang INACTIVE mà không xử lý Contract và quyết toán.
- Không tự cho phép backdate ngày bắt đầu hợp đồng vì nhập thông tin muộn.

---

## 13. Tóm tắt bàn giao cho thành viên

**Khi nhận một Use Case, mỗi thành viên cần trả lời được năm câu hỏi:**

1. Actor nào khởi tạo UC này và có quyền không?
2. Điều kiện trước khi thao tác là gì (status, ngày tháng, quan hệ phòng–hợp đồng–người thuê)?
3. UC thay đổi đối tượng nào và module nào sở hữu đối tượng đó?
4. Nếu bước giữa thất bại, trạng thái hợp lệ cuối cùng phải là gì?
5. UC phát sinh Invoice, công nợ, thông báo hoặc thay đổi phòng/hợp đồng nào ở module khác?

**Nguyên tắc chung:** Đúng nghiệp vụ và dữ liệu nhất quán quan trọng hơn việc mỗi module có thể tự sửa tất cả các bảng liên quan. Có thể dùng mock khi phát triển; trước khi nghiệm thu phải chạy được workflow liên module bằng service và database thật.

**Tài liệu đi kèm:** [database_design.md](database_design.md), [MODULE_HANDOFF_AND_IMPLEMENTATION_V2.md](MODULE_HANDOFF_AND_IMPLEMENTATION_V2.md). Module Interface Contracts (DTO, service, behavior, transaction) được đặc tả trước khi tách nhánh triển khai. Các tài liệu bổ sung, không thay thế nguồn nghiệp vụ này.
