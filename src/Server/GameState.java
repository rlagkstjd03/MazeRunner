package Server;

import maze_game.MakeMaze;

public class GameState {

    private final int W = 41;
    private final int H = 31;

    public int[][] maze;
    public int exitX, exitY;

    public int p1x, p1y;
    public int p2x, p2y;

    public void generateMaze() {
        MakeMaze mk = new MakeMaze(W, H, System.currentTimeMillis());
        maze = mk.make();
        exitX = mk.getExitX();
        exitY = mk.getExitY();

        p1x = 1; p1y = 1;
        p2x = 1; p2y = 1;
    }

    public void movePlayer1(String dir) { move(dir, true); }
    public void movePlayer2(String dir) { move(dir, false); }

    private void move(String dir, boolean isP1) {
        int x = isP1 ? p1x : p2x;
        int y = isP1 ? p1y : p2y;

        int nx = x, ny = y;
        switch (dir) {
            case "UP":    ny--; break;
            case "DOWN":  ny++; break;
            case "LEFT":  nx--; break;
            case "RIGHT": nx++; break;
        }

        if (maze[ny][nx] == 1) {
            if (isP1) { p1x = nx; p1y = ny; }
            else { p2x = nx; p2y = ny; }
        }
    }

    // ★★★ 여기 중요 ★★★
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
