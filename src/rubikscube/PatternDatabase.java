package rubikscube;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * PatternDatabase - 负责生成和查询4层深度的打乱状态数据库
 * 用途：在求解时如果遇到数据库中的状态，可以直接倒着拼接答案
 */
public class PatternDatabase {

    // 存储魔方状态到最小步数的映射
    private static Map<String, PDBEntry> stateDatabase = null;

    // 18种移动（包括90°, 180°, 270°）
    private static final String[] ALL_MOVES = {
            "F", "B", "L", "R", "U", "D", // 90度
            "FF", "BB", "LL", "RR", "UU", "DD", // 180度
            "FFF", "BBB", "LLL", "RRR", "UUU", "DDD" // 270度
    };

    /**
     * 初始化Pattern Database（4层深度）
     */
    public static void initialize() {
        if (stateDatabase != null) {
            return;
        }

        System.out.println("Initializing Pattern Database (4 layers)...");
        long start = System.currentTimeMillis();

        stateDatabase = new HashMap<>();
        RubiksCube solved = new RubiksCube();
        String solvedState = solved.toString();

        Queue<PDBState> queue = new LinkedList<>();
        queue.add(new PDBState(solved, 0, ""));
        stateDatabase.put(solvedState, new PDBEntry(0, ""));

        int maxDepth = 4; // 4层深度

        while (!queue.isEmpty()) {
            PDBState curr = queue.poll();
            if (curr.depth >= maxDepth) {
                continue;
            }

            // 尝试所有18种移动
            for (String move : ALL_MOVES) {
                // 剪枝：避免无意义的重复移动
                if (shouldPruneMove(curr.path, move)) {
                    continue;
                }

                RubiksCube next = curr.cube.deepClone();
                next.applyMoves(move);
                String nextState = next.toString();
                String newPath = curr.path.isEmpty() ? move : curr.path + "|" + move;

                // 如果这个状态还没有被访问过，添加到数据库
                if (!stateDatabase.containsKey(nextState)) {
                    stateDatabase.put(nextState, new PDBEntry(curr.depth + 1, newPath));
                    queue.add(new PDBState(next, curr.depth + 1, newPath));
                }
            }
        }

        System.out.println("Pattern Database initialized: " + stateDatabase.size() + " states in " +
                (System.currentTimeMillis() - start) / 1000.0 + "s");
    }

    /**
     * 查询魔方状态是否在数据库中
     * 
     * @return 如果找到，返回PDBEntry（包含步数和路径），否则返回null
     */
    public static PDBEntry lookup(RubiksCube cube) {
        if (stateDatabase == null) {
            return null;
        }
        String state = cube.toString();
        return stateDatabase.get(state);
    }

    /**
     * 获取数据库中的最小步数（如果存在）
     * 
     * @return 如果找到返回步数，否则返回-1
     */
    public static int getDepth(RubiksCube cube) {
        PDBEntry entry = lookup(cube);
        return entry != null ? entry.depth : -1;
    }

    /**
     * 获取数据库中的路径（如果存在）
     * 
     * @return 如果找到返回路径字符串，否则返回null
     */
    public static String getPath(RubiksCube cube) {
        PDBEntry entry = lookup(cube);
        return entry != null ? entry.path : null;
    }

    /**
     * 检查移动是否应该被剪枝
     */
    private static boolean shouldPruneMove(String previousPath, String nextMove) {
        if (previousPath == null || previousPath.isEmpty()) {
            return false;
        }

        // 从路径中提取最后一个移动
        String lastMove = previousPath;
        int lastSeparator = previousPath.lastIndexOf('|');
        if (lastSeparator >= 0) {
            lastMove = previousPath.substring(lastSeparator + 1);
        }

        char nextFace = nextMove.charAt(0);
        char lastFace = lastMove.charAt(0);

        // 剪枝1: 避免连续4次相同面的操作 (如 F + FFF)
        int consecutiveCount = lastMove.length(); // F=1, FF=2, FFF=3
        if (lastFace == nextFace) {
            int nextMoveCount = nextMove.length();
            if (consecutiveCount + nextMoveCount >= 4) {
                return true;
            }
        }

        // 剪枝2: 避免对面来回操作 (如 F B F)
        if (isOppositeFace(lastFace, nextFace)) {
            // 检查倒数第二个移动
            int secondLastSeparator = previousPath.lastIndexOf('|', lastSeparator - 1);
            if (secondLastSeparator >= 0) {
                String secondLastMove = previousPath.substring(secondLastSeparator + 1, lastSeparator);
                char secondLastFace = secondLastMove.charAt(0);
                if (secondLastFace == nextFace) {
                    return true; // 发现 A-B-A 模式
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

    /**
     * 保存数据库到文件
     */
    public static void save(String filename) throws IOException {
        if (stateDatabase != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(stateDatabase);
            }
            System.out.println("Pattern Database saved to " + filename);
        }
    }

    /**
     * 从文件加载数据库
     */
    @SuppressWarnings("unchecked")
    public static void load(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            stateDatabase = (Map<String, PDBEntry>) ois.readObject();
            System.out.println("Pattern Database loaded: " + stateDatabase.size() + " states");
        }
    }

    /**
     * 数据库条目：存储深度和路径
     */
    public static class PDBEntry implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final int depth;
        public final String path;

        public PDBEntry(int depth, String path) {
            this.depth = depth;
            this.path = path;
        }
    }

    /**
     * BFS状态节点
     */
    private static class PDBState {
        RubiksCube cube;
        int depth;
        String path;

        PDBState(RubiksCube cube, int depth, String path) {
            this.cube = cube;
            this.depth = depth;
            this.path = path;
        }
    }
}