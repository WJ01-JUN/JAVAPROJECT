package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;
import java.util.function.Consumer;

public class ChatServer {
    private final int port;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final Vector<ClientSession> clients = new Vector<>();
    private final ChatRoomManager roomManager = new ChatRoomManager();

    // 로그를 어디로 보낼지 (콘솔 or 서버 GUI)
    private final Consumer<String> logger;

    public ChatServer(int port, Consumer<String> logger) {
        this.port = port;
        this.logger = logger != null ? logger : System.out::println;
    }

    public void start() {
        if (acceptThread != null && acceptThread.isAlive()) return;

        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("서버가 시작되었습니다. (포트: " + port + ")");

                while (acceptThread == Thread.currentThread()) {
                    Socket clientSocket = serverSocket.accept();
                    log("클라이언트 연결: " + clientSocket.getRemoteSocketAddress());

                    ClientSession session = new ClientSession(clientSocket, this, roomManager);
                    clients.add(session);
                    session.start();
                }
            } catch (SocketException e) {
                log("서버 소켓 종료: " + e.getMessage());
            } catch (IOException e) {
                log("서버 오류: " + e.getMessage());
            } finally {
                closeServer();
            }
        });

        acceptThread.start();
    }

    public void stop() {
        try {
            acceptThread = null;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            log("서버를 종료했습니다.");
        } catch (IOException e) {
            log("서버소켓 닫기 오류> " + e.getMessage());
        }
    }

    void removeClient(ClientSession session) {
        clients.remove(session);
        log("클라이언트 퇴장, 현재 참가자 수: " + clients.size());
    }

    void log(String msg) {
        logger.accept(msg);
    }

    private void closeServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("서버 닫기 오류> " + e.getMessage());
        }
    }

    // 서버만 단독 실행 시
    public static void main(String[] args) {
        ChatServer server = new ChatServer(54321, System.out::println);
        server.start();
    }
}
