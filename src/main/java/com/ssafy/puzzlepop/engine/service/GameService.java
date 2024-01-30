package com.ssafy.puzzlepop.engine.service;

import com.ssafy.puzzlepop.engine.domain.Game;
import com.ssafy.puzzlepop.engine.domain.User;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Getter
public class GameService {
    private Map<String, Game> gameRooms;

    @PostConstruct
    //의존관게 주입완료되면 실행되는 코드
    private void init() {
        gameRooms = new LinkedHashMap<>();
    }

    //채팅방 불러오기
    public List<Game> findAllRoom() {
        //채팅방 최근 생성 순으로 반환
        List<Game> result = new ArrayList<>(gameRooms.values());
        Collections.reverse(result);

        return result;
    }

    public Game findById(String roomId) {
        if (gameRooms.get(roomId) == null) {
            System.out.println("game이 없음.......");
        }
        return gameRooms.get(roomId);
    }

    //채팅방 생성
    public Game createRoom(String name, String userid) {
        Game game = Game.create(name, userid);
        gameRooms.put(game.getGameId(), game);
        return game;
    }

    public void deleteRoom(String name) {
        gameRooms.remove(name);
    }

//    public Game createRoom(String name, String userid, GameType type) {
//        Game game = Game.create(name, userid, type);
//        gameRooms.put(game.getGameId(), game);
//        return game;
//    }

    public Game startGame(String roomId) {
        System.out.println("GameService.startGame");
        System.out.println("gameid = " + roomId);
        System.out.println(gameRooms);
        Game game = findById(roomId);
        game.start();
        return game;
    }

    public Game playGame(String roomId, String message, String targets) {
        System.out.println("GameService.playGame");
        System.out.println(gameRooms);
        Game game = findById(roomId);

        //TODO
        //게임 진행 로직
        if (message.equals("ADD_PIECE")) {
            String[] stringToInt = targets.split(",");
            List<Integer> pieces = new LinkedList<>();
            for (int i = 0; i < stringToInt.length; i++) {
                pieces.add(Integer.parseInt(stringToInt[i]));
            }
            System.out.println("ADD_PIECE");
            game.getRedPuzzle().addPiece(pieces);
            game.getRedPuzzle().print();
        } else {
            System.out.println("구현중인 명령어 : " + message);
            System.out.println("targets = " + targets);
        }

        return game;
    }

    public boolean enterGame(String gameId, String userId, String sessionId) {
        Game game = gameRooms.get(gameId);
        if (game.enterPlayer(new User(userId), sessionId)) {
            return true;
        }

        return false;
    }
}
