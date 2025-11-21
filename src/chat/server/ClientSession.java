package chat.server;

import chat.common.ChatMsg;
import java.io.*;
import java.net.Socket;

public class ClientSession extends Thread {
    private final Socket socket;
    private final ChatServer server;
    private final ChatRoomManager roomManager;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    String uid;          // 로그인한 사용자 ID
    ChatRoom currentRoom; // 현재 참여중인 방

    public ClientSession(Socket socket, ChatServer server, ChatRoomManager roomManager) {
        this.socket = socket;
        this.server = server;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            // ObjectOutputStream 먼저 생성 + flush → ObjectInputStream
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

            // 최초 기본방 설정 (lobby)
            currentRoom = roomManager.getRoom("lobby");

            ChatMsg msg;
            while ((msg = (ChatMsg) in.readObject()) != null) {
                handleMessage(msg);
            }

        } catch (EOFException e) {
            server.log(uid + " 연결 종료 (EOF)");
        } catch (IOException e) {
            server.log("서버 읽기 오류> " + e.getMessage());
        } catch (ClassNotFoundException e) {
            server.log("수신 객체 클래스 오류> " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleMessage(ChatMsg msg) {
        // 여기서 mode별로 분기 처리
        switch (msg.mode) {
            case ChatMsg.MODE_LOGIN:
                // uid 설정, 방 입장 처리 등
                break;

            case ChatMsg.MODE_LOGOUT:
                // 로그아웃 처리
                break;

            case ChatMsg.MODE_TX_STRING:
                // 문자열 메시지 방에 브로드캐스트
                break;

            case ChatMsg.MODE_TX_IMAGE:
                // 이미지 메시지 방에 브로드캐스트
                break;

            // TODO: MODE_JOIN_ROOM, MODE_LEAVE_ROOM 등 확장 가능
        }
    }

    public void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            server.log("클라이언트 전송 오류> " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (currentRoom != null) {
                currentRoom.leave(this);
            }
            server.removeClient(this);
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            server.log("클라이언트 세션 종료 오류> " + e.getMessage());
        }
    }
}
