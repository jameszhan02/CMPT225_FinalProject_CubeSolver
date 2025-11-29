package rubikscube;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CubeEstimate - 负责所有启发函数的计算
 * 提供多种启发函数估计方法，用于A*搜索
 */
public class CubeEstimate {

    /**
     * 主启发函数 - 综合使用多种估计方法
     * 1. 优先检查Pattern Database（如果在4层内）
     * 2. 使用增强的角块和边块评估
     */
    public static int estimate(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        // 1. 首先检查Pattern Database
        int pdbEstimate = estimateWithPDB(cube);
        if (pdbEstimate >= 0) {
            return pdbEstimate;
        }

        // 2. 使用增强的启发函数
        return estimateEnhanced(cube);
    }

    /**
     * 使用Pattern Database的估计
     * 如果状态在PDB中，直接返回准确步数
     */
    private static int estimateWithPDB(RubiksCube cube) {
        PatternDatabase.PDBEntry entry = PatternDatabase.lookup(cube);
        if (entry != null) {
            return entry.depth;
        }
        return -1; // 不在数据库中
    }

    /**
     * 增强的启发函数 - 综合角块和边块评估
     */
    public static int estimateEnhanced(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        // 先检查已知模式
        int patternEstimate = checkKnownPatterns(cube);
        if (patternEstimate >= 0) {
            return patternEstimate;
        }

        int cornerEstimate = estimateCorners(cube);
        int edgeEstimate = estimateEdges(cube);

        // 角块和边块是独立的，取最大值
        int baseEstimate = Math.max(cornerEstimate, edgeEstimate);

        // 如果两者都很高，说明魔方很乱，需要额外步骤
        if (cornerEstimate >= 8 && edgeEstimate >= 8) {
            baseEstimate += Math.min(cornerEstimate, edgeEstimate) / 3;
        } else if (cornerEstimate >= 5 && edgeEstimate >= 5) {
            baseEstimate += Math.min(cornerEstimate, edgeEstimate) / 4;
        }

        return baseEstimate;
    }

    /**
     * 简单启发函数 - 按老师要求：总和除以8
     */
    public static int estimateSimple(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        int cornerSteps = estimateCornerStepsSimple(cube);
        int edgeSteps = estimateEdgeStepsSimple(cube);

        return (cornerSteps + edgeSteps) / 8;
    }

    // ========================================
    // 增强的角块评估
    // ========================================

    private static int estimateCorners(RubiksCube cube) {
        String[][] c = cube.cube;
        int totalSteps = 0;
        int misplacedCount = 0;
        int twistedCount = 0;

        CornerDef[] corners = {
                new CornerDef(new int[][] { { 2, 3 }, { 3, 2 }, { 3, 3 } }, new String[] { "O", "G", "W" }),
                new CornerDef(new int[][] { { 2, 5 }, { 3, 5 }, { 3, 6 } }, new String[] { "O", "W", "B" }),
                new CornerDef(new int[][] { { 0, 3 }, { 3, 11 }, { 3, 0 } }, new String[] { "O", "Y", "G" }),
                new CornerDef(new int[][] { { 0, 5 }, { 3, 8 }, { 3, 9 } }, new String[] { "O", "B", "Y" }),
                new CornerDef(new int[][] { { 6, 3 }, { 5, 2 }, { 5, 3 } }, new String[] { "R", "G", "W" }),
                new CornerDef(new int[][] { { 6, 5 }, { 5, 5 }, { 5, 6 } }, new String[] { "R", "W", "B" }),
                new CornerDef(new int[][] { { 8, 3 }, { 5, 11 }, { 5, 0 } }, new String[] { "R", "Y", "G" }),
                new CornerDef(new int[][] { { 8, 5 }, { 5, 8 }, { 5, 9 } }, new String[] { "R", "B", "Y" })
        };

        for (CornerDef corner : corners) {
            int score = evaluateCorner(c, corner);
            totalSteps += score;

            if (score >= 4) {
                misplacedCount++;
            } else if (score > 0) {
                twistedCount++;
            }
        }

        int baseEstimate = totalSteps / 4;

        if (misplacedCount > 0) {
            baseEstimate += misplacedCount;
        }

        if (twistedCount >= 4) {
            baseEstimate += 2;
        } else if (twistedCount >= 2) {
            baseEstimate += 1;
        }

        return baseEstimate;
    }

    private static int evaluateCorner(String[][] c, CornerDef corner) {
        String c0 = c[corner.pos[0][0]][corner.pos[0][1]];
        String c1 = c[corner.pos[1][0]][corner.pos[1][1]];
        String c2 = c[corner.pos[2][0]][corner.pos[2][1]];

        Set<String> currentColors = new HashSet<>(Arrays.asList(c0, c1, c2));
        Set<String> targetColors = new HashSet<>(Arrays.asList(corner.colors));

        if (!currentColors.equals(targetColors)) {
            return 2; // 位置错误
        }

        if (c0.equals(corner.colors[0]) &&
                c1.equals(corner.colors[1]) &&
                c2.equals(corner.colors[2])) {
            return 0; // 完全正确
        }

        // 位置正确但旋转错误
        return 2;
    }

    // ========================================
    // 增强的边块评估
    // ========================================

    private static int estimateEdges(RubiksCube cube) {
        String[][] c = cube.cube;
        int totalSteps = 0;
        int misplacedCount = 0;
        int flippedCount = 0;

        EdgeDef[] edges = {
                new EdgeDef(new int[][] { { 2, 4 }, { 3, 4 } }, new String[] { "O", "W" }),
                new EdgeDef(new int[][] { { 1, 5 }, { 3, 7 } }, new String[] { "O", "B" }),
                new EdgeDef(new int[][] { { 0, 4 }, { 3, 10 } }, new String[] { "O", "Y" }),
                new EdgeDef(new int[][] { { 1, 3 }, { 3, 1 } }, new String[] { "O", "G" }),
                new EdgeDef(new int[][] { { 4, 2 }, { 4, 3 } }, new String[] { "G", "W" }),
                new EdgeDef(new int[][] { { 4, 5 }, { 4, 6 } }, new String[] { "W", "B" }),
                new EdgeDef(new int[][] { { 4, 11 }, { 4, 0 } }, new String[] { "Y", "G" }),
                new EdgeDef(new int[][] { { 4, 8 }, { 4, 9 } }, new String[] { "B", "Y" }),
                new EdgeDef(new int[][] { { 5, 4 }, { 6, 4 } }, new String[] { "W", "R" }),
                new EdgeDef(new int[][] { { 5, 7 }, { 7, 5 } }, new String[] { "B", "R" }),
                new EdgeDef(new int[][] { { 5, 10 }, { 8, 4 } }, new String[] { "Y", "R" }),
                new EdgeDef(new int[][] { { 5, 1 }, { 7, 3 } }, new String[] { "G", "R" })
        };

        for (EdgeDef edge : edges) {
            int score = evaluateEdge(c, edge);
            totalSteps += score;

            if (score >= 4) {
                misplacedCount++;
            } else if (score == 2) {
                flippedCount++;
            }
        }

        int baseEstimate = totalSteps / 4;

        if (misplacedCount > 0) {
            baseEstimate += misplacedCount / 2;
        }

        if (flippedCount >= 4) {
            baseEstimate += 2;
        } else if (flippedCount >= 2) {
            baseEstimate += 1;
        }

        return baseEstimate;
    }

    private static int evaluateEdge(String[][] c, EdgeDef edge) {
        String c0 = c[edge.pos[0][0]][edge.pos[0][1]];
        String c1 = c[edge.pos[1][0]][edge.pos[1][1]];

        Set<String> currentColors = new HashSet<>(Arrays.asList(c0, c1));
        Set<String> targetColors = new HashSet<>(Arrays.asList(edge.colors));

        if (!currentColors.equals(targetColors)) {
            return 2; // 位置错误
        }

        if (c0.equals(edge.colors[0]) && c1.equals(edge.colors[1])) {
            return 0; // 完全正确
        }

        if (c0.equals(edge.colors[1]) && c1.equals(edge.colors[0])) {
            return 1; // 翻转
        }

        return 3;
    }

    // ========================================
    // 简单的角块和边块评估（按老师要求）
    // ========================================

    private static int estimateCornerStepsSimple(RubiksCube cube) {
        int totalSteps = 0;
        String[][] c = cube.cube;

        Corner[] corners = {
                new Corner(new int[][] { { 2, 3 }, { 3, 2 }, { 3, 3 } }, new String[] { "O", "G", "W" }),
                new Corner(new int[][] { { 2, 5 }, { 3, 5 }, { 3, 6 } }, new String[] { "O", "W", "B" }),
                new Corner(new int[][] { { 0, 3 }, { 3, 11 }, { 3, 0 } }, new String[] { "O", "Y", "G" }),
                new Corner(new int[][] { { 0, 5 }, { 3, 8 }, { 3, 9 } }, new String[] { "O", "B", "Y" }),
                new Corner(new int[][] { { 5, 2 }, { 5, 3 }, { 6, 3 } }, new String[] { "G", "W", "R" }),
                new Corner(new int[][] { { 5, 5 }, { 5, 6 }, { 6, 5 } }, new String[] { "W", "B", "R" }),
                new Corner(new int[][] { { 5, 11 }, { 5, 0 }, { 8, 3 } }, new String[] { "Y", "G", "R" }),
                new Corner(new int[][] { { 5, 8 }, { 5, 9 }, { 8, 5 } }, new String[] { "B", "Y", "R" })
        };

        for (Corner corner : corners) {
            totalSteps += evaluateCornerSimple(c, corner);
        }

        return totalSteps;
    }

    private static int estimateEdgeStepsSimple(RubiksCube cube) {
        int totalSteps = 0;
        String[][] c = cube.cube;

        Edge[] edges = {
                new Edge(new int[][] { { 2, 4 }, { 3, 4 } }, new String[] { "O", "W" }),
                new Edge(new int[][] { { 1, 5 }, { 3, 7 } }, new String[] { "O", "B" }),
                new Edge(new int[][] { { 0, 4 }, { 3, 10 } }, new String[] { "O", "Y" }),
                new Edge(new int[][] { { 1, 3 }, { 3, 1 } }, new String[] { "O", "G" }),
                new Edge(new int[][] { { 4, 2 }, { 4, 3 } }, new String[] { "G", "W" }),
                new Edge(new int[][] { { 4, 5 }, { 4, 6 } }, new String[] { "W", "B" }),
                new Edge(new int[][] { { 4, 11 }, { 4, 0 } }, new String[] { "Y", "G" }),
                new Edge(new int[][] { { 4, 8 }, { 4, 9 } }, new String[] { "B", "Y" }),
                new Edge(new int[][] { { 5, 4 }, { 6, 4 } }, new String[] { "W", "R" }),
                new Edge(new int[][] { { 5, 7 }, { 7, 5 } }, new String[] { "B", "R" }),
                new Edge(new int[][] { { 5, 10 }, { 8, 4 } }, new String[] { "Y", "R" }),
                new Edge(new int[][] { { 5, 1 }, { 7, 3 } }, new String[] { "G", "R" })
        };

        for (Edge edge : edges) {
            totalSteps += evaluateEdgeSimple(c, edge);
        }

        return totalSteps;
    }

    private static int evaluateCornerSimple(String[][] cube, Corner corner) {
        String[] currentColors = new String[3];
        for (int i = 0; i < 3; i++) {
            int row = corner.positions[i][0];
            int col = corner.positions[i][1];
            currentColors[i] = cube[row][col];
        }

        String[] sortedCurrent = Arrays.copyOf(currentColors, 3);
        String[] sortedTarget = Arrays.copyOf(corner.targetColors, 3);
        Arrays.sort(sortedCurrent);
        Arrays.sort(sortedTarget);

        if (!Arrays.equals(sortedCurrent, sortedTarget)) {
            return 5; // 位置不对
        }

        if (Arrays.equals(currentColors, corner.targetColors)) {
            return 0; // 完全正确
        } else {
            return 3; // 方向不对
        }
    }

    private static int evaluateEdgeSimple(String[][] cube, Edge edge) {
        String[] currentColors = new String[2];
        for (int i = 0; i < 2; i++) {
            int row = edge.positions[i][0];
            int col = edge.positions[i][1];
            currentColors[i] = cube[row][col];
        }

        String[] sortedCurrent = Arrays.copyOf(currentColors, 2);
        String[] sortedTarget = Arrays.copyOf(edge.targetColors, 2);
        Arrays.sort(sortedCurrent);
        Arrays.sort(sortedTarget);

        if (!Arrays.equals(sortedCurrent, sortedTarget)) {
            return 4; // 位置不对
        }

        if (Arrays.equals(currentColors, edge.targetColors)) {
            return 0; // 完全正确
        } else {
            return 3; // 方向翻转
        }
    }

    // ========================================
    // 模式识别
    // ========================================

    private static int checkKnownPatterns(RubiksCube cube) {
        String[][] c = cube.cube;

        int totalWrong = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 12; j++) {
                if (c[i][j] == null)
                    continue;
                String target = getTargetColor(i, j);
                if (target != null && !c[i][j].equals(target)) {
                    totalWrong++;
                }
            }
        }

        if (totalWrong == 0)
            return 0;
        if (totalWrong <= 3)
            return 1;
        if (totalWrong <= 6)
            return 2;
        if (totalWrong <= 9)
            return 3;

        // 检查顶层是否完成
        boolean topDone = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                if (!c[i][j].equals("O")) {
                    topDone = false;
                    break;
                }
            }
        }

        if (topDone) {
            int bottomWrong = 0;
            for (int i = 6; i < 9; i++) {
                for (int j = 3; j < 6; j++) {
                    if (!c[i][j].equals("R"))
                        bottomWrong++;
                }
            }
            return Math.max(1, bottomWrong / 3);
        }

        return -1;
    }

    private static String getTargetColor(int row, int col) {
        if (row >= 0 && row <= 2 && col >= 3 && col <= 5)
            return "O";
        if (row >= 3 && row <= 5 && col >= 3 && col <= 5)
            return "W";
        if (row >= 6 && row <= 8 && col >= 3 && col <= 5)
            return "R";
        if (row >= 3 && row <= 5 && col >= 0 && col <= 2)
            return "G";
        if (row >= 3 && row <= 5 && col >= 6 && col <= 8)
            return "B";
        if (row >= 3 && row <= 5 && col >= 9 && col <= 11)
            return "Y";
        return null;
    }

    // ========================================
    // 辅助类
    // ========================================

    private static class CornerDef {
        int[][] pos;
        String[] colors;

        CornerDef(int[][] pos, String[] colors) {
            this.pos = pos;
            this.colors = colors;
        }
    }

    private static class EdgeDef {
        int[][] pos;
        String[] colors;

        EdgeDef(int[][] pos, String[] colors) {
            this.pos = pos;
            this.colors = colors;
        }
    }

    static class Corner {
        int[][] positions;
        String[] targetColors;

        Corner(int[][] positions, String[] targetColors) {
            this.positions = positions;
            this.targetColors = targetColors;
        }
    }

    static class Edge {
        int[][] positions;
        String[] targetColors;

        Edge(int[][] positions, String[] targetColors) {
            this.positions = positions;
            this.targetColors = targetColors;
        }
    }
}