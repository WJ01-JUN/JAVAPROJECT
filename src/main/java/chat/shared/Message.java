package chat.shared;

import javax.swing.*;
import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

    public enum Type {
        LOGIN,          // 클라이언트 → 서버 (닉네임 전달)
        ROOM_LIST,      // 서버 → 클라이언트 (방 목록 전체)
        CREATE_ROOM,    // 클라이언트 → 서버 (방 만들기 요청)
        JOIN_ROOM,      // 클라이언트 → 서버 (방 입장 요청)
        CHAT,           // 양방향 (채팅 메시지)
        SYSTEM,         // 서버 → 클라이언트 (공지, 안내 등)
        ERROR,          // 서버 → 클라이언트 (에러 안내)
        GAME_EVENT,     // 나중에 미니게임용
        USER_LIST ,      // 방 참가자 목록
        IMAGE            //이미지 전달
    }

    private Type type;
    private String room;       // 어느 방에 속한 건지
    private String sender;     // 보낸 사람 닉네임
    private String text;       // 메시지 내용
    private List<String> rooms; // ROOM_LIST 용
    private List<String> users; //USER_LIST용
    private ImageIcon image;

    public Message(Type type) {
        this.type = type;
    }

    // 편의 생성자들
    public static Message login(String nickname) {
        Message m = new Message(Type.LOGIN);
        m.sender = nickname;
        return m;
    }

    public static Message createRoom(String roomName) {
        Message m = new Message(Type.CREATE_ROOM);
        m.room = roomName;
        return m;
    }

    public static Message joinRoom(String roomName) {
        Message m = new Message(Type.JOIN_ROOM);
        m.room = roomName;
        return m;
    }

    public static Message chat(String room, String sender, String text) {
        Message m = new Message(Type.CHAT);
        m.room = room;
        m.sender = sender;
        m.text = text;
        return m;
    }

    public static Message roomList(List<String> rooms) {
        Message m = new Message(Type.ROOM_LIST);
        m.rooms = rooms;
        return m;
    }

    public static Message system(String text) {
        Message m = new Message(Type.SYSTEM);
        m.text = text;
        return m;
    }

    public static Message error(String text) {
        Message m = new Message(Type.ERROR);
        m.text = text;
        return m;
    }

    public static Message systemForRoom(String room, String text) {
        Message m = new Message(Type.SYSTEM);
        m.room = room;
        m.text = text;
        return m;
    }


    public static Message userList(String room, List<String> users) {
        Message m = new Message(Type.USER_LIST);
        m.room = room;
        m.users = users;
        return m;
    }

    public static Message sendImage(String room, String sender, ImageIcon image){
        Message m = new Message(Type.IMAGE);
        m.room = room;
        m.sender = sender;
        m.image = image;

        return m;
    }

    // getter
    public Type getType() { return type; }
    public String getRoom() { return room; }
    public String getSender() { return sender; }
    public String getText() { return text; }
    public List<String> getRooms() { return rooms; }
    public List<String> getUsers() { return users; }
    public ImageIcon getImage() {return image; }
}
