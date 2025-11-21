package chat.client;

import chat.common.ChatMsg;
import chat.client.ChatClient;
import chat.server.ClientSession;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientFrame extends JFrame {
    private JTextPane t_display;
    private JTextField t_input, t_userID, t_userAddress,t_userPort;
    private JButton b_send, b_connect, b_disconnect, b_select;
    private DefaultStyledDocument document;
    private String serverAddress, userID;
    private int serverPort;

    private ChatClient chatClient;

    public ClientFrame(String serverAddress, int serverPort) {
        super("Multi Talk");

        buildGUI();

        // ChatClient 생성 (수신 메시지를 onMessageReceived로 넘김)
        chatClient = new ChatClient(
                serverAddress,
                serverPort,
                this::onMessageReceived,
                this::printDisplay
        );

        setBounds(100, 200, 500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        add(connectPanel(), BorderLayout.SOUTH);
        add(createDisplayPanel(), BorderLayout.CENTER);
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);
        p.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return p;
    }

    private JPanel createInfoPanel() {
        // 아이디, 서버주소, 포트 입력/표시 패널 (네 코드 그대로 옮기면 됨)
        JLabel l_ID, l_Address, l_Port;
        l_ID= new JLabel("아이디: ");
        l_Address = new JLabel ("서버주소: ");
        l_Port = new JLabel("포트번호: ");

        t_userID = new JTextField(5);
        t_userAddress = new JTextField(10);
        t_userPort = new JTextField(5);



        InetAddress local = null;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String addr = local.getHostAddress();
        String[] part = addr.split("\\.");
        t_userID.setText("guest" + part[3]);
        t_userAddress.setText(serverAddress);
        t_userPort.setText(Integer.toString(serverPort));

        JPanel u_panel = new JPanel ();
        u_panel.add(l_ID);
        u_panel.add(t_userID);

        u_panel.add(l_Address);
        u_panel.add(t_userAddress);

        u_panel.add(l_Port);
        u_panel.add(t_userPort);

        u_panel.setBackground(Color.LIGHT_GRAY);

        return u_panel;
    }

    private JPanel createInputPanel() {
        // t_input, 보내기 버튼, 이미지 선택 버튼 구현
        t_input = new JTextField(20);
        JButton b_send = new JButton("보내기");
        JPanel t_panel =new JPanel (new BorderLayout());

        t_input.addActionListener(e -> {
            String text = t_input.getText();
            sendMessage();
            //   receiveMessage();
            t_input.setText("");
        });

        b_send.addActionListener(e->{
            sendMessage();
            //	receiveMessage();
        });

        b_select = new JButton("선택하기");
        b_select.addActionListener(new ActionListener() {

            JFileChooser chooser = new JFileChooser();
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images",
                        "jpg", "gif", "png");
                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(ClientFrame.this);
                if(ret != JFileChooser.APPROVE_OPTION){
                    JOptionPane.showMessageDialog(ClientFrame.this, "파일을 선택하지 않음","경고",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        t_panel.add(t_input,BorderLayout.CENTER);
        JPanel p_button = new JPanel(new GridLayout(1,0));
        p_button.add(b_select);
        p_button.add(b_send);
        t_panel.add(p_button, BorderLayout.EAST);

        return t_panel;
    }

    private JPanel createControlPanel() {
        // 접속하기, 끊기, 종료 버튼
        JButton b_connect = new JButton("접속하기");
        JButton b_disconnect = new JButton("접속끊기");
        JButton b_exit = new JButton("종료하기");

        b_connect.addActionListener(new ClientFrame.conncetAction());
        b_disconnect.addActionListener(new ClientFrame.disconnectAction());

        JPanel b_panel = new JPanel (new GridLayout());
        b_panel.add(b_connect);
        b_panel.add(b_disconnect);
        b_panel.add(b_exit);
        return b_panel;
    }

    private JPanel connectPanel() {
        JPanel cn_panel = new JPanel(new GridLayout(3, 1));
        cn_panel.add(createInputPanel());
        cn_panel.add(createInfoPanel());
        cn_panel.add(createControlPanel());
        return cn_panel;
    }

    // === 네트워크 관련 동작 ===

    private void onConnect() {
        try {
            chatClient.connect();
            sendUserID();
        } catch (Exception e) {
            printDisplay("서버 접속 오류: " + e.getMessage());
        }
    }

    private void sendUserID() {
        userID = t_userID.getText();
        chatClient.send(new ChatMsg(userID, ChatMsg.MODE_LOGIN));
    }

    private void onDisconnect() {
        chatClient.disconnect();
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (message.isEmpty()) return;
        chatClient.send(new ChatMsg(userID, ChatMsg.MODE_TX_STRING, message));
        t_input.setText("");
    }

    private void sendImage() {
        // 파일에서 ImageIcon 만들고 ChatMsg.MODE_TX_IMAGE로 보내기
        String filename = t_input.getText().strip();
        if(filename.isEmpty()) return;

        File file = new File(filename);
        if(!file.exists()){
            printDisplay(">>파일이 존재하지 않습니다." + filename);
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        chatClient.send(new ChatMsg(userID, ChatMsg.MODE_TX_IMAGE,file.getName(),icon));

        t_input.setText("");
    }

    // === 수신 메시지 처리 ===
    private void onMessageReceived(ChatMsg msg) {
        switch (msg.mode) {
            case ChatMsg.MODE_TX_STRING:
                printDisplay(msg.userID + " : " + msg.message);
                break;
            case ChatMsg.MODE_TX_IMAGE:
                printDisplay(msg.userID + " : " + msg.message);
                printDisplay(msg.image);
                break;
        }
    }

    // === 출력 도우미들 ===

    private void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private void printDisplay(ImageIcon icon) {
        // 이미지 크기 조절 + t_display에 아이콘 삽입
        t_display.setCaretPosition(t_display.getDocument().getLength());

        if(icon.getIconWidth() > 400){
            Image img = icon.getImage();
            Image changeImg = img. getScaledInstance(400,-1,Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }

        t_display.insertIcon(icon);

        printDisplay("");
        t_input.setText("");
    }

    class conncetAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습

            try{
                chatClient.connect();
                sendUserID();
            }catch(UnknownHostException ex){
                printDisplay("서버의 주소를 찾을 수 없습니다: "+ ex.getMessage());
            }catch(IOException ex){
                printDisplay("서버 접속 오류"+ ex.getMessage());
            }

        }
    }

    class disconnectAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습
            // TODO Auto-generated method stub
            chatClient.disconnect();
        }
    }

    public static void main(String[] args) {
        new ClientFrame("localhost", 54321);
    }
}

