package Client;

import maze_game.MazeGame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * Maze Runner 클라이언트
 *
 * 화면 구성:
 * - Start 화면: title_bg.png + 큰 START 버튼
 * - Connect 화면: title_lobby.png + 닉네임/IP 입력 + CONNECT/BACK 버튼
 * - Lobby 화면: title_lobby.png + READY/START/BACK 버튼
 * - Game 화면: 플레이스홀더 (미로 화면 예정)
 *
 * 기능 구성:
 * - CardLayout로 화면 전환
 * - AnimatedImageButton: 이미지 버튼 + hover/press 애니메이션
 * - ImageBackgroundPanel: 배경 이미지 표시
 * - 서버와 TCP 통신 (JOIN/STATE/READY/START 메시지 처리)
 */
public class MazeRunnerClient extends JFrame {

    private GamePanel gamePanel;
    private MazeGame mazeGame;

    // ===== 이미지 파일 경로 =====
    private static final String IMG_TITLE_BG    = "title_bg.png";
    private static final String IMG_LOBBY_BG    = "title_lobby.png";
    private static final String IMG_BTN_CONNECT = "btn_connect.png";
    private static final String IMG_BTN_READY   = "btn_ready.png";
    private static final String IMG_BTN_START   = "btn_start.png";
    private static final String IMG_BTN_BACK    = "btn_back.png";

    // 버튼 기본 크기
    private static final int BTN_W = 260;
    private static final int BTN_H = 110;

    // Start 화면 전용 큰 버튼 크기
    private static final int START_BTN_W = 300;
    private static final int START_BTN_H = 120;

    // ===== 네트워크 관련 =====
    private volatile Socket socket;
    private volatile BufferedReader in;
    private volatile PrintWriter out;
    private volatile boolean connected = false;

    // 1 = HOST, 2 = CLIENT
    private volatile int myRole = 0;
    private volatile boolean isHost = false;
    private volatile boolean myReady = false;

    // ===== 화면 전환 (CardLayout) =====
    private CardLayout cards = new CardLayout();
    private JPanel root = new JPanel(cards);

    // ===== Connect 화면 입력창 =====
    private JTextField tfName, tfIp;
    private AnimatedImageButton btnConnectImg;
    private AnimatedImageButton btnConnectBackImg;

    // ===== Lobby 화면 UI =====
    private JLabel lblRole, lblP1, lblP2, lblState;
    private AnimatedImageButton btnReadyImg;
    private AnimatedImageButton btnStartImg;
    private AnimatedImageButton btnLobbyBackImg;

    // ===== 메인 (UI 시작) =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeRunnerClient app = new MazeRunnerClient();
            app.setVisible(true);
        });
    }

    public MazeRunnerClient() {
        setTitle("Maze Runner - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        gamePanel = new GamePanel();

        // 화면 구성
        root.add(buildStartCard(),   "start");
        root.add(buildConnectCard(), "connect");
        root.add(buildLobbyCard(),   "lobby");
        root.add(gamePanel,    "game");
        add(root);

        cards.show(root, "start");
    }

    // =====================================================================
    // 배경 이미지 패널
    // =====================================================================
    private static class ImageBackgroundPanel extends JPanel {
        private Image bg;

        public ImageBackgroundPanel(String imgPath, Color fallback) {
            setOpaque(true);
            setBackground(fallback);

            // 1) 리소스 폴더에서 읽기
            URL url = MazeRunnerClient.class.getResource("/" + imgPath);
            if (url != null) bg = new ImageIcon(url).getImage();
            else {
                // 2) 실행 폴더에서 찾기
                File f = new File(imgPath);
                if (f.exists()) bg = new ImageIcon(imgPath).getImage();
                else System.out.println("[WARN] 배경 이미지 없음: " + f.getAbsolutePath());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) {
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    // =====================================================================
    // 애니메이션 이미지 버튼
    // =====================================================================
    private static class AnimatedImageButton extends JComponent {
        private Image image;
        private float animScale = 1.0f;
        private float targetAnimScale = 1.0f;
        private Timer timer;
        private boolean enabled = true;
        private ActionListener actionListener;

        private int baseWidth;
        private int baseHeight;

        public AnimatedImageButton(String path, int targetWidth, int targetHeight) {
            this.baseWidth = targetWidth;
            this.baseHeight = targetHeight;

            // 이미지 로드
            URL url = MazeRunnerClient.class.getResource("/" + path);
            if (url != null) image = new ImageIcon(url).getImage();
            else {
                File f = new File(path);
                if (f.exists()) image = new ImageIcon(path).getImage();
                else System.out.println("[WARN] 버튼 이미지 없음: " + f.getAbsolutePath());
            }

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            setPreferredSize(new Dimension(baseWidth, baseHeight));
            setMinimumSize(new Dimension(baseWidth, baseHeight));
            setMaximumSize(new Dimension(baseWidth, baseHeight));

            // 60fps 애니메이션
            timer = new Timer(16, e -> {
                animScale += (targetAnimScale - animScale) * 0.2f;
                if (Math.abs(animScale - targetAnimScale) < 0.005f) {
                    animScale = targetAnimScale;
                }
                repaint();
            });
            timer.start();

            // hover/press 이벤트
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (enabled) targetAnimScale = 1.08f;
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (enabled) targetAnimScale = 1.0f;
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (enabled) targetAnimScale = 0.95f;
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!enabled) return;
                    targetAnimScale = 1.08f;

                    if (contains(e.getPoint()) && actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(this,
                                ActionEvent.ACTION_PERFORMED, "click"));
                    }
                }
            });
        }

        public void addActionListener(ActionListener l) {
            this.actionListener = l;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            this.targetAnimScale = 1.0f;
            this.animScale = 1.0f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if (image != null) {
                int iw = image.getWidth(this);
                int ih = image.getHeight(this);

                // 비율 유지 + 버튼 영역에 맞게 스케일
                float baseScale = Math.max((float) w / iw, (float) h / ih);
                float finalScale = baseScale * animScale;

                int drawW = (int) (iw * finalScale);
                int drawH = (int) (ih * finalScale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;

                if (!enabled)
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

                g2.drawImage(image, x, y, drawW, drawH, this);

            } else {
                // 이미지 없을 때
                g2.setColor(enabled ? Color.DARK_GRAY : Color.GRAY);
                int drawW = (int) (w * animScale);
                int drawH = (int) (h * animScale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;
                g2.fillRoundRect(x, y, drawW, drawH, 20, 20);
            }

            g2.dispose();
        }
    }

    // =====================================================================
    // 1) 시작 화면
    // =====================================================================
    private JComponent buildStartCard() {
        ImageBackgroundPanel p =
                new ImageBackgroundPanel(IMG_TITLE_BG, new Color(24, 29, 40));
        p.setLayout(null);

        AnimatedImageButton btnStartMain =
                new AnimatedImageButton(IMG_BTN_START, START_BTN_W, START_BTN_H);
        p.add(btnStartMain);

        // 가운데 아래 배치
        p.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();
                btnStartMain.setBounds(
                        (w - START_BTN_W) / 2,
                        (int) (h * 0.67),
                        START_BTN_W,
                        START_BTN_H
                );
            }
        });

        // START → Connect 화면 이동
        btnStartMain.addActionListener(e -> cards.show(root, "connect"));

        return p;
    }

    // =====================================================================
    // 2) Connect 화면
    // =====================================================================
    private JComponent buildConnectCard() {
        ImageBackgroundPanel p =
                new ImageBackgroundPanel(IMG_LOBBY_BG, new Color(30,34,42));
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(24,24,24,24));

        JLabel title = new JLabel("MAZE RUNNER - 접속", SwingConstants.LEFT);
        title.setForeground(new Color(255,205,96));
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 22));

        // 입력 폼
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new GridLayout(0,1,8,8));

        tfName = new JTextField("Player_" + (int)(Math.random()*9000+1000));
        tfIp   = new JTextField("127.0.0.1");
        style(tfName);
        style(tfIp);

        form.add(labelWrap("닉네임", tfName));
        form.add(labelWrap("서버 IP", tfIp));

        // 버튼 영역
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 16));
        bottom.setOpaque(false);

        btnConnectImg = new AnimatedImageButton(IMG_BTN_CONNECT, BTN_W, BTN_H);
        btnConnectImg.addActionListener(this::doConnect);

        btnConnectBackImg = new AnimatedImageButton(IMG_BTN_BACK, 200, 80);
        btnConnectBackImg.addActionListener(e -> cards.show(root, "start"));

        bottom.add(btnConnectImg);
        bottom.add(btnConnectBackImg);

        p.add(title, BorderLayout.NORTH);
        p.add(form, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel labelWrap(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(Color.WHITE);
        l.setBorder(new EmptyBorder(0,0,4,0));
        row.add(l, BorderLayout.NORTH);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private void style(JTextField tf){
        tf.setBackground(new Color(40,46,56));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70,80,96)),
                new EmptyBorder(6,8,6,8)
        ));
    }

    // 서버 접속
    private void doConnect(ActionEvent e) {
        String ip = tfIp.getText().trim();
        String name = tfName.getText().trim();

        if (ip.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임과 IP를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnConnectImg.setEnabled(false);

        new Thread(() -> {
            try {
                socket = new Socket(ip, 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;

                new Thread(this::recvLoop, "recv").start();
                send("JOIN " + name);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "서버에 접속했습니다.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    cards.show(root, "lobby");
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "접속 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> btnConnectImg.setEnabled(true));
            }
        }).start();
    }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    // =====================================================================
    // 3) Lobby 화면
    // =====================================================================
    private JComponent buildLobbyCard() {
        ImageBackgroundPanel p =
                new ImageBackgroundPanel(IMG_LOBBY_BG, new Color(26,31,39));
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(16,16,16,16));

        JLabel title = new JLabel("LOBBY", SwingConstants.LEFT);
        title.setForeground(new Color(255,205,96));
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 20));

        lblRole = new JLabel("역할: -");
        lblRole.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(lblRole, BorderLayout.EAST);

        // 플레이어 상태 표시
        lblP1 = new JLabel("PLAYER 1: 미접속");
        lblP2 = new JLabel("PLAYER 2: 미접속");
        lblP1.setForeground(Color.WHITE);
        lblP2.setForeground(Color.WHITE);

        JPanel mid = new JPanel(new GridLayout(2,1,6,6));
        mid.setOpaque(false);
        mid.add(lblP1);
        mid.add(lblP2);

        // 우측 버튼 영역
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        // READY 버튼
        btnReadyImg = new AnimatedImageButton(IMG_BTN_READY, BTN_W, BTN_H);
        btnReadyImg.addActionListener(e -> {
            myReady = !myReady;
            send("READY " + (myReady ? "1" : "0"));
        });

        // START 버튼 (HOST만 활성화)
        btnStartImg = new AnimatedImageButton(IMG_BTN_START, BTN_W, BTN_H);
        btnStartImg.setEnabled(false);
        btnStartImg.addActionListener(e -> send("START"));

        // BACK 버튼
        btnLobbyBackImg = new AnimatedImageButton(IMG_BTN_BACK, 200, 80);
        btnLobbyBackImg.addActionListener(e -> {
            try { if (socket != null) socket.close(); } catch (IOException ignored){}
            socket = null; in = null; out = null; connected = false; myRole = 0; myReady = false;
            lblRole.setText("역할: -");
            cards.show(root, "connect");
        });

        right.add(btnReadyImg);
        right.add(Box.createVerticalStrut(8));
        right.add(btnStartImg);
        right.add(Box.createVerticalStrut(8));
        right.add(btnLobbyBackImg);

        lblState = new JLabel("대기 중…");
        lblState.setForeground(new Color(210,220,230));
        lblState.setBorder(new EmptyBorder(8,0,0,0));

        JPanel center = new JPanel(new BorderLayout(12,0));
        center.setOpaque(false);
        center.add(mid, BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);

        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        p.add(lblState, BorderLayout.SOUTH);

        return p;
    }

    // 서버 메시지 수신 루프
    private void recvLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                final String msg = line.trim();

                // ROLE n (1=HOST, 2=CLIENT)
                if (msg.startsWith("ROLE")) {
                    String[] sp = msg.split("\\s+");
                    if (sp.length >= 2) {
                        myRole = Integer.parseInt(sp[1]);
                        isHost = (myRole == 1);

                        SwingUtilities.invokeLater(() ->
                                lblRole.setText("역할: " + (isHost ? "HOST(1)" : "CLIENT(2)")));
                    }
                }

                // STATE 정보 (플레이어 접속/준비 상태)
                else if (msg.startsWith("STATE|")) {

                    // STATE|p1Present|p1Name|p1Ready|p2Present|p2Name|p2Ready|canStart
                    String[] sp = msg.split("\\|", -1);
                    if (sp.length >= 8) {
                        boolean p1Present = "1".equals(sp[1]);
                        String p1Name = sp[2];
                        boolean p1Ready = "1".equals(sp[3]);

                        boolean p2Present = "1".equals(sp[4]);
                        String p2Name = sp[5];
                        boolean p2Ready = "1".equals(sp[6]);

                        boolean canStart = "1".equals(sp[7]);

                        // 내 READY 동기화
                        if (myRole == 1) myReady = p1Ready;
                        else if (myRole == 2) myReady = p2Ready;

                        SwingUtilities.invokeLater(() -> {
                            lblP1.setText("PLAYER 1: " +
                                    (p1Present ? p1Name : "미접속") +
                                    (p1Ready ? " [READY]" : ""));

                            lblP2.setText("PLAYER 2: " +
                                    (p2Present ? p2Name : "미접속") +
                                    (p2Ready ? " [READY]" : ""));

                            lblState.setText(canStart
                                    ? (isHost ? "두 명 모두 READY! START 가능" : "두 명 모두 READY! 호스트 대기 중")
                                    : "상대 접속/READY 대기…");

                            btnStartImg.setEnabled(isHost && canStart);
                        });
                    }
                }

                // 게임 시작
                else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(() -> cards.show(root, "game"));
                }

                // 인원 초과
                else if (msg.equals("FULL")) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "방이 가득 찼습니다.", "알림", JOptionPane.WARNING_MESSAGE));
                }
            }

        } catch (IOException e) {
            // 서버 연결 종료
        } finally {
            connected = false;
        }
    }
}
