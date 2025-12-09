package Client;

import maze_game.Player;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;

public class GamePanel extends JPanel implements KeyListener {

    private int[][] maze;
    private int cellSize = 24;

    private int exitX = -1, exitY = -1;

    private Player player1; // 나
    private Player player2; // 상대

    private BufferedImage floor;
    private BufferedImage wallTile;
    private BufferedImage flag;

    // --- 플레이어 1 이미지 ---
    private BufferedImage p1_stand;
    private BufferedImage p1_walk_l;
    private BufferedImage p1_walk_r;

    // --- 플레이어 2 이미지 ---
    private BufferedImage p2_stand;
    private BufferedImage p2_walk_l;
    private BufferedImage p2_walk_r;

    private boolean p1Walking = false;
    private boolean p2Walking = false;

    private long lastFrameTime = 0;
    private boolean walkFrame = false;   // true면 walk, false면 stand

    private PrintWriter out;

    private int visibileRadius = 140;

    public GamePanel(PrintWriter out) {
        this.out = out;

        setBackground(new Color(0xF7EEDB));

        loadImages();

        setFocusable(true);
        addKeyListener(this);

        requestFocusInWindow();
    }

    /* ===================== 이미지 로딩 ===================== */
    private void loadImages() {
        try {
            floor    = ImageIO.read(getClass().getResource("/Client/floor.png"));
            wallTile = ImageIO.read(getClass().getResource("/Client/wall.png"));
            flag     = ImageIO.read(getClass().getResource("/Client/flag.png"));

            // ★ 방향별 이미지 로딩
            p1_stand   = ImageIO.read(getClass().getResource("/Client/player1_stand.png"));
            p1_walk_l  = ImageIO.read(getClass().getResource("/Client/player1_walk_l.png"));
            p1_walk_r  = ImageIO.read(getClass().getResource("/Client/player1_walk_r.png"));

            p2_stand   = ImageIO.read(getClass().getResource("/Client/player2_stand.png"));
            p2_walk_l  = ImageIO.read(getClass().getResource("/Client/player2_walk_l.png"));
            p2_walk_r  = ImageIO.read(getClass().getResource("/Client/player2_walk_r.png"));

        } catch (Exception e) {
            System.out.println("이미지 로딩 실패: " + e);
        }
    }

    /* ===================== 키 입력 처리 → 서버 이동 명령 전송 ===================== */
    @Override
    public void keyPressed(KeyEvent e) {
        if (out == null) return;

        p1Walking = true;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    out.println("MOVE UP");    break;
            case KeyEvent.VK_DOWN:  out.println("MOVE DOWN");  break;
            case KeyEvent.VK_LEFT:  out.println("MOVE LEFT");  break;
            case KeyEvent.VK_RIGHT: out.println("MOVE RIGHT"); break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public void setNetworkOutput(PrintWriter out) {
        this.out = out;
    }

    /* ===================== 서버 좌표 반영 ===================== */
    public void updatePlayer1Position(int x, int y) {
        if (player1 != null) {
            p1Walking = (player1.getX() != x || player1.getY() != y);
            player1.setPosition(x, y);
            repaint();
        }
    }

    public void updatePlayer2Position(int x, int y) {
        if (player2 != null) {
            p2Walking = (player2.getX() != x || player2.getY() != y);
            player2.setPosition(x, y);
            repaint();
        }
    }

    /* ===================== 미로 설정 ===================== */
    public void setMaze(int[][] maze) {
        this.maze = maze;

        player1 = new Player(1, 1, maze);
        player2 = new Player(1, 1, maze);

        setPreferredSize(
                new Dimension(maze[0].length * cellSize, maze.length * cellSize)
        );

        requestFocusInWindow();
        revalidate();
        repaint();
    }

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

        // === 1) 미로 타일 ===
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {

                if (maze[y][x] == 1) {
                    g2.drawImage(floor, x * cellSize, y * cellSize, cellSize, cellSize, null);
                } else {
                    g2.drawImage(wallTile, x * cellSize, y * cellSize, cellSize, cellSize, null);
                }
            }
        }

        // === 2) 출구 ===
        if (exitX != -1 && exitY != -1) {
            g2.drawImage(flag, exitX * cellSize, exitY * cellSize, cellSize, cellSize, null);
        }

        // === 3) 애니메이션 프레임 업데이트 ===
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > 180) {  // 0.18초마다 프레임 전환
            walkFrame = !walkFrame;
            lastFrameTime = now;
        }

        // === 4) 플레이어1 이미지 선택 ===
        BufferedImage img1;
        if (p1Walking) {
            if (!walkFrame) {
                img1 = p1_stand;
            } else {
                img1 = (player1.getDirection() == Player.Direction.LEFT) ? p1_walk_l : p1_walk_r;
            }
        } else {
            img1 = p1_stand;
        }

        g2.drawImage(img1,
                player1.getX() * cellSize, player1.getY() * cellSize,
                cellSize, cellSize, null);

        // === 5) 플레이어2 이미지 선택 ===
        BufferedImage img2;
        if (p2Walking) {
            if (!walkFrame) {
                img2 = p2_stand;
            } else {
                img2 = (player2.getDirection() == Player.Direction.LEFT) ? p2_walk_l : p2_walk_r;
            }
        } else {
            img2 = p2_stand;
        }

        g2.drawImage(img2,
                player2.getX() * cellSize, player2.getY() * cellSize,
                cellSize, cellSize, null);

        // === 6) 시야 (플레이어1 + 플레이어2) ===
        BufferedImage fog = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D fg = fog.createGraphics();

        // 전체 어둡게
        fg.setColor(new Color(0, 0, 0, 200));
        fg.fillRect(0, 0, getWidth(), getHeight());
        fg.setComposite(AlphaComposite.DstOut);

        // p1 시야
        int p1x = player1.getX() * cellSize + cellSize / 2;
        int p1y = player1.getY() * cellSize + cellSize / 2;

        fg.fill(new Ellipse2D.Float(
                p1x - visibileRadius,
                p1y - visibileRadius,
                visibileRadius * 2,
                visibileRadius * 2
        ));

        // p2 시야
        int p2x = player2.getX() * cellSize + cellSize / 2;
        int p2y = player2.getY() * cellSize + cellSize / 2;

        fg.fill(new Ellipse2D.Float(
                p2x - visibileRadius,
                p2y - visibileRadius,
                visibileRadius * 2,
                visibileRadius * 2
        ));

        fg.dispose();
        g2.drawImage(fog, 0, 0, null);
    }
}
