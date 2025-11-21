import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import javax.swing.*;


public class Server {
    private JFrame frame;
    private JTextArea t_display;
    private JButton b_exit;
    private JPanel d_panel;
    private JPanel b_panel;
    private JTextField t_input;
    int port;
    private Thread acceptThread = null;
    ServerSocket serverSocket;
    Socket clientSocket;
    BufferedReader in;
    Vector<ClientHandler> users = new Vector<ClientHandler>();


    public Server(int port) {
        this.port=port;
        frame = new JFrame();
        frame.setTitle("Multi ChatServer");
        buildGUI();

        frame.setBounds(700, 200, 500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void buildGUI() {

        frame.add(createControlPanel(), BorderLayout.SOUTH);
        frame.add(createDisplayPanel(),BorderLayout.CENTER);

    }

    private JPanel createDisplayPanel() {
        t_display = new JTextArea();
        JPanel d_panel = new JPanel (new GridLayout());
        d_panel.add(t_display);

        return d_panel;
    }


    private JPanel createControlPanel(){
        JButton b_connect = new JButton("서버시작");
        JButton b_disconnect = new JButton("서버종료");
        JButton b_exit = new JButton("종료하기");

        b_connect.addActionListener(new conncetAction());
        b_disconnect.addActionListener(new disconnectAction());
        b_exit.addActionListener(new exitAction());

        JPanel b_panel = new JPanel (new GridLayout());
        b_panel.add(b_connect);
        b_panel.add(b_disconnect);
        b_panel.add(b_exit);

        return b_panel;
    }

    private void startServer() {
        clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다.\n");

            while (acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();
                t_display.append("클라이언트가 연결되었습니다.\n");

                ClientHandler cHandler = new ClientHandler(clientSocket);
                cHandler.start();
                users.add(cHandler);

            }
        }catch(SocketException e) {
            printDisplay("서버소켓종료");
        }
        catch(IOException e) {
            e.printStackTrace();
            printDisplay("서버 오류: " + e.getMessage());
        }
        finally {
            try {
                if(clientSocket != null) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch(IOException e) {
                System.err.println("서버 닫기 오류> "+e.getMessage());
                System.exit(-1);
            }
        }
    }

    private void printDisplay(String str){
        t_display.append(str+"\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }


    public void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        }catch(IOException e) {
            System.err.println("서버소켓 닫기 오류>" + e.getMessage());
            System.exit(-1);
        }
    }

    class MyActionListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습
            // TODO Auto-generated method stub
            t_display.append("\n서버가 종료되었습니다");
            System.exit(-1);
        }

    }


    class conncetAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습
            acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    startServer();
                }

            });
            acceptThread.start();
        }
    }

    class disconnectAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모
            disconnect();
        }
    }


    class exitAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) { // 매개변수로 ActionEvent가 오는 모습
            System.exit(-1);
        }
    }

    private class ClientHandler extends Thread{
        private Socket clientSocket;
        private ObjectOutputStream out;
        String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessage(Socket cs) {

            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()));

                String message;
                ChatMsg msg;
                while ((msg = (ChatMsg) in.readObject()) != null) {
                    if(msg.mode ==ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;

                        printDisplay("새 참가자:" + uid );
                        printDisplay("현재 참가자 수:" + users.size());
                        continue;
                    }
                    else if (msg.mode == ChatMsg.MODE_LOGOUT){
                       break;
                    }
                    else if( msg.mode == ChatMsg.MODE_TX_STRING){
                        message = uid +": " + msg.message;
                        printDisplay(message);
                        broadcasting(msg);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_IMAGE){
                        printDisplay(uid + ": "+ msg.message);
                        broadcasting(msg);
                    }
                }

                users.removeElement(this);
                printDisplay(uid +"퇴장, 현재 참가자 수: " + users.size());
            }catch(IOException e) {
                printDisplay("서버 읽기 오류> "+e.getMessage());
            } catch (ClassNotFoundException e) {
                printDisplay("수신 객체 클래스 오류> " + e.getMessage());
            } finally {
                try {
                    users.remove(this);
                    cs.close();
                } catch(IOException e) {
                    System.err.println("서버 닫기 오류> "+e.getMessage());
                    System.exit(-1);
                }
            }
        }

        private void send(ChatMsg msg){
            try{
                out.writeObject(msg);
                out.flush();
            }catch(IOException e){
                System.err.println("클라이언트 일반 전송 오류> "+ e.getMessage());
            }
        }

        private void sendMessage(String msg){
            send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING,msg));
        }

        private void broadcasting(ChatMsg msg) {
            for(ClientHandler c : users){
                c.send(msg);
            }
        }


        @Override
        public void run() {
            receiveMessage(clientSocket);
        }
    }

    public static void main(String[] args) {
        int port = 54321;

        Server server = new Server(port);
    }
}

