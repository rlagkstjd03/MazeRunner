package Server;

import maze_game.MakeMaze;

public class GameState {

    private final int W = 41;
    private final int H = 31;

    private int[][] maze;
    private int exitX, exitY;

    public void generateMaze() {
        MakeMaze mk = new MakeMaze(W, H, System.currentTimeMillis());
        maze = mk.make();
        exitX = mk.getExitX();
        exitY = mk.getExitY();
    }

    public int[][] getMaze() { return maze; }
    public int getExitX() { return exitX; }
    public int getExitY() { return exitY; }

    // 클라이언트에게 보낼 문자열로 변환
    public String mazeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MAZE|").append(W).append("|").append(H).append("|");

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                sb.append(maze[y][x]);
                if (!(x == W - 1 && y == H - 1)) sb.append(",");
            }
        }

        return sb.toString();
    }
}
