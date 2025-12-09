package Client;

import maze_game.Player;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;

/**
 * Maze Runner 클라이언트
 * 이미지 버튼 + 배경 복원 / 버튼 크기 조절 가능 버전
 */
public class MazeRunnerClient extends JFrame {

    // ===== 이미지 파일 경로 =====
    private static final String IMG_TITLE_BG    = "/Client/title_bg.png";
    private static final String IMG_LOBBY_BG    = "/Client/title_lobby.png";

    private static final String IMG_BTN_CONNECT = "/Client/btn_connect.png";
    private static final String IMG_BTN_READY   = "/Client/btn_ready.png";
    private static final String IMG_BTN_START   = "/Client/btn_start.png";
    private static final String IMG_BTN_BACK    = "/Client/btn_back.png";

    // ===== 버튼 크기 조절 =====
    private static final int BTN_W = 220;
    private static final int BTN_H = 95;

    private static final int START_BTN_W = 300;
    private static final int START_BTN_H = 120;

    // ===== 네트워크 =====
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean connected = false;
    private boolean myReady   = false;
    private int myRole        = 0;
    private boolean isHost    = false;

    // ===== 화면 전환 =====
    private CardLayout cards = new CardLayout();
    private JPanel root;

    // ===== UI =====
    private JTextField tfName;
    private JTextField tfIp;
    private JLabel lblRole;
    private JLabel lblP1;
    private JLabel lblP2;
    private JLabel lblState;

    private AnimatedImageButton btnReadyImg;
    private AnimatedImageButton btnStartImg;

    // ===== 게임 패널 =====
    private GamePanel gamePanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeRunnerClient().setVisible(true));
    }

    public MazeRunnerClient() {



        this.root = new JPanel(cards);
        this.gamePanel = new GamePanel(null);

        setTitle("Maze Runner - Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        root.add(buildStartCard(),   "start");
        root.add(buildConnectCard(), "connect");
        root.add(buildLobbyCard(),   "lobby");
        root.add(buildGameCard(),    "game");

        add(root);
        cards.show(root, "start");
    }

    //============================================================
    // Maze 데이터 파싱
    //============================================================
    private int[][] parseMaze(String msg) {
        String[] parts = msg.split("\\|");

        int W = Integer.parseInt(parts[1]);
        int H = Integer.parseInt(parts[2]);
        String[] cells = parts[3].split(",");

        int[][] maze = new int[H][W];
        int idx = 0;

        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                maze[y][x] = Integer.parseInt(cells[idx++]);

        return maze;
    }

    //============================================================
    // START 화면
    //============================================================
    private JComponent buildStartCard() {
        ImageBackgroundPanel p = new ImageBackgroundPanel(IMG_TITLE_BG);
        p.setLayout(null);

        AnimatedImageButton btnStart = new AnimatedImageButton(
                IMG_BTN_START, START_BTN_W, START_BTN_H
        );
        btnStart.setBounds(300, 380, START_BTN_W, START_BTN_H);
        btnStart.addActionListener(e -> cards.show(root, "connect"));

        p.add(btnStart);
        return p;
    }

    //============================================================
    // CONNECT 화면
    //============================================================
    private JComponent buildConnectCard() {
        ImageBackgroundPanel p = new ImageBackgroundPanel(IMG_LOBBY_BG);
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("SERVER CONNECT", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10));
        form.setOpaque(false);
        tfName = new JTextField("Player_" + (int)(Math.random()*9000+1000));
        tfIp   = new JTextField("127.0.0.1");

        form.add(labeled("닉네임", tfName));
        form.add(labeled("서버 IP", tfIp));

        AnimatedImageButton btnConnect =
                new AnimatedImageButton(IMG_BTN_CONNECT, BTN_W, BTN_H);
        btnConnect.addActionListener(e -> doConnect());

        AnimatedImageButton btnBack =
                new AnimatedImageButton(IMG_BTN_BACK, BTN_W, BTN_H);
        btnBack.addActionListener(e -> cards.show(root, "start"));

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(btnConnect);
        bottom.add(btnBack);

        p.add(title, BorderLayout.NORTH);
        p.add(form, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private JPanel labeled(String label, JComponent cmp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setForeground(Color.WHITE);

        p.add(l, BorderLayout.NORTH);
        p.add(cmp, BorderLayout.CENTER);
        return p;
    }

    //============================================================
    // LOBBY
    //============================================================
    private JComponent buildLobbyCard() {
        ImageBackgroundPanel p = new ImageBackgroundPanel(IMG_LOBBY_BG);
        p.setLayout(new BorderLayout());

        lblRole = new JLabel("역할: -");
        lblRole.setForeground(Color.WHITE);

        lblP1 = new JLabel("PLAYER1: -");
        lblP1.setForeground(Color.WHITE);

        lblP2 = new JLabel("PLAYER2: -");
        lblP2.setForeground(Color.WHITE);

        lblState = new JLabel("대기 중…");
        lblState.setForeground(Color.WHITE);

        // READY 버튼
        btnReadyImg = new AnimatedImageButton(IMG_BTN_READY, BTN_W, BTN_H);
        btnReadyImg.addActionListener(e -> {
            myReady = !myReady;
            send("READY " + (myReady ? "1" : "0"));
        });

        // START 버튼
        btnStartImg = new AnimatedImageButton(IMG_BTN_START, BTN_W, BTN_H);
        btnStartImg.setEnabled(false);
        btnStartImg.addActionListener(e -> send("START"));

        JPanel players = new JPanel(new GridLayout(0, 1));
        players.setOpaque(false);
        players.add(lblP1);
        players.add(lblP2);

        JPanel right = new JPanel(new GridLayout(0, 1, 10, 10));
        right.setOpaque(false);
        right.add(btnReadyImg);
        right.add(btnStartImg);

        p.add(lblRole, BorderLayout.NORTH);
        p.add(players, BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        p.add(lblState, BorderLayout.SOUTH);

        return p;
    }

    //============================================================
    // GAME 화면
    //============================================================
    private JComponent buildGameCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);

        JScrollPane scroll = new JScrollPane(gamePanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    //============================================================
    // 서버 연결
    //============================================================
    private void doConnect() {
        String ip   = tfIp.getText().trim();
        String name = tfName.getText().trim();

        new Thread(() -> {
            try {
                socket = new Socket(ip, 5000);

                in  = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(
                        socket.getOutputStream(), "UTF-8"), true);

                connected = true;

                gamePanel.setNetworkOutput(out);
                new Thread(this::recvLoop).start();

                send("JOIN " + name);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "서버 연결 성공!");
                    cards.show(root, "lobby");
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "접속 실패: " + e));
            }
        }).start();
    }

    private void send(String msg) {
        if (out != null) out.println(msg);
    }

    //============================================================
    // 서버 메시지 루프
    //============================================================
    private void recvLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line.trim();

                if (msg.startsWith("ROLE")) {
                    myRole = Integer.parseInt(msg.split("\\s+")[1]);
                    isHost = (myRole == 1);

                    SwingUtilities.invokeLater(() ->
                            lblRole.setText("역할: " + (isHost ? "HOST" : "CLIENT"))
                    );

                } else if (msg.startsWith("STATE|")) {
                    handleState(msg);

                } else if (msg.startsWith("MAZE|")) {
                    int[][] maze = parseMaze(msg);
                    SwingUtilities.invokeLater(() -> gamePanel.setMaze(maze));

                } else if (msg.startsWith("EXIT|")) {
                    String[] sp = msg.split("\\|");
                    int ex = Integer.parseInt(sp[1]);
                    int ey = Integer.parseInt(sp[2]);
                    SwingUtilities.invokeLater(() -> gamePanel.setExit(ex, ey));

                } else if (msg.startsWith("P1_POS|")) {
                    String[] sp = msg.split("\\|");
                    SwingUtilities.invokeLater(() ->
                            gamePanel.updatePlayer1Position(
                                    Integer.parseInt(sp[1]),
                                    Integer.parseInt(sp[2])
                            ));

                } else if (msg.startsWith("P2_POS|")) {
                    String[] sp = msg.split("\\|");
                    SwingUtilities.invokeLater(() ->
                            gamePanel.updatePlayer2Position(
                                    Integer.parseInt(sp[1]),
                                    Integer.parseInt(sp[2])
                            ));

                } else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(() -> {
                        cards.show(root, "game");
                        gamePanel.requestFocusInWindow();
                    });
                }
            }
        } catch (Exception e) {
            connected = false;
        }
    }

    //============================================================
    // STATE 처리
    //============================================================
    private void handleState(String msg) {
        String[] sp = msg.split("\\|");

        boolean p1Present = sp[1].equals("1");
        String  p1Name    = sp[2];
        boolean p1Ready   = sp[3].equals("1");

        boolean p2Present = sp[4].equals("1");
        String  p2Name    = sp[5];
        boolean p2Ready   = sp[6].equals("1");

        boolean canStart  = sp[7].equals("1");

        SwingUtilities.invokeLater(() -> {
            lblP1.setText("PLAYER 1: " + (p1Present ? p1Name : "미접속") + (p1Ready ? " [READY]" : ""));
            lblP2.setText("PLAYER 2: " + (p2Present ? p2Name : "미접속") + (p2Ready ? " [READY]" : ""));

            if (isHost) btnStartImg.setEnabled(canStart);
            lblState.setText(canStart ? "게임 시작 가능!" : "준비 대기중...");
        });
    }

    //============================================================
    // 배경 이미지 패널
    //============================================================
    private static class ImageBackgroundPanel extends JPanel {

        private Image bg;

        public ImageBackgroundPanel(String path) {
            try {
                bg = ImageIO.read(getClass().getResource(path));
            } catch (Exception e) {
                System.out.println("배경 이미지 로딩 실패: " + path);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) {
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    //============================================================
    // 이미지 버튼 (사이즈 지정 가능)
    //============================================================
    private static class AnimatedImageButton extends JButton {

        public AnimatedImageButton(String imgPath, int w, int h) {

            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);

            try {
                Image img = ImageIO.read(getClass().getResource(imgPath));
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                setIcon(new ImageIcon(scaled));
                setPreferredSize(new Dimension(w, h));

            } catch (Exception e) {
                System.out.println("버튼 이미지 로딩 실패: " + imgPath);
                setText("BUTTON");
                setPreferredSize(new Dimension(w, h));
            }
        }
    }

}
