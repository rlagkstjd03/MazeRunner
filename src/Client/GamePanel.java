package Client;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private int[][] maze;        // 미로 데이터
    private int cellSize = 20;   // 셀 크기
    private int exitX = -1, exitY = -1;

    public GamePanel() {
        setBackground(Color.BLACK);
    }

    /** 미로 설정 */
    public void setMaze(int[][] maze) {
        this.maze = maze;

        if (maze != null) {
            int w = maze[0].length * cellSize;
            int h = maze.length * cellSize;
            setPreferredSize(new Dimension(w, h));
        }

        revalidate();
        repaint();
    }

    /** 출구 설정 */
    public void setExit(int x, int y) {
        this.exitX = x;
        this.exitY = y;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (maze == null) return;

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {

                if (maze[y][x] == 0) {
                    g.setColor(Color.DARK_GRAY); // 벽
                } else {
                    g.setColor(Color.WHITE);     // 길
                }

                g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }

        // ===== 입구 표시 (1,1) =====
        g.setColor(Color.BLUE);
        g.fillRect(1 * cellSize, 1 * cellSize, cellSize, cellSize);

        // ===== 출구 표시 =====
        if (exitX != -1 && exitY != -1) {
            g.setColor(Color.GREEN);
            g.fillRect(exitX * cellSize, exitY * cellSize, cellSize, cellSize);
        }
    }
}
