package rubikscube;

import java.util.Arrays;
import java.util.HashMap;

public class CubeEstimate {

    // 【按老师要求】计算每个块需要多少步归位（位置 + 方向）
    public static int estimate(RubiksCube cube) {
        if (cube.isSolved()) {
            return 0;
        }

        int cornerSteps = estimateCornerSteps(cube);
        int edgeSteps = estimateEdgeSteps(cube);

        // 老师说：取总和除以8
        return (cornerSteps + edgeSteps) / 8;
    }

    // 计算所有角块需要的步数
    private static int estimateCornerSteps(RubiksCube cube) {
        int totalSteps = 0;
        String[][] c = cube.cube;

        // 8个角块，每个角块有3个面
        Corner[] corners = {
                new Corner(new int[][] { { 2, 3 }, { 3, 2 }, { 3, 3 } }, new String[] { "O", "G", "W" }), // 左前上
                new Corner(new int[][] { { 2, 5 }, { 3, 5 }, { 3, 6 } }, new String[] { "O", "W", "B" }), // 右前上
                new Corner(new int[][] { { 0, 3 }, { 3, 11 }, { 3, 0 } }, new String[] { "O", "Y", "G" }), // 左后上
                new Corner(new int[][] { { 0, 5 }, { 3, 8 }, { 3, 9 } }, new String[] { "O", "B", "Y" }), // 右后上
                new Corner(new int[][] { { 5, 2 }, { 5, 3 }, { 6, 3 } }, new String[] { "G", "W", "R" }), // 左前下
                new Corner(new int[][] { { 5, 5 }, { 5, 6 }, { 6, 5 } }, new String[] { "W", "B", "R" }), // 右前下
                new Corner(new int[][] { { 5, 11 }, { 5, 0 }, { 8, 3 } }, new String[] { "Y", "G", "R" }), // 左后下
                new Corner(new int[][] { { 5, 8 }, { 5, 9 }, { 8, 5 } }, new String[] { "B", "Y", "R" }) // 右后下
        };

        for (Corner corner : corners) {
            totalSteps += evaluateCorner(c, corner);
        }

        return totalSteps;
    }

    // 计算所有边块需要的步数
    private static int estimateEdgeSteps(RubiksCube cube) {
        int totalSteps = 0;
        String[][] c = cube.cube;

        // 12个边块，每个边块有2个面
        Edge[] edges = {
                new Edge(new int[][] { { 2, 4 }, { 3, 4 } }, new String[] { "O", "W" }), // 上前
                new Edge(new int[][] { { 1, 5 }, { 3, 7 } }, new String[] { "O", "B" }), // 上右
                new Edge(new int[][] { { 0, 4 }, { 3, 10 } }, new String[] { "O", "Y" }), // 上后
                new Edge(new int[][] { { 1, 3 }, { 3, 1 } }, new String[] { "O", "G" }), // 上左
                new Edge(new int[][] { { 4, 2 }, { 4, 3 } }, new String[] { "G", "W" }), // 左前
                new Edge(new int[][] { { 4, 5 }, { 4, 6 } }, new String[] { "W", "B" }), // 右前
                new Edge(new int[][] { { 4, 11 }, { 4, 0 } }, new String[] { "Y", "G" }), // 左后
                new Edge(new int[][] { { 4, 8 }, { 4, 9 } }, new String[] { "B", "Y" }), // 右后
                new Edge(new int[][] { { 5, 4 }, { 6, 4 } }, new String[] { "W", "R" }), // 下前
                new Edge(new int[][] { { 5, 7 }, { 7, 5 } }, new String[] { "B", "R" }), // 下右
                new Edge(new int[][] { { 5, 10 }, { 8, 4 } }, new String[] { "Y", "R" }), // 下后
                new Edge(new int[][] { { 5, 1 }, { 7, 3 } }, new String[] { "G", "R" }) // 下左
        };

        for (Edge edge : edges) {
            totalSteps += evaluateEdge(c, edge);
        }

        return totalSteps;
    }

    // 评估单个角块需要的步数
    private static int evaluateCorner(String[][] cube, Corner corner) {
        // 读取当前位置的3个颜色
        String[] currentColors = new String[3];
        for (int i = 0; i < 3; i++) {
            int row = corner.positions[i][0];
            int col = corner.positions[i][1];
            currentColors[i] = cube[row][col];
        }

        // 检查：位置正确吗？
        String[] sortedCurrent = Arrays.copyOf(currentColors, 3);
        String[] sortedTarget = Arrays.copyOf(corner.targetColors, 3);
        Arrays.sort(sortedCurrent);
        Arrays.sort(sortedTarget);

        if (!Arrays.equals(sortedCurrent, sortedTarget)) {
            // 位置不对：至少需要2步移动到正确位置
            return 5;
        }

        // 位置对了，检查方向
        if (Arrays.equals(currentColors, corner.targetColors)) {
            // 位置和方向都对
            return 0;
        } else {
            // 位置对但方向不对：需要3步调整方向（老师说的）
            return 3;
        }
    }

    // 评估单个边块需要的步数
    private static int evaluateEdge(String[][] cube, Edge edge) {
        // 读取当前位置的2个颜色
        String[] currentColors = new String[2];
        for (int i = 0; i < 2; i++) {
            int row = edge.positions[i][0];
            int col = edge.positions[i][1];
            currentColors[i] = cube[row][col];
        }

        // 检查：位置正确吗？
        String[] sortedCurrent = Arrays.copyOf(currentColors, 2);
        String[] sortedTarget = Arrays.copyOf(edge.targetColors, 2);
        Arrays.sort(sortedCurrent);
        Arrays.sort(sortedTarget);

        if (!Arrays.equals(sortedCurrent, sortedTarget)) {
            // 位置不对：至少需要1步移动到正确位置
            return 4;
        }

        // 位置对了，检查方向
        if (Arrays.equals(currentColors, edge.targetColors)) {
            // 位置和方向都对
            return 0;
        } else {
            // 位置对但方向翻转：需要3步调整方向
            return 3;
        }
    }

    // 辅助类：角块
    static class Corner {
        int[][] positions; // 3个位置 [row, col]
        String[] targetColors; // 目标颜色顺序

        Corner(int[][] positions, String[] targetColors) {
            this.positions = positions;
            this.targetColors = targetColors;
        }
    }

    // 辅助类：边块
    static class Edge {
        int[][] positions; // 2个位置 [row, col]
        String[] targetColors; // 目标颜色顺序

        Edge(int[][] positions, String[] targetColors) {
            this.positions = positions;
            this.targetColors = targetColors;
        }
    }
}