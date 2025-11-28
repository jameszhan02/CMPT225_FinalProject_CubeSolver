package rubikscube;

import java.util.HashMap;
import java.util.Map;

enum FaceType {
    F, B, R, L, U, D
}

public class Face {
    // face mapping index 3 element as a group 3 rows on face and 4 edges adjacent
    // to the face with order [top, right, bottom, left]
    final Map<FaceType, String[]> FACE_MAP = new HashMap<FaceType, String[]>() {
        {
            put(FaceType.F, new String[] { "3_3", "3_4", "3_5", "4_3", "4_4", "4_5", "5_3", "5_4", "5_5", "2_3", "2_4",
                    "2_5", "3_6", "4_6", "5_6", "6_5", "6_4", "6_3", "5_2", "4_2", "3_2" });
            put(FaceType.B, new String[] { "3_9", "3_10", "3_11", "4_9", "4_10", "4_11", "5_9", "5_10", "5_11", "0_5",
                    "0_4", "0_3", "3_0", "4_0", "5_0", "8_3", "8_4", "8_5", "5_8", "4_8", "3_8" });
            put(FaceType.R, new String[] { "3_6", "3_7", "3_8", "4_6", "4_7", "4_8", "5_6", "5_7", "5_8", "2_5", "1_5",
                    "0_5", "3_9", "4_9", "5_9", "8_5", "7_5", "6_5", "5_5", "4_5", "3_5" });
            put(FaceType.L, new String[] { "3_0", "3_1", "3_2", "4_0", "4_1", "4_2", "5_0", "5_1", "5_2", "0_3", "1_3",
                    "2_3", "3_3", "4_3", "5_3", "6_3", "7_3", "8_3", "5_11", "4_11", "3_11" });
            put(FaceType.U, new String[] { "0_3", "0_4", "0_5", "1_3", "1_4", "1_5", "2_3", "2_4", "2_5", "3_11",
                    "3_10", "3_9", "3_8", "3_7", "3_6", "3_5", "3_4", "3_3", "3_2", "3_1", "3_0" });
            put(FaceType.D, new String[] { "6_3", "6_4", "6_5", "7_3", "7_4", "7_5", "8_3", "8_4", "8_5", "5_3", "5_4",
                    "5_5", "5_6", "5_7", "5_8", "5_9", "5_10", "5_11", "5_0", "5_1", "5_2" });
        }
    };

    // 7 x 3 matrix 3 x 3 face and 4 x 3 edges
    String[][] face;
    FaceType faceType;

    Face(String[][] currentCube, FaceType faceType) {
        this.face = new String[7][3];
        this.faceType = faceType;
        String[] faceMap = FACE_MAP.get(faceType);
        int loadIndex = 0;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                String currentIndex = faceMap[loadIndex];
                this.face[i][j] = currentCube[Integer.parseInt(currentIndex.split("_")[0])][Integer
                        .parseInt(currentIndex.split("_")[1])];
                loadIndex++;
            }
        }
    }

    public void rotateFace() {
        String[][] oldFace = new String[7][3];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                oldFace[i][j] = this.face[i][j];
            }
        }
        this.face = new String[7][3];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                if (i < 3) { // inner face exchange
                    this.face[j][2 - i] = oldFace[i][j];
                } else { // edges exchange
                    if (i + 1 > 6) {
                        this.face[3][j] = oldFace[i][j];
                    } else {
                        this.face[i + 1][j] = oldFace[i][j];
                    }
                }
            }
        }
    }

    // modify currentCube with the new face according to the FACE_MAP
    public void modifyCube(String[][] currentCube) {
        int loadIndex = 0;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                String currentIndex = FACE_MAP.get(this.faceType)[loadIndex];
                loadIndex++;
                currentCube[Integer.parseInt(currentIndex.split("_")[0])][Integer
                        .parseInt(currentIndex.split("_")[1])] = this.face[i][j];
            }
        }
    }
}
