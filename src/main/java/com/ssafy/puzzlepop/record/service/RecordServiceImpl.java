package com.ssafy.puzzlepop.record.service;

import com.ssafy.puzzlepop.gameinfo.domain.GameInfoDto;
import com.ssafy.puzzlepop.gameinfo.service.GameInfoService;
import com.ssafy.puzzlepop.record.domain.Record;
import com.ssafy.puzzlepop.record.domain.*;
import com.ssafy.puzzlepop.record.exception.RecordException;
import com.ssafy.puzzlepop.record.repository.RecordRepository;
import com.ssafy.puzzlepop.team.domain.TeamDto;
import com.ssafy.puzzlepop.team.service.TeamService;
import com.ssafy.puzzlepop.teamuser.domain.TeamUserResponseDto;
import com.ssafy.puzzlepop.teamuser.service.TeamUserService;
import com.ssafy.puzzlepop.user.domain.UserDto;
import com.ssafy.puzzlepop.user.domain.UserInfoDto;
import com.ssafy.puzzlepop.user.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final GameInfoService gameInfoService;
    private final TeamService teamService;
    private final TeamUserService teamUserService;
    private final UserService userService;


    @Autowired
    public RecordServiceImpl(RecordRepository recordRepository, GameInfoService gameInfoService, TeamService teamService, TeamUserService teamUserService, UserService userService) {
        this.recordRepository = recordRepository;
        this.gameInfoService = gameInfoService;
        this.teamService = teamService;
        this.teamUserService = teamUserService;
        this.userService = userService;
    }

    ///////

    @Override
    public Long createRecord(RecordCreateDto recordCreateDto) throws RecordException {
        if (recordCreateDto.getUserId() == null || recordCreateDto.getGameId() <= 0) {
            throw new RecordException("bad request");
        }

        // recordCreateDto의 데이터 기반 엔티티 생성
        Record record = new Record();

        record.setUserId(recordCreateDto.getUserId());
        record.setGameId(recordCreateDto.getGameId());

        // db에 저장
        try {
            record = recordRepository.save(record);
            return record.getId();
        } catch (Exception e) {
            throw new RecordException("error occurred while create dm");
        }

    }

    @Override
    public Long updateRecord(RecordDto recordDto) throws RecordException {
        if (recordDto.getId() <= 0 || recordDto.getUserId() == null || recordDto.getGameId() <= 0) {
            throw new RecordException("bad request");
        }

        Record existRecord = recordRepository.findById(recordDto.getId()).orElse(null);

        if (existRecord == null) {
            throw new RecordException("존재하지 않는 기록입니다.");
        }

        existRecord.setUserId(recordDto.getUserId());
        existRecord.setGameId(recordDto.getGameId());

        try {
            recordRepository.save(existRecord);
            return existRecord.getId();
        } catch (Exception e) {
            throw new RecordException("error occurred during update record data");
        }

    }

    @Override
    public void deleteRecord(Long id) throws RecordException {
        if (id <= 0) {
            throw new RecordException("bad request");
        }

        Record existRecord = recordRepository.findById(id).orElse(null);
        if (existRecord == null) {
            throw new RecordException("record match to id doesn't exist");
        }

        try {
            recordRepository.deleteById(id);
        } catch (Exception e) {
            throw new RecordException("error occurred during delete record data");
        }
    }

    @Override
    public RecordDto getRecordById(Long id) throws RecordException {
        if (id <= 0) {
            throw new RecordException("bad request");
        }

        Record existRecord = recordRepository.findById(id).orElse(null);
        if (existRecord == null) {
            throw new RecordException("record matches to id doesn't exist");
        }

        try {
            return new RecordDto(existRecord);
        } catch (Exception e) {
            throw new RecordException("error occurred during find record data");
        }
    }

    /////////////////

    @Transactional
    @Override
    public List<RecordDetailDto> getRecentRecordsByUserId(Long userId) throws RecordException {
        if (userId == null) {
            throw new RecordException("bad request");
        }

        try {
            // userId에 대한 전체 전적 목록
            List<Record> recordList = recordRepository.findByUserId(userId);
            Collections.reverse(recordList); // 최신 순으로 정렬

            // 전적 정보 담을 리스트 생성
            List<RecordDetailDto> recordDetailList = new ArrayList<>();

            // 전적이 20개 이상인 경우, 최근 20개만 거르기
            if (recordList.size() > 20) { // 20개 이상인 경우 리스트 자르기
                recordList.subList(0, 19);
            }

            // 각 recordDetailDto 채우기!
            for (Record record : recordList) {
                RecordDetailDto recordDetailDto = new RecordDetailDto();

                // gameId 바탕으로 gameInfo 가져오기
                GameInfoDto gameInfoDto = gameInfoService.readGameInfo(record.getGameId());
                recordDetailDto.setGameInfo(gameInfoDto);

                // gameId 바탕으로 teamList 가져오기
                List<TeamDto> teamDtoList = teamService.findAllByGameId(record.getGameId());
                recordDetailDto.setTeamList(teamDtoList);

                // 싱글 게임인 경우
                // 팀 리스트 2개 다 null로 리턴
                if ("SINGLE".equals(gameInfoDto.getType())) {
                    recordDetailList.add(recordDetailDto);
                }
                // 협동 게임인 경우
                // teamList1에 담고 teamList2는 null로 리턴
                else if ("COOPERATION".equals(gameInfoDto.getType())) {
                    List<TeamUserResponseDto> userTeamDtoList = teamUserService.findAllByTeamId(teamDtoList.get(0).getId());
                    recordDetailDto.setUserTeamList1(userTeamDtoList);
                    recordDetailList.add(recordDetailDto);
                }
                // 배틀 게임인 경우
                // teamList1과 teamList2 모두 정보 담아 리턴
                else if ("BATTLE".equals(gameInfoDto.getType())) {

                    List<TeamUserResponseDto> userTeamDtoList = teamUserService.findAllByTeamId(teamDtoList.get(0).getId());
                    recordDetailDto.setUserTeamList1(userTeamDtoList);

                    List<TeamUserResponseDto> userTeamDtoList2 = teamUserService.findAllByTeamId(teamDtoList.get(1).getId());
                    recordDetailDto.setUserTeamList2(userTeamDtoList2);

                    recordDetailList.add(recordDetailDto);
                }
                // 셋 다 아님. 여기까지 왔다는 건 뭔가 문제가 있다는 것...
                else {
                    throw new RecordException("something wrong...");
                }
            }

            return recordDetailList;
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RecordException("ERROR");
        }

    }

    @Transactional
    @Override
    public UserRecordInfoDto getUserRecordInfo(Long userId) throws RecordException {

        if (userId == null) {
            throw new RecordException("bad request");
        }

        try {
            UserRecordInfoDto userRecordInfoDto = new UserRecordInfoDto();

            // uid
            userRecordInfoDto.setUserId(userId);

            // 전체 맞춘 피스수
            int totalMatchedPieceCount = getTotalMatchedPieceCount(userId);
            userRecordInfoDto.setTotalMatchedPieceCount(totalMatchedPieceCount);

            // 전체 플레이 게임 횟수
            int playedGameCount = recordRepository.countByUserId(userId);
            userRecordInfoDto.setPlayedGameCount(playedGameCount);

            // 배틀게임 플레이 횟수
            int playedBattleGameCount = 0;
            // 이긴 배틀게임 수
            int battleGameWinCount = 0;

            List<Record> recordList = recordRepository.findByUserId(userId);
            for (Record record : recordList) {
                GameInfoDto gameInfoDto = gameInfoService.readGameInfo(record.getGameId());
                if ("BATTLE".equals(gameInfoDto.getType())) { // 배틀 게임이면
                    playedBattleGameCount++; // 횟수 카운트 ++

                    // 소속 팀 확인
                    if (didUserWin(userId, gameInfoDto.getId())) {
                        battleGameWinCount++;
                    }
                }
            }
            userRecordInfoDto.setPlayedBattleGameCount(playedBattleGameCount);
            userRecordInfoDto.setWinCount(battleGameWinCount);

            return userRecordInfoDto;
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RecordException("error occurred during find record info");
        }

    }

    @Override
    public List<PlayedGameCountRankingDto> rankPlayedGameCount() throws RecordException {
        List<PlayedGameCountRankingDto> playedGameCountRanking = new ArrayList<>();

        try {

            List<RankingQueryDto> queryDtoList = recordRepository.countGamesByUserId();
//            System.out.println(queryDtoList);
            for (RankingQueryDto qd : queryDtoList) {
                UserDto user = userService.getUserById(qd.getUserId());
                UserInfoDto userInfoDto = new UserInfoDto(qd.getUserId(), user.getEmail(), user.getNickname(), user.getImgPath(), user.getLocale(), user.getFamilyName(), user.getGivenName(), user.getPlayingGameID(), user.getOnlineStatus());
                playedGameCountRanking.add(new PlayedGameCountRankingDto(userInfoDto, (int) qd.getQueriedCount()));
            }

            return playedGameCountRanking;

        } catch (Exception e) {
//            e.printStackTrace();
            throw new RecordException("ERROR");
        }

        // 서비스 시작할 땐 플레이된 게임이 없을 테니 리스트가 null일 수도 있긴 함.. ^_^
    }

    @Override
    public List<WinCountRankingDto> rankSoloBattleWinCount() throws RecordException {
        try {
            List<WinCountRankingDto> soloBattleWinCountRanking = new ArrayList<>();

            List<UserInfoDto> userList = userService.getAllUsers();
            for (UserInfoDto user : userList) {
                int playedSoloBattleGameCount = 0;
                int soloBattleWinCount = 0;

                List<Record> recordList = recordRepository.findByUserId(user.getId());
                if (recordList == null || recordList.isEmpty()) { // 한 판도 플레이하지 않은 유저
                    // 랭킹에 포함되지 않음. 패스
                    continue;
                }

                for (Record record : recordList) {
                    GameInfoDto gameInfoDto = gameInfoService.readGameInfo(record.getGameId()); // 각 record에 대해 gid로 gameinfo 조회
                    if ("BATTLE".equals(gameInfoDto.getType()) && gameInfoDto.getMaxPlayerCount() == 2) { // 배틀 게임 + 2명 게임이면
                        playedSoloBattleGameCount++; // 1:1 배틀 게임 플레이 횟수 ++

                        if (didUserWin(user.getId(), gameInfoDto.getId())) { // 이 게임에서 이겼으면
                            soloBattleWinCount++; // 1:1 배틀 승리 횟수 ++
                        }
                    }
                }

                if (playedSoloBattleGameCount == 0) { // 1:1 배틀 게임 플레이한 적 없는 사용자
                    continue;
                }
                soloBattleWinCountRanking.add(new WinCountRankingDto(user, soloBattleWinCount, playedSoloBattleGameCount));
            }

            // sort by win count desc
            Collections.sort(soloBattleWinCountRanking);
            Collections.reverse(soloBattleWinCountRanking);

            return soloBattleWinCountRanking;
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RecordException("error occurred during rank solo battle win count");
        }

    }

    @Override
    public List<WinCountRankingDto> rankTeamBattleWinCount() throws RecordException {
        try {
            List<WinCountRankingDto> teamBattleWinCountRanking = new ArrayList<>();

            List<UserInfoDto> userList = userService.getAllUsers();
            System.out.println(userList);
            for (UserInfoDto user : userList) {
                int playedTeamBattleGameCount = 0;
                int teamBattleWinCount = 0;

                List<Record> recordList = recordRepository.findByUserId(user.getId());

                if (recordList == null || recordList.isEmpty()) { // 한 판도 플레이하지 않은 유저
                    // 랭킹에 포함되지 않음. 패스
                    continue;
                }

                for (Record record : recordList) {
                    GameInfoDto gameInfoDto = gameInfoService.readGameInfo(record.getGameId()); // 각 record에 대해 gid로 gameinfo 조회
                    if ("BATTLE".equals(gameInfoDto.getType()) && gameInfoDto.getMaxPlayerCount() >= 4) { // 배틀 게임 + 4명 이상 게임이면
                        playedTeamBattleGameCount++; // n:n 배틀 게임 플레이 횟수 ++

                        if (didUserWin(user.getId(), gameInfoDto.getId())) { // 이 게임에서 이겼으면
                            teamBattleWinCount++; // n:n 배틀 승리 횟수 ++
                        }
                    }
                }

                if (playedTeamBattleGameCount == 0) { // n:n 배틀 게임 플레이한 적 없는 사용자
                    continue;
                }
                teamBattleWinCountRanking.add(new WinCountRankingDto(user, teamBattleWinCount, playedTeamBattleGameCount));
            }

            // sort
            Collections.sort(teamBattleWinCountRanking);
            Collections.reverse(teamBattleWinCountRanking);

            return teamBattleWinCountRanking;
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RecordException("error occurred during rank battle game win count");
        }

    }

    @Override
    public List<WinningRateRankingDto> rankWinningRate() throws RecordException {
        try {

            List<WinningRateRankingDto> winningRateRanking = new ArrayList<>();

            // 필요한 값 : 전체 유저에 대해 전체 게임 플레이 횟수 / 전체 이긴 횟수
            // 그에 따라 백분율 승률 계산하기

            List<UserInfoDto> userList = userService.getAllUsers();
            for (UserInfoDto user : userList) {
                int playedBattleGameCount = 0;
                int battleWinCount = 0;

                List<Record> recordList = recordRepository.findByUserId(user.getId());
                if (recordList == null) { // 한 판도 플레이하지 않은 유저
                    // 랭킹에 포함되지 않음. 패스
                    continue;
                } else if (recordList.isEmpty()) {
                    continue;
                }

                for (Record record : recordList) {
                    GameInfoDto gameInfoDto = gameInfoService.readGameInfo(record.getGameId()); // 각 record에 대해 gid로 gameinfo 조회
                    if ("BATTLE".equals(gameInfoDto.getType())) { // 배틀 게임 + 2명 게임이면
                        playedBattleGameCount++; // 1:1 배틀 게임 플레이 횟수 ++

                        if (didUserWin(user.getId(), gameInfoDto.getId())) { // 이 게임에서 이겼으면
                            battleWinCount++; // 1:1 배틀 승리 횟수 ++
                        }
                    }
                }

                if (playedBattleGameCount == 0) { // 배틀게임 한 번도 플레이한 적 없는 사용자
                    continue;
                }

                double winningRate = (double) battleWinCount / playedBattleGameCount * 100;
                winningRateRanking.add(new WinningRateRankingDto(user, winningRate, battleWinCount, playedBattleGameCount));
            }

            // sort
            Collections.sort(winningRateRanking);
            Collections.reverse(winningRateRanking);

            return winningRateRanking;
        } catch (Exception e) {
            throw new RecordException("error occurred during rank winning rate");
        }
    }

    @Override
    public UserRankingDto getRankByUserId(Long userId) throws RecordException {
        try {
            UserRankingDto userRankingDto = new UserRankingDto();

            userRankingDto.setUserId(userId);

            int pgcRank = -1;
            List<PlayedGameCountRankingDto> pgcRanking = rankPlayedGameCount();
            for (int i = 0; i < pgcRanking.size(); ++i) {
                if (userId.equals(pgcRanking.get(i).getUser().getId())) {
                    pgcRank = i + 1;
                    break;
                }
            }
            userRankingDto.setPlayedGameCountRank(pgcRank);

            int sbwcRank = -1;
            List<WinCountRankingDto> sbwcRanking = rankSoloBattleWinCount();
            for (int i = 0; i < sbwcRanking.size(); ++i) {
                if (userId.equals(sbwcRanking.get(i).getUser().getId())) {
                    sbwcRank = i + 1;
                    break;
                }
            }
            userRankingDto.setSoloBattleWinCountRank(sbwcRank);

            int tbwcRank = -1;
            List<WinCountRankingDto> tbwcRanking = rankTeamBattleWinCount();
            for (int i = 0; i < tbwcRanking.size(); ++i) {
                if (userId.equals(tbwcRanking.get(i).getUser().getId())) {
                    tbwcRank = i + 1;
                    break;
                }
            }
            userRankingDto.setTeamBattleWinCountRank(tbwcRank);

            int wrRank = -1;
            List<WinningRateRankingDto> wrRanking = rankWinningRate();
            for (int i = 0; i < wrRanking.size(); ++i) {
                if (userId.equals(wrRanking.get(i).getUser().getId())) {
                    wrRank = i + 1;
                    break;
                }
            }
            userRankingDto.setWinningRateRank(wrRank);

            return userRankingDto;
        } catch (Exception e) {
            throw new RecordException("error occurred during get rank by id");
        }

    }


    ////////////////////////////////////////////////////////////////////////


    private int getTeamNumber(Long userId, Long gameId) { // 1: team1 / 2: team2 / 0: 이 게임을 플레이하지 않았음

        List<TeamDto> teamDtoList = teamService.findAllByGameId(gameId);

        // 레드 팀 멤버였는지 확인
        List<TeamUserResponseDto> userTeamDtoList = teamUserService.findAllByTeamId(teamDtoList.get(0).getId());
        for (TeamUserResponseDto utd : userTeamDtoList) {
            if (userId.equals(utd.getUser().getId())) {
                return 1;
            }
        }
        // 레드 팀 아니면 블루 팀도 확인
        userTeamDtoList = teamUserService.findAllByTeamId(teamDtoList.get(1).getId());
        for (TeamUserResponseDto utd : userTeamDtoList) {
            if (userId.equals(utd.getUser().getId())) {
                return 2;
            }
        }

        // 둘 다 아니면 이 게임 플레이하지 않은 유저인 것
        return 0;
    }

    private int getTotalMatchedPieceCount(Long userId) {
        int totalMatchedPieceCount = 0;
        List<TeamUserResponseDto> userTeamDtoList = teamUserService.findAllByUserId(userId);
        for (TeamUserResponseDto utd : userTeamDtoList) {
            totalMatchedPieceCount += utd.getMatchedPieceCount();
        }
        return totalMatchedPieceCount;
    }

    private boolean didUserWin(Long userId, Long gameId) throws RecordException {

        int teamNumber = getTeamNumber(userId, gameId); // 해당 게임에서 어떤 팀이었는지 확인
        List<TeamDto> teamDtoList = teamService.findAllByGameId(gameId); // 해당 게임의 두 팀 정보 불러오기

        if (teamNumber == 1) { // team1 소속
            return teamDtoList.get(0).getMatchedPieceCount() > teamDtoList.get(1).getMatchedPieceCount();
        } else if (teamNumber == 2) { // team2 소속
            return teamDtoList.get(0).getMatchedPieceCount() < teamDtoList.get(1).getMatchedPieceCount();
        } else { // 오류 상황
            throw new RecordException("BAD REQUEST");
        }
    }

}