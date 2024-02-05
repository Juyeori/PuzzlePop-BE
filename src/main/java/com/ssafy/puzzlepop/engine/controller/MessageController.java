package com.ssafy.puzzlepop.engine.controller;

import com.ssafy.puzzlepop.engine.InGameMessage;
import com.ssafy.puzzlepop.engine.SocketError;
import com.ssafy.puzzlepop.engine.domain.*;
import com.ssafy.puzzlepop.engine.service.GameService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Controller
@RequiredArgsConstructor
@EnableScheduling
public class MessageController {

    @Autowired
    private GameService gameService;
    private final SimpMessageSendingOperations sendingOperations;
    private final int BATTLE_TIMER = 300;
    private String sessionId;
    private Map<String, String> sessionToGame;

    @PostConstruct
    public void init() {
        sessionToGame = new LinkedHashMap<>();
        sessionToGame = Collections.synchronizedMap(sessionToGame);
    }

    //세션 아이디 설정
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        System.out.println("MessageController.handleWebSocketConnectListener");
        System.out.println(event.getMessage().getHeaders().get("simpSessionId"));
        sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) throws InterruptedException {
        System.out.println("MessageController.handleDisconnectEvent");
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String gameId = sessionToGame.get(sessionId);
        Game game = gameService.findById(gameId);
        if (game == null) {
            return;
        }
        System.out.println(game.getSessionToUser().get(sessionId).getId() + " 님이 퇴장하십니다.");
        game.exitPlayer(sessionId);
        sessionToGame.remove(sessionId);

        if (game.isEmpty()) {
            System.out.println("game.isEmpty()");
            Thread.sleep(5000);
            if (game.isEmpty()) {
                System.out.println("진짜 나간것같아. 게임 지울게!");
                gameService.deleteRoom(gameId);
            } else {
                System.out.println("새로고침이였어. 다시 연결한다!");
                return;
            }
        }

        sendingOperations.convertAndSend("/topic/game/room/"+gameId, game);
    }



    @MessageMapping("/game/message")
    public void enter(InGameMessage message) {
        if (message.getType().equals(InGameMessage.MessageType.ENTER)) {
            Game game = gameService.findById(message.getRoomId());

            sessionToGame.put(sessionId, message.getRoomId());

            if (game.enterPlayer(new User(message.getSender()), sessionId)) {
                sendingOperations.convertAndSend("/topic/game/room/"+message.getRoomId(), game);
                System.out.println(gameService.findById(message.getRoomId()).getGameName() + "에 " + message.getSender() + "님이 입장하셨습니다.");
            } else {
                System.out.println("방 입장 실패");
                sendingOperations.convertAndSend("/topic/game/room/"+message.getRoomId(),new SocketError("room", "방 가득 참"));
                System.out.println(gameService.findById(message.getRoomId()).getGameName() + "에 " + message.getSender() + "님이 입장하지 못했습니다.");
            }
        } else if (message.getType().equals(InGameMessage.MessageType.CHAT)) {
            ResponseChatMessage responseChatMessage = new ResponseChatMessage();
            responseChatMessage.setChatMessage(message.getMessage());
            responseChatMessage.setUserid(message.getSender());
            responseChatMessage.setTime(new Date());
            sendingOperations.convertAndSend("/topic/chat/room/"+message.getRoomId(), responseChatMessage);
        } else {
            if (message.getMessage().equals("GAME_START")) {
                System.out.println("GAME_START");
                Game game = gameService.startGame(message.getRoomId());
                sendingOperations.convertAndSend("/topic/game/room/"+message.getRoomId(), game);
            } else {
                if (!gameService.findById(message.getRoomId()).isStarted()) {
                    System.out.println("게임 시작 안했음! 명령 무시함");
                    return;
                }
                System.out.println("명령어 : " + message.getMessage());
                System.out.println("게임방 : " + message.getRoomId());
                ResponseMessage res = gameService.playGame(message);
                sendingOperations.convertAndSend("/topic/game/room/"+message.getRoomId(), res);
            }
        }
    }

    //서버 타이머  제공
    @Scheduled(fixedRate = 1000)
    public void sendServerTime() {
        List<Game> allRoom = gameService.findAllCooperationRoom();
        allRoom.addAll(gameService.findAllBattleRoom());
        for (int i = allRoom.size()-1; i >= 0 ; i--) {
            if (allRoom.get(i).isStarted()) {
                long time = allRoom.get(i).getTime();
                if (allRoom.get(i).getGameType().equals("BATTLE")) {
                    time = BATTLE_TIMER-time;
                }
                if (time >= 0) {
                    sendingOperations.convertAndSend("/topic/game/room/" + allRoom.get(i).getGameId(), time);
                } else {
                    sendingOperations.convertAndSend("/topic/game/room/" + allRoom.get(i).getGameId(), "너 게임 끝났어! 이 방 폭파됨");
                    gameService.deleteRoom(allRoom.get(i).getGameId());
                }
//                System.out.println(allRoom.get(i).getGameName() + "에 " + allRoom.get(i).getTime() + "초 라고 보냈음");
            }
        }
    }

    //배틀 드랍 아이템 제공
    //20초에 한번씩 제공하기로 함
    //테스트용 확률 조정
    @Scheduled(fixedRate = 5000)
    public void sendDropItem() {
        //배틀로 변경해야함
        List<Game> allRoom = gameService.findAllCooperationRoom();
        Random random = new Random();
        for (int i = allRoom.size()-1; i >= 0 ; i--) {
            if (allRoom.get(i).isStarted()) {
                //확률 계산
                int possibility = random.nextInt(100);
                System.out.println(possibility + " %");
                if (possibility <= 70) {
                    DropItem item = DropItem.randomCreate();
                    System.out.println(item + "을 생성합니다.");
                    ResponseMessage res = new ResponseMessage();
                    res.setMessage("DROP_ITEM");
                    res.setRandomItem(item);
                    sendingOperations.convertAndSend("/topic/game/room/" + allRoom.get(i).getGameId(), res);
                }
            }
        }
    }

}

