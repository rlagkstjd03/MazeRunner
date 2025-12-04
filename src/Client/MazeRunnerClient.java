package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MazeRunnerClient extends JFrame {

    // ===== UI 카드 레이아웃 =====
    private CardLayout cards = new CardLayout();
    private JPanel root = new JPanel(cards);

    // ===== 네트워크 =====
    private volatile Socket socket;
    private volatile BufferedReader in;
    private volatile PrintWriter out;
    private volatile boolean connected = false;

    private volatile int myRole = 0;
    private volatile boolean isHost = false;
    private volatile boolean myReady = false;

    // ===== Lobby UI =====
    private JTextField tfName, tfIp;
    private JLabel lblRole, lblP1, lblP2, lblState;

    // ===== GamePanel (게임 화면 패널) =====
    private GamePanel gamePanel;   // ★★★ 가장 중요: 필드로 선언 ★★★

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeRunnerClient().setVisible(true));
    }

    public MazeRunnerClient() {
        setTitle("Maze Runner - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // ★★★ GamePanel을 가장 먼저 생성 (out은 나중에 연결됨)
        gamePanel = new GamePanel(null);

        root.add(buildStartCard(), "start");
        root.add(buildConnectCard(), "connect");
        root.add(buildLobbyCard(), "lobby");
        root.add(buildGameCard(), "game");

        add(root);
        cards.show(root, "start");
    }

    // =====================================================================
    // START 화면
    // =====================================================================
    private JComponent buildStartCard() {
        JPanel p = new JPanel(new BorderLayout());
        JButton btn = new JButton("START");
        btn.setFont(new Font("Malgun Gothic", Font.BOLD, 30));

        btn.addActionListener(e -> cards.show(root, "connect"));

        p.add(btn, BorderLayout.CENTER);
        return p;
    }

    // =====================================================================
    // CONNECT 화면
    // =====================================================================
    private JComponent buildConnectCard() {
        JPanel p = new JPanel(new BorderLayout(10,10));

        tfName = new JTextField("Player");
        tfIp   = new JTextField("127.0.0.1");

        JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(this::doConnect);

        JPanel form = new JPanel(new GridLayout(0,1,5,5));
        form.add(new JLabel("Name"));
        form.add(tfName);
        form.add(new JLabel("Server IP"));
        form.add(tfIp);
        form.add(btnConnect);

        p.add(form, BorderLayout.CENTER);
        return p;
    }

    private void doConnect(ActionEvent e) {
        String name = tfName.getText().trim();
        String ip = tfIp.getText().trim();

        if (name.isEmpty() || ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임/IP 입력하세요.");
            return;
        }

        new Thread(() -> {
            try {
                socket = new Socket(ip, 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;

                // ★ GamePanel에 out 연결
                gamePanel.setNetworkOutput(out);

                new Thread(this::recvLoop).start();
                send("JOIN " + name);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "서버 연결 완료");
                    cards.show(root, "lobby");
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "연결 실패: " + ex.getMessage()));
            }
        }).start();
    }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    // =====================================================================
    // LOBBY 화면
    // =====================================================================
    private JComponent buildLobbyCard() {
        JPanel p = new JPanel(new BorderLayout(10,10));

        lblRole = new JLabel("역할: -");
        lblP1 = new JLabel("PLAYER 1: -");
        lblP2 = new JLabel("PLAYER 2: -");
        lblState = new JLabel("대기중...");

        JButton btnReady = new JButton("READY");
        btnReady.addActionListener(e -> {
            myReady = !myReady;
            send("READY " + (myReady ? "1" : "0"));
        });

        JButton btnStart = new JButton("START");
        btnStart.addActionListener(e -> send("START"));

        JPanel info = new JPanel(new GridLayout(0,1));
        info.add(lblRole);
        info.add(lblP1);
        info.add(lblP2);
        info.add(lblState);

        JPanel btns = new JPanel(new GridLayout(0,1));
        btns.add(btnReady);
        btns.add(btnStart);

        p.add(info, BorderLayout.CENTER);
        p.add(btns, BorderLayout.EAST);

        return p;
    }

    // =====================================================================
    // GAME 화면
    // =====================================================================
    private JComponent buildGameCard() {

        // ★★★ GamePanel을 포함하는 진짜 게임 화면
        JScrollPane scroll = new JScrollPane(gamePanel);

        return scroll;
    }

    // =====================================================================
    // 서버 메시지 수신
    // =====================================================================
    private void recvLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line.trim();

                // ROLE n
                if (msg.startsWith("ROLE")) {
                    String[] sp = msg.split("\\s+");
                    myRole = Integer.parseInt(sp[1]);
                    isHost = (myRole == 1);

                    SwingUtilities.invokeLater(() ->
                            lblRole.setText("역할: " + (isHost ? "HOST" : "CLIENT")));
                }

                // STATE|...
                else if (msg.startsWith("STATE|")) {
                    String[] sp = msg.split("\\|");

                    boolean p1Ready = sp[3].equals("1");
                    boolean p2Ready = sp[6].equals("1");
                    boolean canStart = sp[7].equals("1");

                    SwingUtilities.invokeLater(() -> {
                        lblP1.setText("PLAYER 1: " + sp[2] + (p1Ready ? " [READY]" : ""));
                        lblP2.setText("PLAYER 2: " + sp[5] + (p2Ready ? " [READY]" : ""));
                        lblState.setText(canStart ? "START 가능" : "대기중...");
                    });
                }

                // START ============
                else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(() -> {
                        cards.show(root, "game");

                        // ★★★★★ 가장 중요한 부분 ★★★★★
                        gamePanel.setFocusable(true);
                        gamePanel.requestFocus();
                        gamePanel.requestFocusInWindow();
                    });
                }

                // MAZE|W|H|데이터
                else if (msg.startsWith("MAZE|")) {
                    int[][] maze = parseMaze(msg);
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.setMaze(maze);
                        gamePanel.requestFocusInWindow();
                    });
                }

                // EXIT|x|y
                else if (msg.startsWith("EXIT|")) {
                    String[] sp = msg.split("\\|");
                    int ex = Integer.parseInt(sp[1]);
                    int ey = Integer.parseInt(sp[2]);

                    SwingUtilities.invokeLater(() ->
                            gamePanel.setExit(ex, ey));
                }

                // 위치 업데이트
                else if (msg.startsWith("P1_POS|")) {
                    String[] sp = msg.split("\\|");
                    SwingUtilities.invokeLater(() ->
                            gamePanel.updatePlayer1Position(
                                    Integer.parseInt(sp[1]),
                                    Integer.parseInt(sp[2])));
                }
                else if (msg.startsWith("P2_POS|")) {
                    String[] sp = msg.split("\\|");
                    SwingUtilities.invokeLater(() ->
                            gamePanel.updatePlayer2Position(
                                    Integer.parseInt(sp[1]),
                                    Integer.parseInt(sp[2])));
                }
            }

        } catch (Exception e) {
            System.out.println("연결 종료");
        }
    }


    // =====================================================================
    // MAZE 문자열 파싱
    // =====================================================================
    private int[][] parseMaze(String msg) {
        String[] parts = msg.split("\\|");
        int W = Integer.parseInt(parts[1]);
        int H = Integer.parseInt(parts[2]);

        String[] raw = parts[3].split(",");

        int[][] maze = new int[H][W];
        int idx = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                maze[y][x] = Integer.parseInt(raw[idx++]);
            }
        }

        return maze;
    }
}
