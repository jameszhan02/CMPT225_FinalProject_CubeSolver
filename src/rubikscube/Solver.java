package rubikscube;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Solver {
	static class State {
		RubiksCube cube;
		String solution;
		int g;
		int h;

		private int getDepth(String solution) {
			int depth = 0;
			if (solution == null || solution.isEmpty()) {
				return depth;
			}
			// remove the first |
			String[] solutionMoves = solution.substring(1).split("\\|");
			depth = solutionMoves.length;
			return depth;
		}

		State(RubiksCube cube, String solution) {
			this.cube = cube;
			this.solution = solution;
			// if 3 same move in a row, then it is 1 move
			this.g = getDepth(solution); // current depth

			// 使用启发函数 (包含PDB查询)
			this.h = CubeEstimate.estimate(cube);
		}

		int f() {
			return g + h;
		}
	}

	private static boolean isOppositeFace(char f1, char f2) {
		return (f1 == 'F' && f2 == 'B') || (f1 == 'B' && f2 == 'F') ||
				(f1 == 'L' && f2 == 'R') || (f1 == 'R' && f2 == 'L') ||
				(f1 == 'U' && f2 == 'D') || (f1 == 'D' && f2 == 'U');
	}

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		System.out.println("Initializing Pattern Database...");
		PatternDatabase.initialize();
		System.out.println("Pattern Database initialized");
		System.out.println("--------------------------------");
		if (args.length < 2) {
			System.out.println("File names are not specified");
			System.out.println(
					"usage: java " + MethodHandles.lookup().lookupClass().getName() + " input_file output_file");
			return;
		}
		String solution = "";
		Map<String, Integer> visited = new HashMap<>();
		// openSet is a priority queue to store the states to be explored and sorted by
		// the f(n) base on State class
		PriorityQueue<State> openSet = new PriorityQueue<>(
				Comparator.comparingInt(State::f) // sort order by f(n)
		);
		String inputFileName = args[0];
		String outputFileName = args[1];
		Integer steps = 0;
		try {
			RubiksCube cube = new RubiksCube(inputFileName);
			State initialState = new State(cube, "");
			// insert the initial state for start the search
			openSet.add(initialState);
			visited.put(cube.toString(), 0);
			while (!openSet.isEmpty()) {
				steps++;
				if (steps > 7000) {
					System.out.println("Steps limit reached");
					break;
				}

				State current = openSet.poll(); // get the state with the lowest f(n) and remove it from the openSet
				String currentStateStr = current.cube.toString();

				// 【优化】提前检查：如果这个状态已经被更短的路径访问过，跳过
				// 这避免了处理队列中的重复状态
				if (visited.containsKey(currentStateStr) &&
						visited.get(currentStateStr) < current.g) {
					continue;
				}

				if (steps % 100 == 0) {
					System.out.println("Steps: " + steps + " Queue size: " + openSet.size());
					System.out.println("Current solution: " + current.solution);
					System.out.println("Current g: " + current.g);
					System.out.println("Current h: " + current.h);
					System.out.println("Current f: " + current.f());
				}

				// 【优化】检查是否在 Pattern Database 中
				PatternDatabase.PDBEntry pdbEntry = PatternDatabase.lookup(current.cube);
				if (pdbEntry != null) {
					// 在 PDB 中找到！直接拼接路径
					if (pdbEntry.path.isEmpty()) {
						solution = current.solution; // 已经是 solved state
					} else {
						// 拼接 PDB 中的路径
						String[] pdbMoves = pdbEntry.path.split("\\|");
						StringBuilder sb = new StringBuilder(current.solution);
						for (String move : pdbMoves) {
							if (!move.isEmpty()) {
								sb.append("|").append(move);
							}
						}
						solution = sb.toString();
					}
					String formatedSolution = solution.replaceAll("\\|", "");
					System.out.println("Solution found: " + formatedSolution + " in " + steps + " steps");
					break;
				}

				if (current.cube.isSolved()) {
					solution = current.solution;
					// solution string without |
					String formatedSolution = solution.replaceAll("\\|", "");
					System.out.println("Solution found: " + formatedSolution + " in " + steps + " steps");
					break;
				}

				// 【关键优化】标记当前状态为已扩展，防止重复扩展
				visited.put(currentStateStr, current.g);
				// create 6 deep clone of the cube
				String[] moves = { "F", "B", "L", "R", "U", "D", "FF", "BB", "LL", "RR", "UU", "DD", "FFF", "LLL",
						"RRR", "UUU", "DDD", "BBB" };
				for (int i = 0; i < moves.length; i++) {
					// bypass 4 same move in a row
					// for example if previous move is F, then next move can not be FFF.
					// if previous move is FFF, then next move can not be F.
					// if previous move is FF, then next move can not be FF. etc.
					String previousMove = "";
					if (current.solution != null && !current.solution.isEmpty()) {
						String[] solutionMoves = current.solution.split("\\|");
						previousMove = solutionMoves[solutionMoves.length - 1];
					}
					// check all by pass cases
					if (!previousMove.isEmpty() && previousMove != null) {
						String currentTwoMoves = previousMove + moves[i];
						// if currentTwoMoves is same letter 4 times, then skip
						if (currentTwoMoves.length() >= 4) {
							char firstChar = currentTwoMoves.charAt(0);
							boolean allSame = true;
							for (int j = 0; j < 4; j++) {
								if (currentTwoMoves.charAt(currentTwoMoves.length() - 4 + j) != firstChar) {
									allSame = false;
									break;
								}
							}
							if (allSame) {
								continue;
							}
						}
						// 【剪枝2】避免对面来回操作 (F-B-F, F-BB-F, FF-B-F 等)
						char currentFace = moves[i].charAt(0);
						// 从路径末尾往前找第一个不同字母的位置
						int lastDifferentPos = -1;
						for (int j = previousMove.length() - 1; j >= 0; j--) {
							if (previousMove.charAt(j) != previousMove.charAt(previousMove.length() - 1)) {
								lastDifferentPos = j;
								break;
							}
						}
						// 如果找到了不同的字母，检查是否是对面
						if (lastDifferentPos >= 0) {
							char prevFace = previousMove.charAt(lastDifferentPos);
							char lastFace = previousMove.charAt(previousMove.length() - 1);
							// 检查模式: prevFace - lastFace - currentFace
							// 如果 prevFace 和 currentFace 相同，且 lastFace 是对面，则剪枝
							if (prevFace == currentFace && isOppositeFace(prevFace, lastFace)) {
								continue;
							}
						}
					}

					String newSolution = current.solution + "|" + moves[i];
					RubiksCube clone = current.cube.deepClone();
					clone.applyMoves(moves[i]);
					String nextState = clone.toString();
					int nextDepth = current.g + 1;
					// if the next state is not visited or the depth is less than the visited depth
					// then add the next state to the openSet
					if (!visited.containsKey(nextState) ||
							visited.get(nextState) > nextDepth) {
						visited.put(nextState, nextDepth);
						State nextStateObj = new State(clone, newSolution);
						openSet.add(nextStateObj);
					}
				}
			}
			System.out.println("Orginal Solution: " + solution);
		} catch (IOException e) {
			System.out.println("Error reading file");
			return;
		} catch (IncorrectFormatException e) {
			System.out.println("Error in file format");
			return;
		}
		// solve...
		// File output = new File(args[1]);
		System.out.println("Solving... -> " + outputFileName);
		long endTime = System.currentTimeMillis();
		// convert to seconds
		System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
	}
}
