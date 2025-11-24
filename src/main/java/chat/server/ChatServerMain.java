package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServerMain {

    public static void main(String[] args) {
        int port = 6000; // 네가 쓰고 있는 포트에 맞춰서 변경 (예: 5000, 6000)
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("포트 파싱 실패, 기본 포트 " + port + " 사용");
            }
        }

        ChatServer server = new ChatServer();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] 채팅 서버 시작: port=" + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] 새 클라이언트 접속: " + clientSocket);

                ClientHandler handler = new ClientHandler(clientSocket, server);
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("[Server] 서버 소켓 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
