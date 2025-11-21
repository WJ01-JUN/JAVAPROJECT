import java.awt.*;
import java.io.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.*;
import java.nio.charset.StandardCharsets;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;


public class Client extends JFrame{
    private JFrame frame;
    JTextPane t_display;
    JTextField t_input, t_userID, t_userAddress,t_userPort;
    JButton b_send,b_connect,b_disconnect,b_exit, b_select;
    String serverAddress, userID;
    int serverPort;
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    private Thread receiveThread =null;
    DefaultStyledDocument document;



    public Client(String serverAddress, int serverPort) {

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        frame = new JFrame();
        frame.setTitle("Multi Talk");

        buildGUI();

        frame.setBounds(100, 200, 500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void buildGUI() {

        frame.add(connectPanel(), BorderLayout.SOUTH);
        frame.add(createDisplayPanel(),BorderLayout.CENTER);

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

                int ret = chooser.showOpenDialog(Client.this);
                if(ret != JFileChooser.APPROVE_OPTION){
                    JOptionPane.showMessageDialog(Client.this, "파일을 선택하지 않음","경고",JOptionPane.WARNING_MESSAGE);
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

    private JPanel createControlPanel(){
        JButton b_connect = new JButton("접속하기");
        JButton b_disconnect = new JButton("접속끊기");
        JButton b_exit = new JButton("종료하기");

        b_connect.addActionListener(new conncetAction());
        b_disconnect.addActionListener(new disconnectAction());

        JPanel b_panel = new JPanel (new GridLayout());
        b_panel.add(b_connect);
        b_panel.add(b_disconnect);
        b_panel.add(b_exit);

        return b_panel;
    }

    private JPanel connectPanel() {
        JPanel cn_panel = new JPanel (new GridLayout(3,0));

        cn_panel.add(createInputPanel());
        cn_panel.add(createInfoPanel());
        cn_panel.add(createControlPanel());

        return cn_panel;
    }

    public void connectToServer() throws UnknownHostException, IOException {
            //socket = new Socket(serverAddress, serverPort);
            socket = new Socket();
            SocketAddress sa = new InetSocketAddress(serverAddress,serverPort);
            socket.connect(sa, 3000);


            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            receiveThread = new Thread(new Runnable() {

                private void receiveMessage(){
                    try{
                        ChatMsg inMsg = (ChatMsg)in.readObject();
                        if (inMsg ==null) {
                            disconnect();
                            printDisplay("서버 연결 끊김");
                            return;
                        }

                        switch(inMsg.mode){
                            case ChatMsg.MODE_TX_IMAGE:
                                printDisplay(inMsg.userID + " : " + inMsg.message);
                                printDisplay(inMsg.image);
                                break;
                            case ChatMsg.MODE_TX_STRING :
                                printDisplay(inMsg.userID + " : " + inMsg.message);
                                break;

                        }
                    }catch(IOException e){
                        printDisplay("연결을 종료했습니다.");
                    }catch(ClassNotFoundException e){
                        printDisplay("잘못된 객체가 전달되었습니다." + e.getMessage());
                    }
                }

                @Override
                public void run() {

                    try{
                        //connecToServer에 존재하면 서버가 중지됨 (out이 없으면 block 됨.)
                        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                    } catch (IOException e) {
                        printDisplay("입력 스트림이 열리지 않음");
                    }

                    while (receiveThread == Thread.currentThread()) {//리시브 스레드에 대한 참조값이 나이면 무한반복
                        receiveMessage();
                    }
                }

            });
            receiveThread.start();
        }

    private void sendUserID(){
        userID = t_userID.getText();
        send(new ChatMsg(userID,ChatMsg.MODE_LOGIN));
    }

    public void disconnect() {
        send(new ChatMsg(userID, ChatMsg.MODE_LOGOUT));

        try {
            receiveThread =null;
            socket.close();
        }catch(IOException e) {
            System.err.println("클라이언트 닫기 오류>" + e.getMessage());
            System.exit(-1);
        }
    }

    private void send(ChatMsg msg){ // 다양한 방식의 전달이 필요해 send() 선언
        try{
            out.writeObject(msg);
            out.flush();
        }catch (IOException e){
            System.err.println("클라이언트 일반 전송 오류> "+ e.getMessage());
        }
    }

    public void sendMessage() {
        String message = t_input.getText();
        if(message.isEmpty()) return;

        send(new ChatMsg(userID, ChatMsg.MODE_TX_STRING,message));

        t_input.setText("");
    }

    private void printDisplay(String msg){
        int len = t_display.getDocument().getLength();
        try{
            document.insertString(len,msg +"\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);

    }

    private void printDisplay(ImageIcon icon){
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

    private void sendImage(){
        String filename = t_input.getText().strip();
        if(filename.isEmpty()) return;

        File file = new File(filename);
        if(!file.exists()){
            printDisplay(">>파일이 존재하지 않습니다." + filename);
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(userID, ChatMsg.MODE_TX_IMAGE,file.getName(),icon));

        t_input.setText("");
    }

    public void receiveMessage() {
        try{
            ChatMsg inMsg = ((ChatMsg)in.readObject());
            if(inMsg == null){
                disconnect();
                printDisplay("서버 연결 끊김");
                return;
            }
            switch (inMsg.mode){
                case ChatMsg.MODE_TX_STRING :
                    printDisplay(inMsg.userID+": "+inMsg.message);
                    break;
            }
        } catch(IOException e){
            printDisplay("연결을 종료했습니다.");
        }catch(ClassNotFoundException e){
            printDisplay("잘못된 객체가 전달되었습니다.");
        }
    }



    class conncetAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습

            try{
                connectToServer();
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
            disconnect();
        }
    }


    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        new Client(serverAddress, serverPort);
    }
}

