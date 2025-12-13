package chat.server;

import chat.shared.Message;

import java.util.HashSet;
import java.util.Set;

/**
 * 채팅방 단위 오목 게임 상태를 관리한다.
 * 서버가 룰을 판정하고 상태 스냅샷을 브로드캐스트한다.
 */
public class OmokGame {

    public static final int BOARD_SIZE = 15;

    private final ChatRoom room;

    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE]; // 0: 빈칸, 1: 흑, 2: 백

    private String blackPlayer;
    private String whitePlayer;
    private String currentTurn; // 닉네임 기준
    private boolean finished;
    private String winner;
    private String resultReason;

    private final Set<String> spectators = new HashSet<>();

    public OmokGame(ChatRoom room) {
        this.room = room;
    }

    public synchronized void joinAsPlayer(String nickname) {
        if (finished) reset();

        // 이미 플레이어라면 그대로 유지
        if (nickname.equals(blackPlayer) || nickname.equals(whitePlayer)) {
            return;
        }

        // 슬롯 배정
        if (blackPlayer == null) {
            blackPlayer = nickname;
        } else if (whitePlayer == null) {
            whitePlayer = nickname;
        } else {
            throw new IllegalStateException("이미 두 플레이어가 참여 중입니다.");
        }

        spectators.remove(nickname);

        // 두 명이 모두 채워지면 게임 시작
        if (blackPlayer != null && whitePlayer != null && currentTurn == null) {
            currentTurn = blackPlayer; // 흑 선
            finished = false;
            winner = null;
            resultReason = null;
        }
    }

    public synchronized boolean joinAsSpectator(String nickname) {
        if (nickname.equals(blackPlayer) || nickname.equals(whitePlayer)) {
            return false;
        }
        return spectators.add(nickname);
    }

    /**
     * 자동으로 플레이어/관전자 결정하여 참여
     * @return "PLAYER" 또는 "SPECTATOR"
     */
    public synchronized String tryJoin(String nickname) {
        if (finished) reset();

        // 이미 플레이어인 경우
        if (nickname.equals(blackPlayer) || nickname.equals(whitePlayer)) {
            return "PLAYER";
        }

        // 빈 슬롯이 있으면 플레이어로
        if (blackPlayer == null) {
            blackPlayer = nickname;
            spectators.remove(nickname);
            if (whitePlayer != null && currentTurn == null) {
                currentTurn = blackPlayer;
            }
            return "PLAYER";
        }
        if (whitePlayer == null) {
            whitePlayer = nickname;
            spectators.remove(nickname);
            if (blackPlayer != null && currentTurn == null) {
                currentTurn = blackPlayer;
            }
            return "PLAYER";
        }

        // 슬롯이 다 찼으면 관전자로
        spectators.add(nickname);
        return "SPECTATOR";
    }

    public synchronized void resign(String nickname) {
        if (finished) return;
        if (!nickname.equals(blackPlayer) && !nickname.equals(whitePlayer)) {
            return;
        }
        finished = true;
        winner = nickname.equals(blackPlayer) ? whitePlayer : blackPlayer;
        resultReason = "RESIGN";
    }

    public synchronized void placeStone(String nickname, int x, int y) {
        validateInRange(x, y);
        if (finished) {
            throw new IllegalStateException("이미 종료된 게임입니다.");
        }
        if (!nickname.equals(currentTurn)) {
            throw new IllegalStateException("지금은 " + currentTurn + "의 차례입니다.");
        }

        int stone = nickname.equals(blackPlayer) ? 1 : (nickname.equals(whitePlayer) ? 2 : 0);
        if (stone == 0) {
            throw new IllegalStateException("플레이어가 아닌 사용자는 수를 둘 수 없습니다.");
        }

        if (board[x][y] != 0) {
            throw new IllegalStateException("이미 돌이 놓인 자리입니다.");
        }

        board[x][y] = stone;

        if (checkWin(x, y, stone)) {
            finished = true;
            winner = nickname;
            resultReason = "WIN";
        } else if (isBoardFull()) {
            finished = true;
            winner = null;
            resultReason = "DRAW";
        } else {
            // 턴 전환
            currentTurn = (stone == 1) ? whitePlayer : blackPlayer;
        }
    }

    public synchronized Message toStateMessage() {
        // 보드 복사본 제공
        int[][] snapshot = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, snapshot[i], 0, BOARD_SIZE);
        }

        java.util.List<String> spectatorList = new java.util.ArrayList<>(spectators);

        return Message.gameState(
                room.getName(),
                snapshot,
                blackPlayer,
                whitePlayer,
                currentTurn,
                finished,
                winner,
                resultReason,
                spectatorList
        );
    }

    public synchronized void onUserLeft(String nickname) {
        // 플레이어가 나가면 결과 없이 게임 상태 초기화
        if (nickname.equals(blackPlayer) || nickname.equals(whitePlayer)) {
            reset();
            return;
        }
        // 관전자만 제거
        spectators.remove(nickname);
    }

    public synchronized boolean isPlayer(String nickname) {
        return nickname != null && (nickname.equals(blackPlayer) || nickname.equals(whitePlayer));
    }

    public synchronized boolean hasPlayers() {
        return blackPlayer != null || whitePlayer != null;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    private void validateInRange(int x, int y) {
        if (x < 0 || y < 0 || x >= BOARD_SIZE || y >= BOARD_SIZE) {
            throw new IllegalArgumentException("좌표가 범위를 벗어났습니다.");
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 0) return false;
            }
        }
        return true;
    }

    private boolean checkWin(int x, int y, int stone) {
        // 4개 방향(가로, 세로, 대각2)에서 연속 5개 확인
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int count = 1;
            count += countDir(x, y, d[0], d[1], stone);
            count += countDir(x, y, -d[0], -d[1], stone);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countDir(int x, int y, int dx, int dy, int stone) {
        int cnt = 0;
        int cx = x + dx;
        int cy = y + dy;
        while (cx >= 0 && cy >= 0 && cx < BOARD_SIZE && cy < BOARD_SIZE && board[cx][cy] == stone) {
            cnt++;
            cx += dx;
            cy += dy;
        }
        return cnt;
    }

    private void reset() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = 0;
            }
        }
        finished = false;
        winner = null;
        resultReason = null;
        currentTurn = null;
        blackPlayer = null;
        whitePlayer = null;
        spectators.clear();
    }
}

