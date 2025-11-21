package chat.server;

import chat.common.ChatMsg;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {
    private final String roomId;
    private final String roomName;
    private final List<ClientSession> participants = new CopyOnWriteArrayList<>();

    public ChatRoom(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void join(ClientSession session) {
        // 참가자 리스트에 추가
        participants.add(session);
    }

    public void leave(ClientSession session) {
        // 참가자 리스트에서 제거
        participants.remove(session);
    }

    public void broadcast(ChatMsg msg) {
        // 방 안의 모든 클라이언트에게 메시지 전송
        for (ClientSession s : participants) {
            s.send(msg);
        }
    }
}

