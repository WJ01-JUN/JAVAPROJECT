package chat.server;

import chat.shared.Message;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ChatRoom {

    private final String name;
    private final Set<ClientHandler> participants = ConcurrentHashMap.newKeySet();

    // ✅ 서버 살아있는 동안 방별 채팅 내용 유지
    private final List<Message> history = new CopyOnWriteArrayList<>();

    public ChatRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // 새로 들어온 클라이언트에게 과거 기록 전송
    public void sendHistoryTo(ClientHandler client) {
        for (Message m : history) {
            client.send(m);
        }
    }

    public void join(ClientHandler client) {
        participants.add(client);

        // 1) 과거 히스토리 먼저 보내고
        sendHistoryTo(client);

        // 2) 입장 시스템 메시지: 히스토리에도 저장
        broadcast(Message.systemForRoom(
                name,
                client.getNickname() + "님이 입장했습니다."
        ), true);

        // 3) 유저 목록 갱신
        broadcastUserList();

        System.out.println("[Room:" + name + "] join: " + client.getNickname());
    }

    public void leave(ClientHandler client) {
        if (participants.remove(client)) {
            // ✅ 퇴장 메시지는 히스토리에는 안 남기고 현재 인원에게만 보냄
            broadcast(Message.systemForRoom(
                    name,
                    client.getNickname() + "님이 퇴장했습니다."
            ), false);

            // ✅ 유저 목록 갱신
            broadcastUserList();

            System.out.println("[Room:" + name + "] leave: " + client.getNickname());
        }
    }

    // saveHistory = true일 때만 history에 저장
    public void broadcast(Message msg, boolean saveHistory) {
        if (saveHistory) {
            history.add(msg);
        }
        for (ClientHandler ch : participants) {
            ch.send(msg);
        }
    }

    // 기존 코드 호환용
    public void broadcast(Message msg) {
        broadcast(msg, false);
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    // ✅ 참여자 목록 브로드캐스트
    public void broadcastUserList() {
        List<String> users = participants.stream()
                .map(ClientHandler::getNickname)
                .sorted()
                .collect(Collectors.toList());

        Message m = Message.userList(name, users);
        for (ClientHandler ch : participants) {
            ch.send(m);
        }
    }
}
