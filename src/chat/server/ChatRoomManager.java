package chat.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomManager {
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    public ChatRoomManager() {
        // 기본 방 하나(로비) 만들어두기
        ChatRoom lobby = new ChatRoom("lobby", "로비");
        rooms.put("lobby", lobby);
    }

    public ChatRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public ChatRoom getOrCreateRoom(String roomId, String roomName) {
        // 방이 없으면 새로 만들고, 있으면 기존거 반환
        return rooms.computeIfAbsent(roomId, id -> new ChatRoom(id, roomName));
    }

    public Map<String, ChatRoom> getRooms() {
        return rooms;
    }
}

