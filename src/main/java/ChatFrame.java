import chat.shared.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;

public class ChatFrame extends JFrame {

    private final ChatClient client;
    private final String roomName;

    private final ChatClient.Listener listener;

    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;

    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    public ChatFrame(ChatClient client, String roomName) {
        this.client = client;
        this.roomName = roomName;

        setTitle("채팅방 - " + roomName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        initUI();

        // 서버 메시지 리스너 등록
        listener = this::handleServerMessage;
        client.addListener(listener);
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ===== 상단 타이틀 =====
        JLabel title = new JLabel("방 이름: " + roomName);
        title.setFont(new Font("Dialog", Font.BOLD, 16));
        main.add(title, BorderLayout.NORTH);

        // ===== 중앙: 채팅창 (왼쪽) + 유저 목록(오른쪽) =====
        JPanel center = new JPanel(new BorderLayout(8, 8));

        // 채팅창: JTextPane + StyledDocument
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.WHITE);

        JScrollPane chatScroll = new JScrollPane(chatPane);
        center.add(chatScroll, BorderLayout.CENTER);

        // 유저 목록 패널 (오른쪽)
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(180, 0));
        userPanel.setBorder(BorderFactory.createTitledBorder("참여자"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        center.add(userPanel, BorderLayout.EAST);

        main.add(center, BorderLayout.CENTER);

        // ===== 하단: 입력창 =====
        JPanel bottom = new JPanel(new BorderLayout(5, 0));

        inputField = new JTextField();
        sendButton = new JButton("전송");

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        main.add(bottom, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendCurrentMessage());
        sendButton.addActionListener(e -> sendCurrentMessage());

        setContentPane(main);
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        try {
            client.send(Message.chat(roomName, client.getNickname(), text));
            inputField.setText("");
        } catch (Exception e) {
            appendSystem("[오류] 메시지 전송 실패: " + e.getMessage());
        }
    }

    // ===== 서버에서 오는 메시지 처리 =====
    private void handleServerMessage(Message m) {
        switch (m.getType()) {
            case CHAT -> {
                if (roomName.equals(m.getRoom())) {
                    appendChat(m.getSender() + ": " + m.getText());
                }
            }
            case SYSTEM -> {
                // room이 null이면 전체용 시스템 메시지, 아니면 방용
                String room = m.getRoom();
                if (room == null || roomName.equals(room)) {
                    appendSystem(m.getText());
                }
            }
            case ERROR -> {
                appendSystem("[오류] " + m.getText());
            }
            case USER_LIST -> {
                if (roomName.equals(m.getRoom())) {
                    updateUserList(m);
                }
            }
            default -> {
                // GAME_EVENT 등은 나중에 여기서 처리
            }
        }
    }

    private void updateUserList(Message m) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (m.getUsers() != null) {
                for (String u : m.getUsers()) {
                    userListModel.addElement(u);
                }
            }
        });
    }

    // ===== JTextPane에 줄 추가 (왼쪽/가운데 정렬) =====

    private void appendChat(String text) {
        appendLine(text, StyleConstants.ALIGN_LEFT, Color.BLACK, false);
    }

    private void appendSystem(String text) {
        // ✅ 가운데 정렬, 회색, 이탤릭 느낌
        appendLine("[System] " + text, StyleConstants.ALIGN_CENTER, Color.GRAY, true);
    }

    private void appendLine(String text, int align, Color color, boolean italic) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();

            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setAlignment(attrs, align);
            StyleConstants.setForeground(attrs, color);
            StyleConstants.setBold(attrs, !italic);
            StyleConstants.setItalic(attrs, italic);

            try {
                int start = doc.getLength();
                doc.insertString(start, text + "\n", attrs);
                // 문단 정렬 적용
                doc.setParagraphAttributes(start, (text + "\n").length(), attrs, false);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            chatPane.setCaretPosition(doc.getLength());
        });
    }

    @Override
    public void dispose() {
        client.removeListener(listener);
        super.dispose();
    }
}
