package chat.client;

import chat.common.ChatMsg;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class ChatClient {
    private final String serverAddress;
    private final int serverPort;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;

    // 수신 메시지 처리용 콜백 (UI로 넘김)
    private final Consumer<ChatMsg> messageHandler;
    // 로그 출력용
    private final Consumer<String> logHandler;

    public ChatClient(String serverAddress, int serverPort,
                      Consumer<ChatMsg> messageHandler,
                      Consumer<String> logHandler) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.messageHandler = messageHandler;
        this.logHandler = logHandler != null ? logHandler : System.out::println;
    }

    public void connect() throws IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        log("서버에 접속했습니다.");

        receiveThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ChatMsg msg = (ChatMsg) in.readObject();
                    if (msg == null) break;
                    if (messageHandler != null) messageHandler.accept(msg);
                }
            } catch (EOFException e) {
                log("서버에서 연결 종료");
            } catch (IOException | ClassNotFoundException e) {
                log("수신 오류: " + e.getMessage());
            } finally {
                disconnect();
            }
        });

        receiveThread.start();
    }

    public void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            log("전송 오류: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (receiveThread != null) receiveThread.interrupt();
            if (socket != null && !socket.isClosed()) socket.close();
            log("연결을 종료했습니다.");
        } catch (IOException e) {
            log("연결 종료 오류: " + e.getMessage());
        }
    }



    private void log(String msg) {
        if (logHandler != null) logHandler.accept(msg);
    }
}

