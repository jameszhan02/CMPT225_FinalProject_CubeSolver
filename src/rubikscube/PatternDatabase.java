package rubikscube;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PatternDatabase {

    private static Map<String, Integer> cornerPDB = null;
    private static Map<String, Integer> edgePDB = null;

    // 【优化】18种移动（包括90°, 180°, 270°）
    private static final String[] ALL_MOVES = {
            "F", "B", "L", "R", "U", "D", // 90度
            "FF", "BB", "LL", "RR", "UU", "DD", // 180度
            "FFF", "BBB", "LLL", "RRR", "UUU", "DDD" // 270度
    };

    public static int estimateWithPDB(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        int patternEstimate = checkKnownPatterns(cube);
        if (patternEstimate >= 0) {
            return patternEstimate;
        }

        int cornerEstimate = (cornerPDB != null && cornerPDB.size() > 0)
                ? lookupCorner(cube)
                : estimateCorners(cube);

        int edgeEstimate = (edgePDB != null && edgePDB.size() > 0)
                ? lookupEdge(cube)
                : estimateEdges(cube);

        // 【关键改进】不要过度除法！使用更激进的估计
        // 角块和边块是独立的，应该取最大值而不是平均
        int baseEstimate = Math.max(cornerEstimate, edgeEstimate);

        // 如果两者都很高，说明魔方很乱，需要额外步骤
        if (cornerEstimate >= 8 && edgeEstimate >= 8) {
            baseEstimate += Math.min(cornerEstimate, edgeEstimate) / 3;
        } else if (cornerEstimate >= 5 && edgeEstimate >= 5) {
            baseEstimate += Math.min(cornerEstimate, edgeEstimate) / 4;
        }

        return baseEstimate;
    }

    // ========================================
    // 【核心】精确评估角块
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
                misplacedCount++; // 位置错误的角块
            } else if (score > 0) {
                twistedCount++; // 只是旋转错误的角块
            }
        }

        // 【关键】不要除以4！直接使用totalSteps
        // 因为魔方的复杂度是叠加的，不是平均的
        int baseEstimate = totalSteps / 2; // 只除以2，保留更多信息

        // 额外惩罚：位置错误比旋转错误更严重
        if (misplacedCount > 0) {
            baseEstimate += misplacedCount; // 每个错位的角块+1步
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

        // 情况1: 颜色集合不匹配 - 角块在错误的位置
        if (!currentColors.equals(targetColors)) {
            // 进一步细分：检查有多少颜色是匹配的
            int matchingColors = 0;
            for (String color : targetColors) {
                if (currentColors.contains(color)) {
                    matchingColors++;
                }
            }

            // 0个匹配: 角块完全在错误的位置，需要更多步骤
            if (matchingColors == 0) {
                return 6; // 完全错误
            }
            // 1个匹配: 部分相关
            else if (matchingColors == 1) {
                return 5;
            }
            // 2个匹配: 很接近但位置错误
            else {
                return 4;
            }
        }

        // 情况2: 颜色集合匹配，检查位置和旋转
        if (c0.equals(corner.colors[0]) &&
                c1.equals(corner.colors[1]) &&
                c2.equals(corner.colors[2])) {
            return 0; // 完全正确
        }

        // 情况3: 位置正确但旋转错误
        // 检查旋转程度
        int rotationDistance = getCornerRotationDistance(
                new String[] { c0, c1, c2 },
                corner.colors);

        // 旋转120度 (顺时针一次)
        if (rotationDistance == 1) {
            return 2;
        }
        // 旋转240度 (顺时针两次)
        else if (rotationDistance == 2) {
            return 2;
        }

        return 3; // 其他情况
    }

    private static int getCornerRotationDistance(String[] current, String[] target) {
        // 尝试旋转0次、1次、2次，看哪个匹配
        for (int rotations = 0; rotations < 3; rotations++) {
            boolean matches = true;
            for (int i = 0; i < 3; i++) {
                if (!current[(i + rotations) % 3].equals(target[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return rotations;
            }
        }
        return 3; // 不应该到达这里
    }
    // ========================================
    // 【核心】精确评估边块
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
                misplacedCount++; // 位置错误的边块
            } else if (score == 2) {
                flippedCount++; // 只是翻转的边块
            }
        }

        // 【关键】不要除以4！直接使用totalSteps
        int baseEstimate = totalSteps / 2; // 只除以2

        // 额外惩罚
        if (misplacedCount > 0) {
            baseEstimate += misplacedCount / 2; // 错位的边块
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

        // 情况1: 颜色集合不匹配 - 边块在错误的位置
        if (!currentColors.equals(targetColors)) {
            // 检查有多少颜色匹配
            int matchingColors = 0;
            for (String color : targetColors) {
                if (currentColors.contains(color)) {
                    matchingColors++;
                }
            }

            // 0个匹配: 边块完全在错误的位置
            if (matchingColors == 0) {
                return 5;
            }
            // 1个匹配: 有一个颜色对了
            else {
                return 4;
            }
        }

        // 情况2: 颜色集合匹配，检查是否正确放置
        if (c0.equals(edge.colors[0]) && c1.equals(edge.colors[1])) {
            return 0; // 完全正确
        }

        // 情况3: 位置正确但翻转了
        if (c0.equals(edge.colors[1]) && c1.equals(edge.colors[0])) {
            return 2; // 只需要翻转
        }

        return 3; // 其他情况（理论上不应该到达）
    }

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

    // ========================================
    // 【优化】模式识别
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
    // 【优化】PDB生成 - 带剪枝
    // ========================================

    /**
     * 检查移动是否应该被剪枝
     */
    private static boolean shouldPruneMove(String previousPath, String nextMove) {
        if (previousPath == null || previousPath.isEmpty()) {
            return false;
        }

        // 获取基本面（去掉重复字母）
        char nextFace = nextMove.charAt(0);

        // 检查路径末尾连续相同字母的数量
        int consecutiveCount = 0;
        for (int i = previousPath.length() - 1; i >= 0; i--) {
            if (previousPath.charAt(i) == nextFace) {
                consecutiveCount++;
            } else {
                break;
            }
        }

        // 计算如果添加这个移动会有多少个连续相同字母
        int nextMoveCount = nextMove.length(); // F=1, FF=2, FFF=3
        int totalConsecutive = consecutiveCount + nextMoveCount;

        // 如果会产生4个或更多连续相同字母，剪枝
        if (totalConsecutive >= 4) {
            return true;
        }

        // 【额外剪枝】避免对面来回操作 (如 F B F B)
        if (previousPath.length() >= 2) {
            char lastFace = previousPath.charAt(previousPath.length() - 1);
            if (isOppositeFace(lastFace, nextFace)) {
                // 检查倒数第二个字符
                for (int i = previousPath.length() - 2; i >= 0; i--) {
                    if (previousPath.charAt(i) == nextFace) {
                        return true; // 发现对面来回，剪枝
                    } else if (previousPath.charAt(i) != lastFace) {
                        break;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isOppositeFace(char f1, char f2) {
        return (f1 == 'F' && f2 == 'B') || (f1 == 'B' && f2 == 'F') ||
                (f1 == 'L' && f2 == 'R') || (f1 == 'R' && f2 == 'L') ||
                (f1 == 'U' && f2 == 'D') || (f1 == 'D' && f2 == 'U');
    }

    public static void initializeCornerPDB() {
        if (cornerPDB != null)
            return;

        System.out.println("Initializing corner PDB with pruning...");
        long start = System.currentTimeMillis();

        cornerPDB = new HashMap<>();
        RubiksCube solved = new RubiksCube();
        String pattern = extractCornerPattern(solved);

        Queue<PDBState> queue = new LinkedList<>();
        queue.add(new PDBState(solved, 0, ""));
        cornerPDB.put(pattern, 0);

        int maxDepth = 5;

        while (!queue.isEmpty()) {
            PDBState curr = queue.poll();
            if (curr.depth >= maxDepth)
                continue;

            // 【优化】使用18种移动 + 剪枝
            for (String move : ALL_MOVES) {
                // 剪枝检查
                if (shouldPruneMove(curr.path, move)) {
                    continue;
                }

                RubiksCube next = curr.cube.deepClone();
                next.applyMoves(move);
                String p = extractCornerPattern(next);

                if (!cornerPDB.containsKey(p)) {
                    cornerPDB.put(p, curr.depth + 1);
                    queue.add(new PDBState(next, curr.depth + 1, curr.path + move));
                }
            }
        }

        System.out.println("Corner PDB: " + cornerPDB.size() + " patterns in " +
                (System.currentTimeMillis() - start) / 1000.0 + "s");
    }

    public static void initializeEdgePDB() {
        if (edgePDB != null)
            return;

        System.out.println("Initializing edge PDB with pruning...");
        long start = System.currentTimeMillis();

        edgePDB = new HashMap<>();
        RubiksCube solved = new RubiksCube();
        String pattern = extractEdgePattern(solved);

        Queue<PDBState> queue = new LinkedList<>();
        queue.add(new PDBState(solved, 0, ""));
        edgePDB.put(pattern, 0);

        int maxDepth = 5;

        while (!queue.isEmpty()) {
            PDBState curr = queue.poll();
            if (curr.depth >= maxDepth)
                continue;

            // 【优化】使用18种移动 + 剪枝
            for (String move : ALL_MOVES) {
                if (shouldPruneMove(curr.path, move)) {
                    continue;
                }

                RubiksCube next = curr.cube.deepClone();
                next.applyMoves(move);
                String p = extractEdgePattern(next);

                if (!edgePDB.containsKey(p)) {
                    edgePDB.put(p, curr.depth + 1);
                    queue.add(new PDBState(next, curr.depth + 1, curr.path + move));
                }
            }
        }

        System.out.println("Edge PDB: " + edgePDB.size() + " patterns in " +
                (System.currentTimeMillis() - start) / 1000.0 + "s");
    }

    public static void initializeBothPDB() {
        initializeCornerPDB();
        initializeEdgePDB();
    }

    public static int lookupCorner(RubiksCube cube) {
        String pattern = extractCornerPattern(cube);
        return cornerPDB.getOrDefault(pattern, estimateCorners(cube));
    }

    public static int lookupEdge(RubiksCube cube) {
        String pattern = extractEdgePattern(cube);
        return edgePDB.getOrDefault(pattern, estimateEdges(cube));
    }

    private static String extractCornerPattern(RubiksCube cube) {
        String[][] c = cube.cube;
        StringBuilder sb = new StringBuilder();

        int[][] positions = {
                { 2, 3 }, { 2, 5 }, { 0, 3 }, { 0, 5 },
                { 6, 3 }, { 6, 5 }, { 8, 3 }, { 8, 5 }
        };

        for (int[] pos : positions) {
            sb.append(c[pos[0]][pos[1]]);
        }

        return sb.toString();
    }

    private static String extractEdgePattern(RubiksCube cube) {
        String[][] c = cube.cube;
        StringBuilder sb = new StringBuilder();

        int[][] positions = {
                { 2, 4 }, { 1, 5 }, { 0, 4 }, { 1, 3 },
                { 4, 2 }, { 4, 5 }, { 5, 4 }, { 7, 5 }
        };

        for (int[] pos : positions) {
            sb.append(c[pos[0]][pos[1]]);
        }

        return sb.toString();
    }

    public static void savePDB(String cornerFile, String edgeFile) throws IOException {
        if (cornerPDB != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cornerFile))) {
                oos.writeObject(cornerPDB);
            }
            System.out.println("Corner PDB saved to " + cornerFile);
        }
        if (edgePDB != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(edgeFile))) {
                oos.writeObject(edgePDB);
            }
            System.out.println("Edge PDB saved to " + edgeFile);
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadPDB(String cornerFile, String edgeFile)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cornerFile))) {
            cornerPDB = (Map<String, Integer>) ois.readObject();
            System.out.println("Corner PDB loaded: " + cornerPDB.size());
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(edgeFile))) {
            edgePDB = (Map<String, Integer>) ois.readObject();
            System.out.println("Edge PDB loaded: " + edgePDB.size());
        }
    }

    private static class PDBState {
        RubiksCube cube;
        int depth;
        String path; // 【新增】记录路径用于剪枝

        PDBState(RubiksCube cube, int depth, String path) {
            this.cube = cube;
            this.depth = depth;
            this.path = path;
        }
    }
}