package com.ssafy.puzzlepop.engine;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.*;

@Getter
@NoArgsConstructor
public class Game {
    private String gameId;
    private String gameName;

    private GameType gameType;

    private User admin;

    private Team redTeam;
    private Team blueTeam;
    private PuzzleBoard redPuzzle;
    private PuzzleBoard bluePuzzle;

    private Date startTime;

    private boolean isStarted = false;

//    public Game(String gameName, int userid) {
//        this.redTeam = new Team(new LinkedList<>());
//        this.blueTeam = new Team(new LinkedList<>());
//
//        this.admin = new User(userid);
//        this.gameId = UUID.randomUUID().toString();
//        this.gameName = gameName;
//    }

    public void changeTeam(User user) {
        if (redTeam.isIn((user))) {
            redTeam.deletePlayer(user);
            blueTeam.addPlayer(user);
        } else {
            blueTeam.deletePlayer(user);
            redTeam.addPlayer(user);
        }

    }

    public boolean enterPlayer(User user) {
        if (redTeam.getPlayers().size() <= 3) {
            redTeam.addPlayer(user);
            return true;
        } else {
            if (blueTeam.getPlayers().size() <= 3) {
                blueTeam.addPlayer(user);
                return true;
            } else {
                return false;
            }
        }
    }

    public static Game create(String name, String userid) {
        User user = new User(userid);
        Game game = new Game();
        String uuid = UUID.randomUUID().toString();


        Team red = new Team(new LinkedList<>());
        Team blue = new Team(new LinkedList<>());

        game.redTeam = red;
        game.blueTeam = blue;

        game.gameType = GameType.BATTLE;

        game.redTeam.addPlayer(user);

        game.gameId = uuid;
        game.gameName = name;
        game.admin = user;


        System.out.println(name + "배틀 방 생성 / id = " + uuid + " / 방장 id = " + user.getId());


        return game;
    }
//    public static Game create(String name, String userid, GameType type) {
//        User user = new User(userid);
//        Game game = new Game();
//        String uuid = UUID.randomUUID().toString();
//
//        if (type == GameType.BATTLE) {
//            Team red = new Team(new LinkedList<>());
//            Team blue = new Team(new LinkedList<>());
//
//            game.redTeam = red;
//            game.blueTeam = blue;
//
//            game.gameType = GameType.BATTLE;
//
//            game.redTeam.addPlayer(user);
//
//            game.gameId = uuid;
//            game.gameName = name;
//            game.admin = user;
//
//
//            System.out.println(name + "배틀 방 생성 / id = " + uuid + " / 방장 id = " + user.getId());
//        } else {
//            Team red = new Team(new LinkedList<>());
//
//            game.redTeam = red;
//
//            game.gameType = GameType.TEAM;
//            game.redTeam.addPlayer(user);
//            game.gameId = uuid;
//            game.gameName = name;
//            game.admin = user;
//
//            System.out.println(name + "협동 방 생성 / id = " + uuid + " / 방장 id = " + user.getId());
//        }
//
//        return game;
//    }

    public void start() {
        Picture picture = new Picture(64, 48, ".");
        redPuzzle = new PuzzleBoard();
        bluePuzzle = new PuzzleBoard();
        redPuzzle.init(picture);
        bluePuzzle.init(picture);

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        startTime = new Date();
        System.out.println(startTime);
        isStarted = true;
        System.out.println("------------------게임 시작-------------------");
        redPuzzle.print();
        bluePuzzle.print();
    }

    //초 단위 경과 시간
    public long getTime() {
        Date nowTime = new Date();
        long start = startTime.getTime();
        long now = nowTime.getTime();
        return (now-start)/1000;
    }

//    public static void main(String[] args) {
//        Scanner sc = new Scanner(System.in);
//        Picture picture = new Picture(64, 48, ".");
//
//        Team redTeam = new Team(new LinkedList<>());
//        Team blueTeam = new Team(new LinkedList<>());
//        PuzzleBoard redPuzzle = null;
//        PuzzleBoard bluePuzzle = null;
//        Map<Integer, String> team = new HashMap<>();
//        boolean gameStart = false;
//
//        while (true) {
//            System.out.print("enter command : ");
//            String command = sc.next();
//
//            if (command.equals("adduser")) {
//                System.out.print("enter user id : ");
//                int id = sc.nextInt();
//                System.out.print("enter team : ");
//                String color = sc.next();
//
//                if (color.equals("red")) {
//                    redTeam.getPlayers().add(new User(id));
//                    team.put(id, "red");
//                } else {
//                    blueTeam.getPlayers().add(new User(id));
//                    team.put(id, "blue");
//                }
//
//                System.out.println("redTeam = " + redTeam);
//                System.out.println("blueTeam = " + blueTeam);
//            }
//
//            if (command.equals("gamestart")) {
//                redPuzzle = new PuzzleBoard();
//                bluePuzzle = new PuzzleBoard();
//                redPuzzle.init(picture);
//                bluePuzzle.init(picture);
//                gameStart = true;
//
//                System.out.println("---------Game Start!!!----------");
//                redPuzzle.print();
//                bluePuzzle.print();
//                continue;
//            }
//
//            if (command.equals("addpiece")) {
//                System.out.print("enter user id : ");
//                int id = sc.nextInt();
//
//                List<Integer> targets = new LinkedList<>();
//
//                while (true) {
//                    int piece = sc.nextInt();
//                    if (piece == -1) {
//                        break;
//                    }
//
//                    targets.add(piece);
//                }
//
//                if (team.get(id).equals("red")) {
//                    redPuzzle.addPiece(targets);
//                    if (targets.size() >= 3) {
//                        redPuzzle.combo(targets);
//                    }
//                } else {
//                    bluePuzzle.addPiece(targets);
//                    if (targets.size() >= 3) {
//                        bluePuzzle.combo(targets);
//                    }
//                }
//
//                redPuzzle.print();
//                bluePuzzle.print();
//
//                continue;
//            }
//
//            if (command.equals("additem")) {
//                System.out.print("enter user id : ");
//                int id = sc.nextInt();
//                System.out.print("enter item name : ");
//                String item = sc.next();
//
//                if (team.get(id).equals("red")) {
//                    redPuzzle.addItem(item);
//                } else {
//                    bluePuzzle.addItem(item);
//                }
//
//                redPuzzle.print();
//                bluePuzzle.print();
//
//                continue;
//            }
//
//            if (command.equals("useitem")) {
//                System.out.print("enter user id : ");
//                int id = sc.nextInt();
//                System.out.print("enter item keyboard number : ");
//                int itemNumber = sc.nextInt();
//
//                if (team.get(id).equals("red")) {
//                    redPuzzle.useItem(itemNumber, redPuzzle);
//
//                } else {
//                    bluePuzzle.useItem(itemNumber, bluePuzzle);
//                }
//
//                redPuzzle.print();
//                bluePuzzle.print();
//                continue;
//            }
//        }
//    }
}
