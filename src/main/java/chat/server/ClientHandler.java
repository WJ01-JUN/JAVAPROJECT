package chat.server;

import chat.shared.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final ChatServer server;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String nickname;
    private ChatRoom currentRoom;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void run() {
        try {
            // ⚠️ ObjectOutputStream 먼저 만들고 flush, 그 다음 InputStream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 1) 첫 메시지는 LOGIN 이어야 한다고 가정
            Object obj = in.readObject();
            if (!(obj instanceof Message first) ||
                    first.getType() != Message.Type.LOGIN ||
                    first.getSender() == null ||
                    first.getSender().isBlank()) {

                send(Message.error("첫 메시지는 LOGIN 이어야 합니다."));
                close();
                return;
            }

            this.nickname = first.getSender().trim();
            server.addClient(this);
            send(Message.system("환영합니다, " + nickname + "님!"));

            // 2) 메시지 처리 루프
            while (true) {
                Object o = in.readObject();
                if (!(o instanceof Message msg)) {
                    continue;
                }
                handleMessage(msg);
            }

        } catch (Exception e) {
            System.out.println("[ClientHandler] 종료(" + nickname + "): " + e.getMessage());
        } finally {
            // 정리
            if (currentRoom != null) {
                currentRoom.leave(this);
                server.removeEmptyRoom(currentRoom.getName());
            }
            server.removeClient(this);
            close();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case ROOM_LIST:
                // 클라이언트가 방 목록을 요청했을 때
                server.sendRoomListTo(this);
                break;

            case CREATE_ROOM:
                handleCreateRoom(msg);
                break;

            case JOIN_ROOM:
                handleJoinRoom(msg);
                break;

            case CHAT:
                handleChat(msg);
                break;

            case GAME_EVENT:
                // 나중에 미니게임 이벤트 처리
                break;

            default:
                send(Message.error("지원하지 않는 타입: " + msg.getType()));
        }
    }

    private void handleCreateRoom(Message msg) {
        String roomName = msg.getRoom();
        if (roomName == null || roomName.isBlank()) {
            send(Message.error("방 이름을 입력하세요."));
            return;
        }

        ChatRoom room = server.getOrCreateRoom(roomName.trim());
        System.out.println("[Server] 방 생성/존재 확인: " + room.getName());

        // 방 목록이 바뀌었으니 전체에게 ROOM_LIST 푸시
        server.broadcastRoomListToAll();
    }

    private void handleJoinRoom(Message msg) {
        String roomName = msg.getRoom();
        if (roomName == null || roomName.isBlank()) {
            send(Message.error("방 이름이 비어 있습니다."));
            return;
        }

        ChatRoom room = server.getOrCreateRoom(roomName.trim());

        if (currentRoom != null) {
            currentRoom.leave(this);
            server.removeEmptyRoom(currentRoom.getName());
        }

        currentRoom = room;
        currentRoom.join(this);
        // join() 안에서 입장 시스템 + 유저 목록 브로드캐스트까지 처리
    }

    private void handleChat(Message msg) {
        if (currentRoom == null) {
            send(Message.error("먼저 방에 입장해 주세요."));
            return;
        }
        String text = msg.getText();
        if (text == null || text.isBlank()) return;

        // 채팅은 히스토리에 저장
        currentRoom.broadcast(Message.chat(
                currentRoom.getName(),
                nickname,
                text
        ), true);
    }

    public void send(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("[ClientHandler] send 실패(" + nickname + "): " + e.getMessage());
        }
    }

    private void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
