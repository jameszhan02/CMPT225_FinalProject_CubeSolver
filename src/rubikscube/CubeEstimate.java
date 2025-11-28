package rubikscube;

import java.util.Arrays;
import java.util.HashMap;

// 【推荐】分阶段启发式函数
public class CubeEstimate {
    public static String sortString(String string) {
        char[] charArray = string.toCharArray();
        Arrays.sort(charArray);
        String sortedString = new String(charArray);
        return sortedString;
    }

    final static String[] conerElementColors = new String[] {
            "O_G_W",
            "O_W_B",
            "G_W_R",
            "W_B_R",
            "O_B_Y",
            "B_Y_R",
            "O_Y_G",
            "Y_G_R"
    };
    final static String[][] conerElementPositions = new String[][] {
            { "2_3", "3_2", "3_3", },
            { "2_5", "3_5", "3_6" },
            { "5_2", "5_3", "6_3" },
            { "5_5", "5_6", "6_5" },
            { "0_5", "3_8", "3_9" },
            { "5_8", "5_9", "8_5" },
            { "0_3", "3_11", "3_0" },
            { "5_11", "5_0", "8_3" }
    };

    final static String[] edgeElementColors = new String[] {
            "O_W",
            "W_R",
            "G_W",
            "W_B",
            "O_B",
            "B_R",
            "B_Y",
            "O_Y",
            "Y_G",
            "Y_R",
            "O_G",
            "G_R"
    };
    final static String[][] edgeElementPositions = new String[][] {
            { "2_4", "3_4" },
            { "5_4", "6_4" },
            { "4_2", "4_3" },
            { "4_5", "4_6" },
            { "1_5", "3_7" },
            { "5_7", "7_5" },
            { "4_8", "4_9" },
            { "0_4", "3_10" },
            { "4_11", "4_0" },
            { "5_10", "8_4" },
            { "1_3", "3_1" },
            { "5_1", "7_3" }

    };

    public static int estimate(RubiksCube cube) {
        // manhattan distance for each element in the cube.
        // coner elemnt adjust 3 color each of this should be unique so we should be
        // able to identify
        // this is a map with conerElementColors as key and the position of the element
        // as value
        // orginalposition is position of the element in conerElementPositions each
        // element has 3 positions

        int conerScore = 0;
        int edgeScore = 0;
        HashMap<String, String[]> conerElementMap = detectConerElement(cube);
        // HashMap<String, String[]> edgeElementMap = detectEdgeElement(cube);
        for (int i = 0; i < conerElementColors.length; i++) {
            String key = conerElementColors[i];
            String[] currentPosition = conerElementMap.get(key);
            String[] orginalPosition = conerElementPositions[i];
            for (int j = 0; j < currentPosition.length; j++) {
                conerScore += manhattanDistance(currentPosition[j], orginalPosition[j], "coner");
            }
        }
        HashMap<String, String[]> edgeElementMap = detectEdgeElement(cube);
        for (int i = 0; i < edgeElementColors.length; i++) {
            String key = edgeElementColors[i];
            String[] currentPosition = edgeElementMap.get(key);
            String[] orginalPosition = edgeElementPositions[i];
            for (int j = 0; j < currentPosition.length; j++) {
                edgeScore += manhattanDistance(currentPosition[j], orginalPosition[j], "edge");
            }
        }
        return Math.max(conerScore, edgeScore);
    }

    private static int manhattanDistance(String position, String orginalPosition, String type) {
        int row = Integer.parseInt(position.split("_")[0]);
        int column = Integer.parseInt(position.split("_")[1]);
        int orginalRow = Integer.parseInt(orginalPosition.split("_")[0]);
        int orginalColumn = Integer.parseInt(orginalPosition.split("_")[1]);
        int manhattanDistance = Math.abs(row - orginalRow) + Math.abs(column - orginalColumn);
        if (type == "coner") {
            return manhattanDistance / 3;
        } else {
            return manhattanDistance / 2;
        }
    }

    // detect each coner element position and color in current cube from
    // conerElementColors
    // return a map of position align with conerElementColors
    private static HashMap<String, String[]> detectConerElement(RubiksCube cube) {
        // HashMap<String, String> conerElementMap = new HashMap<String, String>();
        // fetch all coner element align with position
        HashMap<String, String[]> conerElementMap = new HashMap<String, String[]>();
        String[] cubeStringRows = cube.toString().split("\n");
        for (int i = 0; i < conerElementPositions.length; i++) {
            String[] position = conerElementPositions[i];
            String conerColorCombination = "";
            HashMap<String, String> conerElementMapBuffer = new HashMap<String, String>();
            for (int j = 0; j < position.length; j++) {
                String pos = position[j];
                int row = Integer.parseInt(pos.split("_")[0]);
                int column = Integer.parseInt(pos.split("_")[1]);
                String color = String.valueOf(cubeStringRows[row].charAt(column));
                conerElementMapBuffer.put(color, pos);
                conerColorCombination += color;
            }
            // check if current coner match any conerElementColors regredless of order
            for (String conerColor : conerElementColors) {
                // sort string alphabetically
                String colorStringSorted = sortString(conerColor.replaceAll("_", ""));
                String conerColorCombinationSorted = sortString(conerColorCombination);
                if (colorStringSorted.equals(conerColorCombinationSorted)) {
                    String[] conerColorIndexes = conerColor.split("_");
                    String[] correctColorMapCurrentPosition = { conerElementMapBuffer.get(conerColorIndexes[0]),
                            conerElementMapBuffer.get(conerColorIndexes[1]),
                            conerElementMapBuffer.get(conerColorIndexes[2]) };
                    conerElementMap.put(conerColor, correctColorMapCurrentPosition);
                    // clean conerElementMapBuffer
                    conerElementMapBuffer.clear();
                    conerColorCombination = "";
                    break;
                }
            }
        }
        return conerElementMap;
    }

    private static HashMap<String, String[]> detectEdgeElement(RubiksCube cube) {
        // HashMap<String, String> conerElementMap = new HashMap<String, String>();
        // fetch all coner element align with position
        HashMap<String, String[]> edgeElementMap = new HashMap<String, String[]>();
        String[] cubeStringRows = cube.toString().split("\n");
        for (int i = 0; i < edgeElementPositions.length; i++) {
            String[] position = edgeElementPositions[i];
            String edgeColorCombination = "";
            HashMap<String, String> edgeElementMapBuffer = new HashMap<String, String>();
            for (int j = 0; j < position.length; j++) {
                String pos = position[j];
                int row = Integer.parseInt(pos.split("_")[0]);
                int column = Integer.parseInt(pos.split("_")[1]);
                String color = String.valueOf(cubeStringRows[row].charAt(column));
                edgeElementMapBuffer.put(color, pos);
                edgeColorCombination += color;
            }
            // check if current coner match any conerElementColors regredless of order
            for (String edgeColor : edgeElementColors) {
                // sort string alphabetically
                String colorStringSorted = sortString(edgeColor.replaceAll("_", ""));
                String edgeColorCombinationSorted = sortString(edgeColorCombination);
                if (colorStringSorted.equals(edgeColorCombinationSorted)) {
                    String[] edgeColorIndexes = edgeColor.split("_");
                    String[] correctColorMapCurrentPosition = { edgeElementMapBuffer.get(edgeColorIndexes[0]),
                            edgeElementMapBuffer.get(edgeColorIndexes[1]) };
                    edgeElementMap.put(edgeColor, correctColorMapCurrentPosition);
                    // clean conerElementMapBuffer
                    edgeElementMapBuffer.clear();
                    edgeColorCombination = "";
                    break;
                }
            }
        }
        return edgeElementMap;
    }
}

// ================

// public class CubeEstimate {
// public static int estimate(RubiksCube cube) {
// // 8 is form research
// //
// https://stackoverflow.com/questions/60130124/heuristic-function-for-rubiks-cube-in-a-algorithm-artificial-intelligence
// //
// [https://www.cs.princeton.edu/courses/archive/fall06/cos402/papers/korfrubik.pdf]
// if (cube.isSolved()) {
// return 0;
// }
// int misplacedCorners = countMisplacedCorners(cube);
// int misplacedEdges = countMisplacedEdges(cube);
// int misplacedCenters = countMisplacedCenters(cube);
// return (misplacedCorners * 2 + misplacedEdges * 3 + misplacedCenters) / 3;
// }

// private static int countMisplacedCenters(RubiksCube cube) {
// int count = 0;
// // check the center of the face is the correct element
// String[] centerElements = new String[] { "1_5", "4_1", "4_5", "4_8", "4_11",
// "7_5" };
// for (String centerElement : centerElements) {
// int row = Integer.parseInt(centerElement.split("_")[0]);
// int column = Integer.parseInt(centerElement.split("_")[1]);
// if (!cube.cube[row][column].equals(RubiksCube.solvedCube.cube[row][column]))
// {
// count++;
// }
// }
// return count;
// }

// private static int countMisplacedCorners(RubiksCube cube) {
// int count = 0;
// for (int i = 0; i < cube.cube.length; i++) {
// for (int j = 0; j < cube.cube[i].length; j++) {
// if (cube.cube[i][j] == null || cube.cube[i][j].isEmpty()) {
// continue;
// }
// if (!cube.cube[i][j].equals(RubiksCube.solvedCube.cube[i][j])) {
// count++;
// }
// }
// }
// return count;
// }

// private static int countMisplacedEdges(RubiksCube cube) {
// int count = 0;
// // only count the centers that are not in the correct position
// for (int i = 0; i < 3; i++) {
// for (int j = 3; j < 6; j++) {
// if (!cube.cube[i][j].equals(RubiksCube.solvedCube.cube[i][j])) {
// count++;
// }
// }
// }
// return count;
// }
// }

// package rubikscube;

// public class CubeEstimate {
// private static final int[][] CENTERS = {
// { 1, 5 }, { 4, 1 }, { 4, 5 }, { 4, 8 }, { 4, 11 }, { 7, 5 }
// };

// private static final int[][] CORNERS = {
// { 3, 3 }, { 3, 5 }, { 3, 9 }, { 3, 11 },
// { 5, 3 }, { 5, 5 }, { 5, 9 }, { 5, 11 },
// };

// private static final int[][] EDGES = {
// { 0, 3 }, { 2, 5 }, { 3, 0 }, { 5, 2 }, { 3, 3 }, { 5, 5 }, { 3, 6 }, { 5, 8
// }, { 3, 9 }, { 5, 11 },
// { 6, 3 }, { 8, 5 }
// };

// public static int estimate(RubiksCube cube) {
// if (cube.isSolved())
// return 0;

// int hCenter = countMisplaced(cube, CENTERS) * 1;
// int hEdge = countMisplaced(cube, EDGES) * 2;
// int hCorner = countMisplaced(cube, CORNERS) * 3;
// int h = (hCorner + hEdge + hCenter) / 3;
// return h;
// }

// private static int countMisplaced(RubiksCube cube, int[][] positions) {
// int count = 0;
// for (int[] pos : positions) {
// int r = pos[0];
// int c = pos[1];
// String cur = cube.cube[r][c];
// String solved = RubiksCube.solvedCube.cube[r][c];
// if (!cur.equals(solved)) {
// count++;
// }
// }
// return count;
// }
// }