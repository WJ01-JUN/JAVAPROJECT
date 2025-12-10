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
        GAME_EVENT,     // 미니게임(오목) 이벤트
        USER_LIST ,      // 방 참가자 목록
        IMAGE  ,          //이미지 전달
        LEAVE_ROOM       //클라이언트 ->  서버?
    }

    public enum GameAction {
        REQUEST_JOIN_PLAYER,
        REQUEST_SPECTATOR,
        STATE,              // 전체 상태 동기화 (보드, 턴, 플레이어)
        MOVE,               // 단일 수 적용 알림
        RESULT,             // 최종 결과
        RESIGN,             // 기권
        ERROR               // 게임 전용 오류
    }

    private Type type;
    private String room;       // 어느 방에 속한 건지
    private String sender;     // 보낸 사람 닉네임
    private String text;       // 메시지 내용
    private List<String> rooms; // ROOM_LIST 용
    private List<String> users; //USER_LIST용
    private ImageIcon image;

    // ===== 게임 관련 필드 =====
    private GameAction gameAction;
    private int x;
    private int y;
    private int[][] board;
    private String blackPlayer;
    private String whitePlayer;
    private String currentTurn;
    private boolean finished;
    private String winner;
    private String resultReason;
    private List<String> spectators;

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

    public static Message leaveRoom(String roomName, String name) {
        Message m = new Message(Type.LEAVE_ROOM);
        m.sender = name;
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

    // ===== 게임용 편의 생성자들 =====

    public static Message gameJoinPlayer(String room) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.REQUEST_JOIN_PLAYER;
        return m;
    }

    public static Message gameJoinSpectator(String room) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.REQUEST_SPECTATOR;
        return m;
    }

    public static Message gameMove(String room, int x, int y) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.MOVE;
        m.x = x;
        m.y = y;
        return m;
    }

    public static Message gameResign(String room) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.RESIGN;
        return m;
    }

    public static Message gameState(String room,
                                    int[][] board,
                                    String blackPlayer,
                                    String whitePlayer,
                                    String currentTurn,
                                    boolean finished,
                                    String winner,
                                    String resultReason,
                                    List<String> spectators) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.STATE;
        m.board = board;
        m.blackPlayer = blackPlayer;
        m.whitePlayer = whitePlayer;
        m.currentTurn = currentTurn;
        m.finished = finished;
        m.winner = winner;
        m.resultReason = resultReason;
        m.spectators = spectators;
        return m;
    }

    public static Message gameError(String room, String text) {
        Message m = new Message(Type.GAME_EVENT);
        m.room = room;
        m.gameAction = GameAction.ERROR;
        m.text = text;
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

    // 게임 관련 getter
    public GameAction getGameAction() { return gameAction; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int[][] getBoard() { return board; }
    public String getBlackPlayer() { return blackPlayer; }
    public String getWhitePlayer() { return whitePlayer; }
    public String getCurrentTurn() { return currentTurn; }
    public boolean isFinished() { return finished; }
    public String getWinner() { return winner; }
    public String getResultReason() { return resultReason; }
    public List<String> getSpectators() { return spectators; }
}
