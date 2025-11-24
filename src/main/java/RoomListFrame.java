import chat.shared.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class RoomListFrame extends JFrame {

    private final ChatClient client;
    private final String host;
    private final int port;

    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private JLabel profileLabel;

    public RoomListFrame(ChatClient client, String host, int port) {
        this.client = client;
        this.host = host;
        this.port = port;

        setTitle("Talk & Play - 방 목록");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();

        // 서버 메시지 수신 리스너 등록
        client.addListener(this::handleServerMessage);

        // 접속하자마자 한 번 방 목록 요청
        requestRoomList();
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        main.setBackground(new Color(240, 253, 244));

        // 상단 프로필
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        profileLabel = new JLabel();
        profileLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        profileLabel.setForeground(new Color(22, 30, 46));

        String profileText = String.format(
                "닉네임: %s   |   서버: %s:%d",
                client.getNickname(), host, port
        );
        profileLabel.setText(profileText);

        JButton refreshBtn = new JButton("새로고침");
        refreshBtn.addActionListener(e -> requestRoomList());

        top.add(profileLabel, BorderLayout.WEST);
        top.add(refreshBtn, BorderLayout.EAST);

        main.add(top, BorderLayout.NORTH);

        // 중앙 방 목록
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(roomList);
        main.add(scroll, BorderLayout.CENTER);

        // 하단 버튼 영역
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton createBtn = new JButton("방 만들기");
        createBtn.addActionListener(e -> createRoom());

        JButton joinBtn = new JButton("입장하기");
        joinBtn.addActionListener(e -> joinSelectedRoom());

        bottom.add(createBtn);
        bottom.add(joinBtn);

        main.add(bottom, BorderLayout.SOUTH);

        setContentPane(main);
    }

    // ===== 서버 메시지 처리 =====

    private void handleServerMessage(Message m) {
        // 방 목록 관련 메시지만 처리하고, SYSTEM/ERROR 등은 여기서 무시
        if (m.getType() == Message.Type.ROOM_LIST) {
            SwingUtilities.invokeLater(() -> updateRoomList(m));
        }
    }

    private void updateRoomList(Message m) {
        roomListModel.clear();
        if (m.getRooms() != null) {
            for (String r : m.getRooms()) {
                roomListModel.addElement(r);
            }
        }
    }

    // ===== 서버로 요청 보내기 =====

    private void requestRoomList() {
        try {
            client.send(new Message(Message.Type.ROOM_LIST));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "방 목록 요청 실패: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createRoom() {
        String name = JOptionPane.showInputDialog(
                this, "방 이름을 입력하세요:", "방 만들기",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "방 이름을 입력해주세요.", "입력 오류",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            client.send(Message.createRoom(name));
            // 서버가 방 생성 후 broadcastRoomListToAll() 호출 → 자동으로 ROOM_LIST 푸시됨
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "방 생성 실패: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinSelectedRoom() {
        String selected = roomList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "입장할 방을 선택해주세요.",
                    "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // ✅ 먼저 ChatFrame을 만들고 리스너 등록
            ChatFrame chat = new ChatFrame(client, selected);
            chat.setVisible(true);

            // ✅ 그 다음에 JOIN 메시지를 보내야
            // 서버가 보내는 히스토리/입장 시스템 메시지를 ChatFrame이 받을 수 있음
            client.send(Message.joinRoom(selected));

            // (방 목록창을 계속 보여줄지 말지는 취향대로)
            // setVisible(false);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "방 입장 실패: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }


    @Override
    public void dispose() {
        client.removeListener(this::handleServerMessage);
        super.dispose();
    }
}
