## 오목 미니게임 프로토콜 설계

채팅방 기반의 멀티룸 채팅 시스템 위에 오목 게임을 올리는 구조이며,  
채팅과 게임 메시지는 `Type` / `GameAction`으로 명확히 구분합니다.

---

### 1. 메시지 타입 및 공통 구조

- **Type**
  - `CHAT` : 일반 채팅/방 관련 메시지
  - `GAME_EVENT` : 오목 게임 관련 메시지

- **GameAction**
  - `REQUEST_JOIN_PLAYER`
  - `REQUEST_SPECTATOR`
  - `MOVE`
  - `RESIGN`
  - `STATE`
  - `ERROR`
  - (필요 시 확장 가능)

`ClientHandler`는 `Type == GAME_EVENT` 인 메시지를 받아  
`GameAction`에 따라 오목 게임 로직으로 분기 처리합니다.

---

### 2. 클라이언트 → 서버 프로토콜

#### 2.1 REQUEST_JOIN_PLAYER (플레이어로 참여)

- **설명**: 현재 채팅방에서 오목 게임에 **플레이어**로 참여 요청.
- **방향**: Client → Server
- **사용 시점**
  - 유저가 “게임 시작” / “플레이어로 참여” 버튼을 눌렀을 때
- **주요 필드 예시**
  - `roomName` : 게임이 진행될 채팅방 이름
  - `userId` : 요청한 사용자 ID

---

#### 2.2 REQUEST_SPECTATOR (관전자로 참여)

- **설명**: 진행 중인 오목 게임에 **관전자**로 입장 요청.
- **방향**: Client → Server
- **사용 시점**
  - 게임이 이미 진행 중일 때, 다른 유저가 관전 버튼을 눌렀을 때
- **주요 필드 예시**
  - `roomName`
  - `userId`

---

#### 2.3 MOVE (돌 두기)

- **설명**: 바둑알(돌)을 `(x, y)` 위치에 두겠다는 요청.
- **방향**: Client → Server
- **사용 시점**
  - 자신의 턴에 보드에서 특정 좌표를 클릭했을 때
- **주요 필드 예시**
  - `roomName`
  - `userId`
  - `x`, `y` : 보드 좌표

---

#### 2.4 RESIGN (기권)

- **설명**: 현재 게임을 기권하는 요청.
- **방향**: Client → Server
- **사용 시점**
  - 사용자가 “기권” 버튼을 눌렀을 때
- **주요 필드 예시**
  - `roomName`
  - `userId`

---

### 3. 서버 → 클라이언트 프로토콜

#### 3.1 STATE (게임 전체 상태 동기화)

- **설명**: 게임의 **전체 상황**(보드, 턴, 플레이어, 결과 등)을 전송하는 메시지.
- **방향**: Server → Client
- **특징**
  - **플레이어 및 관전자 모두** 이 메시지를 수신하여 화면을 동기화.
  - 별도의 `RESULT` 액션 없이, **게임 결과 정보도 STATE 안에 포함**.
- **주요 필드 예시**
  - `roomName`
  - `board` : 현재 보드 상태(2차원 배열)
  - `currentTurnUserId` : 현재 수를 둘 차례인 유저 ID
  - `blackPlayerId`, `whitePlayerId`
  - `status` : `"PLAYING"`, `"FINISHED"` 등
  - `winnerUserId` : 게임이 끝난 경우 승자 ID (없으면 null)

> 서버에서는 `OmokGame.toStateMessage()` 를 통해  
> 현재 게임 상태를 `STATE` 메시지로 직렬화하고,  
> 해당 방의 모든 참여자(플레이어 + 관전자)에게 방송합니다.

---

#### 3.2 ERROR (오류 메시지)

- **설명**: 잘못된 요청이나 게임 불가능 상태에 대한 오류 전송.
- **방향**: Server → Client
- **사용 시점**
  - 턴이 아닌 사용자가 `MOVE` 를 보낸 경우
  - 유효하지 않은 좌표, 게임이 없는 방에서의 요청 등
- **주요 필드 예시**
  - `roomName`
  - `message` : 오류 내용 (예: `"Not your turn"`, `"Invalid position"`)

---

### 4. GAME_EVENT 흐름 및 동기화 구조

1. **클라이언트에서 GAME_EVENT 전송**
   - `Message.Type = GAME_EVENT`
   - `GameAction = REQUEST_JOIN_PLAYER / MOVE / RESIGN ...`

2. **서버(ClientHandler)에서 처리**
   - `ClientHandler`는 `GAME_EVENT` 메시지를 받아 `GameAction`별로 분기.
   - 오목 로직은 `OmokGame` 객체가 담당.
   - 게임 상태 변화가 있을 때마다 `OmokGame.toStateMessage()`를 호출해  
     최신 `STATE` 메시지를 생성.

3. **STATE 브로드캐스트**
   - 생성된 `STATE` 메시지는 `ChatRoom` 단위로 브로드캐스트.
   - 플레이어 뿐 아니라 **관전자에게도 동일한 STATE**를 전송해  
     모든 클라이언트가 같은 화면을 보도록 유지.

4. **클라이언트 측 화면 갱신 흐름**
   - `STATE`의 1차 수신자는 **`ChatFrame`**.
   - `ChatFrame`은 수신한 `STATE` 메시지를 **`OmokWindow`**에 전달.
   - `OmokWindow`는 `applyState(...)`를 통해
     - 보드 상태
     - 턴 정보
     - 승패 상태
     를 반영하고 화면을 갱신.

---

### 5. 방 나가기 & 게임 리셋 처리

- 플레이어가 게임 중에 채팅방을 떠날 경우:
  - `ChatRoom.leave(...)` 에서 `OmokGame.onUserLeft(...)` 를 호출.
  - `OmokGame`은 내부적으로 보드를 초기화하고,  
    **패배 처리 없이** 게임을 리셋.
  - 이후 보드가 초기화된 새로운 `STATE` 메시지를 다시 방송하여  
    남아있는 클라이언트들의 화면을 초기 상태로 맞춘다.

이 구조를 통해 채팅방/게임 로직을 분리하면서도,  
모든 클라이언트(플레이어 + 관전자)가 `STATE` 메시지를 기준으로  
항상 동일한 게임 화면을 공유할 수 있도록 설계되었다.
