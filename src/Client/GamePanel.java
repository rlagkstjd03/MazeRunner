package Client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {

    private int[][] maze;            // 미로 데이터
    private int cellSize = 24;       // 타일 크기
    private int exitX = -1, exitY = -1;
    private int playerX = 1;
    private int playerY = 1;

    private BufferedImage wallTile;   // 벽 타일만 사용
    private BufferedImage flag;
    private BufferedImage player_1;
    public GamePanel() {
        setBackground(new Color(0xF7EEDB));  // 부드러운 베이지


        try {
            // 벽으로 사용할 타일만 로드 (floor.png)
            wallTile = ImageIO.read(getClass().getResource("/Client/floor.png"));
            flag = ImageIO.read(getClass().getResource("/Client/flag.png"));
            player_1 = ImageIO.read(getClass().getResource("/Client/player1_r.png"));

            System.out.println("wallTile = " + wallTile);
        } catch (Exception e) {
            System.out.println("이미지 로딩 실패: " + e);
        }
    }

    /* 미로 설정 */
    public void setMaze(int[][] maze) {
        this.maze = maze;

        if (maze != null) {
            int w = maze[0].length * cellSize;
            int h = maze.length * cellSize;
            setPreferredSize(new Dimension(w, h));  // 스크롤 지원
        }

        revalidate();
        repaint();
    }

    /* 출구 설정 */
    public void setExit(int x, int y) {
        this.exitX = x;
        this.exitY = y;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (maze == null) return;

        Graphics2D g2 = (Graphics2D) g;

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {

                if (maze[y][x] == 0) {
                    //벽을 floor.png로 그리기
                    g2.drawImage(wallTile,
                            x * cellSize, y * cellSize, cellSize, cellSize, null);
                }
                else {
                    // 길(빈칸)은 아무것도 안 그림
                }
            }
        }
        // 출구 표시
        if (exitX != -1 && exitY != -1) {
            g2.drawImage(flag, exitX * cellSize, exitY * cellSize, cellSize, cellSize, null);
        }

        g2.drawImage(player_1,
                playerX * cellSize,
                playerY * cellSize,
                cellSize, cellSize, null);

    }
}
