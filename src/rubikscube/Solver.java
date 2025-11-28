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
			for (int i = 0; i < solution.length(); i++) {
				// if i + 1 and i + 2 out of range, then it is 1 move
				if (i + 1 >= solution.length() || i + 2 >= solution.length()) {
					depth++;
					continue;
				}
				if (solution.charAt(i) == solution.charAt(i + 1) && solution.charAt(i) == solution.charAt(i + 2)) {
					depth++;
					i += 2;
				} else {
					depth++;
				}
			}
			return depth;
		}

		State(RubiksCube cube, String solution) {
			this.cube = cube;
			this.solution = solution;
			// if 3 same move in a row, then it is 1 move
			this.g = getDepth(solution); // current depth
			this.h = CubeEstimate.estimate(cube); // heuristic estimate
		}

		int f() {
			return g + h;
		}
	}

	public static void main(String[] args) {
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
			System.out.println("Reading file: " + inputFileName);
			RubiksCube cube = new RubiksCube(inputFileName);
			State initialState = new State(cube, "");
			// insert the initial state for start the search
			openSet.add(initialState);
			visited.put(cube.toString(), 0);
			while (!openSet.isEmpty()) {
				steps++;
				if (steps > 25000) {
					// accroding to the assignment, all cube could be solved in 20 steps | but since
					// we only allow one direction move , 500 still will be far more than enough
					System.out.println("Steps limit reached");
					break;
				}
				State current = openSet.poll(); // get the state with the lowest f(n) and remove it from the openSet
				if (steps % 100 == 0) {
					System.out.println("Steps: " + steps + " Queue size: " + openSet.size());
					System.out.println("Current solution: " + current.solution);
					System.out.println("Current g: " + current.g);
					System.out.println("Current h: " + current.h);
					System.out.println("Current f: " + current.f());
				}
				if (current.cube.isSolved()) {
					solution = current.solution;
					System.out.println("Solution found: " + solution + " in " + steps + " steps");
					break;
				}

				String currentStateStr = current.cube.toString();
				// current state is already visited and the solution is shorter than the current
				// solution then skip
				if (visited.containsKey(currentStateStr) &&
						visited.get(currentStateStr) < current.g) {
					continue;
				}
				// create 6 deep clone of the cube
				String[] moves = { "F", "B", "L", "R", "U", "D", "FF", "BB", "LL", "RR", "UU", "DD", "FFF", "LLL",
						"RRR", "UUU", "DDD", "BBB" };
				for (int i = 0; i < moves.length; i++) {
					// bypass 4 same move in a row
					String newSolution = current.solution + moves[i];
					String fourSameMove = String.valueOf(moves[i].charAt(0)) + String.valueOf(moves[i].charAt(0))
							+ String.valueOf(moves[i].charAt(0))
							+ moves[i].charAt(0);
					if (newSolution.length() > 4 && newSolution.substring(newSolution.length() - 4)
							.equals(fourSameMove)) {
						continue;
					}
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
			System.out.println("Solution: " + solution);
		} catch (

		IOException e) {
			System.out.println("Error reading file");
			return;
		} catch (IncorrectFormatException e) {
			System.out.println("Error in file format");
			return;
		}
		// solve...
		// File output = new File(args[1]);
		System.out.println("Solving... -> " + outputFileName);
	}
}
