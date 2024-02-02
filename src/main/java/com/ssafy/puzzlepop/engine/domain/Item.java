package com.ssafy.puzzlepop.engine.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class Item {
    private Long id;
    private ItemType name;
    private String description;
    private String img_path;

    public Item (ItemType name) {
        this.name = name;
        if (name == ItemType.HINT) {
            this.id = 1L;
        } else if (name == ItemType.EARTHQUAKE) {
            this.id = 2L;
        } else if (name == ItemType.MIRROR) {
            this.id = 3L;
        } else if (name == ItemType.FRAME) {
            this.id = 4L;
        } else if (name == ItemType.SHIELD) {
            this.id = 5L;
        } else if (name == ItemType.MAGNET) {
            this.id = 6L;
        } else if (name == ItemType.ROCKET) {
            this.id = 7L;
        } else if (name == ItemType.FIRE) {
            this.id = 8L;
        }
    }

    public void run(PuzzleBoard puzzle) {
        List<Set<Piece>> bundles;
        Set<Piece> mostManyBundle;
        List<Integer> targets;

        switch (this.id.intValue()) {
            case 1:
                System.out.println(puzzle + "에 힌트 아이템 효과 발동~");
                boolean[][] tmp = puzzle.getIsCorrected();
                for (int i = 0; i < puzzle.getLengthCnt(); i++) {
                    for (int j = 0; j < puzzle.getWidthCnt()-1; j++) {
                        if (!tmp[i][j] && !tmp[i][j+1]) {
                            System.out.println(puzzle.getBoard()[0][i][j] + " " + puzzle.getBoard()[0][i][j+1]);
                            return;
                        }
                    }
                }
                break;

            case 2:
                puzzle.randomArrange();
                break;

            case 3:
                //게임 내에서 구현
                break;

            //액자
            case 4:
                List<Integer> list = new LinkedList<>();
                for (int i = 0; i < puzzle.getLengthCnt(); i++) {
                    if (!puzzle.getIsCorrected()[i][0]) {
                        list.add(puzzle.getBoard()[0][i][0].getIndex());
                    }
                }

                for (int i = 0; i < puzzle.getWidthCnt(); i++) {
                    if (!puzzle.getIsCorrected()[0][i]) {
                        list.add(puzzle.getBoard()[0][0][i].getIndex());
                    }
                }

                for (int i = 0; i < puzzle.getLengthCnt(); i++) {
                    if (!puzzle.getIsCorrected()[i][puzzle.getWidthCnt()-1]) {
                        list.add(puzzle.getBoard()[0][i][puzzle.getWidthCnt()-1].getIndex());
                    }
                }

                for (int i = 0; i < puzzle.getWidthCnt(); i++) {
                    if (!puzzle.getIsCorrected()[puzzle.getLengthCnt()-1][i]) {
                        list.add(puzzle.getBoard()[0][puzzle.getLengthCnt()-1][i].getIndex());
                    }
                }

                System.out.println("액자 효과 대상 : " + list);
                puzzle.addPiece(list);
                break;

            case 5:
                //게임 내에서 구현
                break;

            //자석
            case 6:
                targets = new LinkedList<>();
                for (int i = 0; i < puzzle.getLengthCnt(); i++) {
                    for (int j = 0; j < puzzle.getWidthCnt(); j++) {
                        if (!puzzle.getIsCorrected()[i][j]) {
                            targets.add(puzzle.getBoard()[0][i][j].getIndex());

                            int bottom = puzzle.getBoard()[0][i][j].getCorrectBottomIndex();
                            if (bottom != -1) {
                                int[] coor = puzzle.getIdxToCoordinate().get(bottom);
                                if (!puzzle.getIsCorrected()[coor[0]][coor[1]]) {
                                    targets.add(bottom);
                                }
                            }

                            int top = puzzle.getBoard()[0][i][j].getCorrectTopIndex();
                            if (top != -1) {
                                int[] coor = puzzle.getIdxToCoordinate().get(top);
                                if (!puzzle.getIsCorrected()[coor[0]][coor[1]]) {
                                    targets.add(top);
                                }
                            }

                            int left = puzzle.getBoard()[0][i][j].getCorrectLeftIndex();
                            if (left != -1) {
                                int[] coor = puzzle.getIdxToCoordinate().get(left);
                                if (!puzzle.getIsCorrected()[coor[0]][coor[1]]) {
                                    targets.add(left);
                                }
                            }

                            int right = puzzle.getBoard()[0][i][j].getCorrectRightIndex();
                            if (right != -1) {
                                int[] coor = puzzle.getIdxToCoordinate().get(right);
                                if (!puzzle.getIsCorrected()[coor[0]][coor[1]]) {
                                    targets.add(right);
                                }
                            }

                            System.out.println("자석 효과 대상 : " + targets);
                            puzzle.addPiece(targets);
                            return;
                        }
                    }
                }

                break;

            //로켓
            case 7:
                if (puzzle.getBundles().isEmpty())
                    return;
                bundles = puzzle.getBundles();
                Collections.sort(bundles, (o1, o2) -> {
                    return o2.size() - o1.size();
                });
                mostManyBundle = bundles.get(0);

                targets = new LinkedList<>();
                for (Piece p : mostManyBundle) {
                    targets.add(p.getIndex());
                }
                List<Integer> deletedPieces = new LinkedList<>();
                for (int t : targets) {
                    deletedPieces.add(t);
                    puzzle.deletePiece(t);
                }

                System.out.println("로켓 대상 : " + deletedPieces);

                break;

            //불지르기
            case 8:
                if (puzzle.getBundles().isEmpty())
                    return;

                //조각 개수가 많은 덩어리 순으로 정렬해서 그 덩어리 해체하기
                bundles = puzzle.getBundles();
                Collections.sort(bundles, (o1, o2) -> {
                    return o2.size() - o1.size();
                });
                mostManyBundle = bundles.get(0);
                int size = mostManyBundle.size();

                //위에서 뽑은 덩어리에서 조각 하나 뽑기
                int randomIdx = puzzle.random(size-1);
                List<Integer> setToList = new LinkedList<>();
                for (Piece p : mostManyBundle) {
                    setToList.add(p.getIndex());
                }
                Piece target = puzzle.getBoard()[0][puzzle.getIdxToCoordinate().get(setToList.get(randomIdx))[0]][puzzle.getIdxToCoordinate().get(setToList.get(randomIdx))[1]];

                List<Integer> targetsList = new LinkedList<>();
                targetsList.add(target.getIndex());

                if (setToList.contains(target.getCorrectBottomIndex())) {
                    targetsList.add(target.getCorrectBottomIndex());
                }

                if (setToList.contains(target.getCorrectTopIndex())) {
                    targetsList.add(target.getCorrectTopIndex());
                }

                if (setToList.contains(target.getCorrectLeftIndex())) {
                    targetsList.add(target.getCorrectLeftIndex());
                }

                if (setToList.contains(target.getCorrectRightIndex())) {
                    targetsList.add(target.getCorrectRightIndex());
                }

                for (int targetIdx : targetsList) {
                    puzzle.deletePiece(targetIdx);
                }

                System.out.println("불 지르기 효과 대상 : " + targetsList);

                puzzle.searchForGroupDisbandment();

                break;
        }
    }
}
