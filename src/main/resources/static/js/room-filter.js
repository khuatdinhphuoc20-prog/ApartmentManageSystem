const search = document.getElementById('room-code');
const rooms = Array.from(document.querySelectorAll('#room-list [data-room-code]'));
const result = document.getElementById('room-result');

document.getElementById('room-search').hidden = false;
search.addEventListener('input', () => {
    const query = search.value.trim().toLocaleLowerCase('vi');
    let matches = 0;
    for (const room of rooms) {
        room.hidden = !room.dataset.roomCode.toLocaleLowerCase('vi').includes(query);
        if (!room.hidden) matches++;
    }
    result.textContent = matches ? `Hiển thị ${matches} phòng.` : 'Không tìm thấy phòng có mã phù hợp.';
});
