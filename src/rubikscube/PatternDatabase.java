package rubikscube;

import java.util.*;
import java.io.*;

public class PatternDatabase {

    private static Map<String, Integer> cornerPDB = null;

    /**
     * 【主入口】混合启发式：模式识别 + PDB + 传统估计
     */
    public static int estimateWithPDB(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        // 【优化1】先检查硬编码的常见模式（最快，0开销）
        int patternEstimate = checkKnownPatterns(cube);
        if (patternEstimate >= 0) {
            return patternEstimate;
        }

        // 【优化2】使用PDB查询（如果已初始化）
        int cornerEstimate = 0;
        int edgeEstimate = 0;

        if (cornerPDB != null && cornerPDB.size() > 0) {
            cornerEstimate = lookupCorner(cube);
            edgeEstimate = estimateEdges(cube);
        } else {
            // PDB未初始化，用传统方法
            cornerEstimate = CubeEstimate.estimate(cube) / 3;
            edgeEstimate = CubeEstimate.estimate(cube) / 2;
        }

        // 取最大值（保证可采纳）
        return Math.max(cornerEstimate, edgeEstimate);
    }

    // ========================================
    // 【优化1】硬编码模式识别
    // ========================================

    private static int checkKnownPatterns(RubiksCube cube) {
        String[][] c = cube.cube;

        // 模式1：只有上层十字错误
        if (isOnlyTopCrossWrong(c)) {
            return 4;
        }

        // 模式2：只有少量角块错位
        int wrongCorners = countWrongCorners(c);
        if (wrongCorners == 1) {
            return 2;
        }
        if (wrongCorners == 2) {
            return 3;
        }

        // 模式3：只有少量边块错位
        int wrongEdges = countWrongEdges(c);
        if (wrongEdges == 1) {
            return 1;
        }
        if (wrongEdges == 2) {
            return 2;
        }

        // 模式4：上层完成，只有下层错误
        if (isTopLayerSolved(c)) {
            int bottomErrors = countBottomErrors(c);
            return Math.max(1, bottomErrors / 3);
        }

        // 模式5：上层 + 中层完成
        if (isTopAndMiddleSolved(c)) {
            int bottomErrors = countBottomErrors(c);
            return Math.max(1, bottomErrors / 4);
        }

        return -1;
    }

    private static boolean isOnlyTopCrossWrong(String[][] c) {
        int topCrossErrors = 0;
        if (!c[0][4].equals("O"))
            topCrossErrors++;
        if (!c[1][3].equals("O"))
            topCrossErrors++;
        if (!c[1][5].equals("O"))
            topCrossErrors++;
        if (!c[2][4].equals("O"))
            topCrossErrors++;

        int otherErrors = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 12; j++) {
                if (c[i][j] == null)
                    continue;
                String target = getTargetColor(i, j);
                if (target == null)
                    continue;

                if ((i == 0 && j == 4) || (i == 1 && (j == 3 || j == 5)) || (i == 2 && j == 4)) {
                    continue;
                }

                if (!c[i][j].equals(target)) {
                    otherErrors++;
                }
            }
        }

        return topCrossErrors > 0 && otherErrors == 0;
    }

    private static int countWrongCorners(String[][] c) {
        int count = 0;
        int[][] corners = { { 0, 3 }, { 0, 5 }, { 2, 3 }, { 2, 5 }, { 6, 3 }, { 6, 5 }, { 8, 3 }, { 8, 5 } };

        for (int[] corner : corners) {
            String target = getTargetColor(corner[0], corner[1]);
            if (target != null && !c[corner[0]][corner[1]].equals(target)) {
                count++;
            }
        }

        return count;
    }

    private static int countWrongEdges(String[][] c) {
        int count = 0;
        int[][] edges = {
                { 0, 4 }, { 1, 3 }, { 1, 5 }, { 2, 4 },
                { 4, 3 }, { 4, 5 },
                { 6, 4 }, { 7, 3 }, { 7, 5 }, { 8, 4 }
        };

        for (int[] edge : edges) {
            String target = getTargetColor(edge[0], edge[1]);
            if (target != null && !c[edge[0]][edge[1]].equals(target)) {
                count++;
            }
        }

        return count;
    }

    private static boolean isTopLayerSolved(String[][] c) {
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                if (!c[i][j].equals("O")) {
                    return false;
                }
            }
        }

        for (int j = 0; j < 12; j++) {
            if (j >= 3 && j <= 5)
                continue;
            String target = getTargetColor(3, j);
            if (target != null && c[3][j] != null && !c[3][j].equals(target)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isTopAndMiddleSolved(String[][] c) {
        if (!isTopLayerSolved(c))
            return false;

        int[][] middlePositions = {
                { 4, 0 }, { 4, 2 }, { 4, 3 }, { 4, 5 }, { 4, 6 }, { 4, 8 }, { 4, 9 }, { 4, 11 }
        };

        for (int[] pos : middlePositions) {
            String target = getTargetColor(pos[0], pos[1]);
            if (target != null && !c[pos[0]][pos[1]].equals(target)) {
                return false;
            }
        }

        return true;
    }

    private static int countBottomErrors(String[][] c) {
        int errors = 0;
        for (int i = 6; i < 9; i++) {
            for (int j = 3; j < 6; j++) {
                if (!c[i][j].equals("R")) {
                    errors++;
                }
            }
        }

        for (int j = 0; j < 12; j++) {
            if (j >= 3 && j <= 5)
                continue;
            String target = getTargetColor(5, j);
            if (target != null && c[5][j] != null && !c[5][j].equals(target)) {
                errors++;
            }
        }

        return errors;
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
    // 【优化2】边块估计
    // ========================================

    private static int estimateEdges(RubiksCube cube) {
        String[][] c = cube.cube;
        int misplaced = 0;

        int[][] edges = {
                { 0, 4 }, { 1, 3 }, { 1, 5 }, { 2, 4 },
                { 4, 0 }, { 4, 2 }, { 4, 3 }, { 4, 5 },
                { 4, 6 }, { 4, 8 }, { 4, 9 }, { 4, 11 },
                { 6, 4 }, { 7, 3 }, { 7, 5 }, { 8, 4 }
        };

        for (int[] edge : edges) {
            String target = getTargetColor(edge[0], edge[1]);
            if (target != null && !c[edge[0]][edge[1]].equals(target)) {
                misplaced++;
            }
        }

        return misplaced / 4;
    }

    // ========================================
    // 【优化3】PDB相关
    // ========================================

    public static void initializeCornerPDB() {
        if (cornerPDB != null)
            return;

        System.out.println("Initializing corner PDB...");
        long startTime = System.currentTimeMillis();

        cornerPDB = new HashMap<>();
        RubiksCube solved = new RubiksCube();
        String solvedPattern = extractCornerPattern(solved);

        Queue<PDBState> queue = new LinkedList<>();
        queue.add(new PDBState(solved, 0));
        cornerPDB.put(solvedPattern, 0);

        String[] moves = { "F", "B", "L", "R", "U", "D" };
        int maxDepth = 6;

        while (!queue.isEmpty()) {
            PDBState current = queue.poll();

            if (current.depth >= maxDepth)
                continue;

            for (String move : moves) {
                RubiksCube next = current.cube.deepClone();
                next.applyMoves(move);
                String pattern = extractCornerPattern(next);

                if (!cornerPDB.containsKey(pattern)) {
                    cornerPDB.put(pattern, current.depth + 1);
                    queue.add(new PDBState(next, current.depth + 1));
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Corner PDB initialized: " + cornerPDB.size() +
                " patterns in " + (elapsed / 1000.0) + "s");
    }

    public static int lookupCorner(RubiksCube cube) {
        String pattern = extractCornerPattern(cube);

        if (cornerPDB.containsKey(pattern)) {
            return cornerPDB.get(pattern);
        } else {
            return CubeEstimate.estimate(cube) / 2;
        }
    }

    private static String extractCornerPattern(RubiksCube cube) {
        String[][] c = cube.cube;
        StringBuilder pattern = new StringBuilder();

        int[][] cornerPositions = {
                { 2, 3 }, { 2, 5 }, { 0, 3 }, { 0, 5 },
                { 6, 3 }, { 6, 5 }, { 8, 3 }, { 8, 5 }
        };

        for (int[] pos : cornerPositions) {
            if (c[pos[0]][pos[1]] != null) {
                pattern.append(c[pos[0]][pos[1]]);
            }
        }

        return pattern.toString();
    }

    // ========================================
    // 保存/加载 PDB
    // ========================================

    public static void savePDB(String filename) throws IOException {
        if (cornerPDB == null)
            return;

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(cornerPDB);
        }
        System.out.println("PDB saved to " + filename);
    }

    @SuppressWarnings("unchecked")
    public static void loadPDB(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            cornerPDB = (Map<String, Integer>) ois.readObject();
        }
        System.out.println("PDB loaded: " + cornerPDB.size() + " patterns");
    }

    // 内部辅助类
    private static class PDBState {
        RubiksCube cube;
        int depth;

        PDBState(RubiksCube cube, int depth) {
            this.cube = cube;
            this.depth = depth;
        }
    }
}