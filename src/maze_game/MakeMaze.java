package maze_game;

import java.util.Random;

public class MakeMaze {

    private final int wid;
    private final int hei;
    private final int[][] maze;

    private int exitX;
    private int exitY;

    private final Random random;

    public MakeMaze(int wid, int hei, long seed) {
        this.wid = wid;
        this.hei = hei;
        this.maze = new int[hei][wid];
        this.random = new Random(seed);
    }

    public int[][] make() {

        // 1) 전체 벽으로 초기화
        for (int y = 0; y < hei; y++) {
            for (int x = 0; x < wid; x++) {
                maze[y][x] = 0;
            }
        }

        // 2) 입구 생성 고정
        maze[1][1] = 1;

        // 3) DFS 미로 생성
        carve(1, 1);

        // 4) 출구 생성 (오른쪽 끝에서 아래쪽 우선)
        for (int y = hei - 2; y >= 1; y--) {
            if (maze[y][wid - 2] == 1) {   // 내부가 길인지 확인
                maze[y][wid - 1] = 1;      // ★ 테두리 벽 하나 뚫기
                exitX = wid - 1;
                exitY = y;
                break;
            }
        }

        return maze;
    }

    // 깊이 우선 탐색(DFS)
    private void carve(int y, int x) {
        int[] dirs = {0, 1, 2, 3};
        shuffle(dirs);

        for (int d : dirs) {
            int nx = x;
            int ny = y;

            switch (d) {
                case 0: ny -= 2; break; // 위
                case 1: nx += 2; break; // 오른쪽
                case 2: ny += 2; break; // 아래
                case 3: nx -= 2; break; // 왼쪽
            }

            if (ny > 0 && ny < hei - 1 && nx > 0 && nx < wid - 1 && maze[ny][nx] == 0) {
                maze[(y + ny) / 2][(x + nx) / 2] = 1;
                maze[ny][nx] = 1;
                carve(ny, nx);
            }
        }
    }

    private void shuffle(int[] dirs) {
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = tmp;
        }
    }

    public int getExitX() { return exitX; }
    public int getExitY() { return exitY; }
}
