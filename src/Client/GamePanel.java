package Client;

import maze_game.Player;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;

public class GamePanel extends JPanel implements KeyListener {

    private int[][] maze;
    private int cellSize = 24;

    private int exitX = -1, exitY = -1;

    private Player player1; // 나
    private Player player2; // 상대

    private BufferedImage wallTile;
    private BufferedImage flag;
    private BufferedImage player_1;
    private BufferedImage player_2;

    private PrintWriter out;   // 서버 송신 (나중에 setNetworkOutput로 연결됨)

    public GamePanel(PrintWriter out) {
        this.out = out;

        setBackground(new Color(0xF7EEDB));

        loadImages();

        setFocusable(true);
        addKeyListener(this);
    }

    /* ===================== 이미지 로딩 ===================== */
    private void loadImages() {
        try {
            wallTile = ImageIO.read(getClass().getResource("/Client/floor.png"));
            flag = ImageIO.read(getClass().getResource("/Client/flag.png"));
            player_1 = ImageIO.read(getClass().getResource("/Client/player1_r.png"));
            player_2 = ImageIO.read(getClass().getResource("/Client/player2_r.png"));
        } catch (Exception e) {
            System.out.println("이미지 로딩 실패: " + e);
        }
    }

    /* ===================== 키 입력 처리 → 서버 이동 명령 전송 ===================== */
    @Override
    public void keyPressed(KeyEvent e) {
        if (out == null) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    out.println("MOVE UP");    break;
            case KeyEvent.VK_DOWN:  out.println("MOVE DOWN");  break;
            case KeyEvent.VK_LEFT:  out.println("MOVE LEFT");  break;
            case KeyEvent.VK_RIGHT: out.println("MOVE RIGHT"); break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    /* ===================== MazeRunnerClient에서 네트워크 출력 스트림 설정 ===================== */
    public void setNetworkOutput(PrintWriter out) {
        this.out = out;
    }

    /* ===================== 서버로부터 받은 좌표 적용 ===================== */
    public void updatePlayer1Position(int x, int y) {
        if (player1 != null) {
            player1.setPosition(x, y);
            repaint();
        }
    }

    public void updatePlayer2Position(int x, int y) {
        if (player2 != null) {
            player2.setPosition(x, y);
            repaint();
        }
    }

    /* ===================== 서버에서 전달받은 미로 설정 ===================== */
    public void setMaze(int[][] maze) {
        this.maze = maze;

        player1 = new Player(1, 1, maze);
        player2 = new Player(1, 1, maze);

        setPreferredSize(
                new Dimension(maze[0].length * cellSize, maze.length * cellSize)
        );

        // ★ 게임 시작 시 키포커스를 강하게 가져오기
        requestFocusInWindow();

        revalidate();
        repaint();
    }

    /* ===================== 출구 좌표 설정 ===================== */
    public void setExit(int x, int y) {
        this.exitX = x;
        this.exitY = y;
        repaint();
    }

    /* ===================== 렌더링 ===================== */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (maze == null) return;

        Graphics2D g2 = (Graphics2D) g;

        // 미로 타일
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {

                // 벽만 그림
                if (maze[y][x] == 0) {
                    g2.drawImage(wallTile,
                            x * cellSize, y * cellSize,
                            cellSize, cellSize, null);
                }
            }
        }

        // 출구 표시
        if (exitX != -1 && exitY != -1) {
            g2.drawImage(flag,
                    exitX * cellSize,
                    exitY * cellSize,
                    cellSize, cellSize, null);
        }

        // 플레이어1 (나)
        if (player1 != null) {
            g2.drawImage(player_1,
                    player1.getX() * cellSize,
                    player1.getY() * cellSize,
                    cellSize, cellSize, null);
        }

        // 플레이어2 (상대)
        if (player2 != null) {
            g2.drawImage(player_2,
                    player2.getX() * cellSize,
                    player2.getY() * cellSize,
                    cellSize, cellSize, null);
        }
    }
}
