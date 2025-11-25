import chat.shared.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {

    // 서버로부터 들어오는 Message를 받고 싶은 쪽에서 구현
    public interface Listener {
        void onMessage(Message m);
    }

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String nickname;

    private Thread listenerThread;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void connect(String host, int port, String nickname) throws Exception {
        this.nickname = nickname;

        socket = new Socket(host, port);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        send(Message.login(nickname));

        listenerThread = new Thread(this::listenLoop, "client-listener");
        listenerThread.start();
    }

    private void listenLoop() {
        try {
            while (!socket.isClosed()) {
                Object obj = in.readObject();
                if (!(obj instanceof Message m)) continue;

                for (Listener l : listeners) {
                    l.onMessage(m);
                }
            }
        } catch (Exception e) {
            System.out.println("[ChatClient] listen 종료: " + e.getMessage());
        }
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public String getNickname() {
        return nickname;
    }

    public void send(Message m) throws Exception {
        if (out == null) {
            throw new IllegalStateException("서버와 연결되지 않았습니다.");
        }
        out.writeObject(m);
        out.flush();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
    }
}
