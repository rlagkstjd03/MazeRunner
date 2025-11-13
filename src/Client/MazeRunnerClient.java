package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class MazeRunnerClient extends JFrame {
    private JTextField tfName, tfIp;
    private JButton btnConnect;

    private JLabel lblRole, lblP1, lblP2, lblState;
    private JButton btnReady, btnStart, btnBack;

    private CardLayout cards = new CardLayout();
    private JPanel root = new JPanel(cards);

    private volatile Socket socket;
    private volatile BufferedReader in;
    private volatile PrintWriter out;

    private volatile int myRole = 0;      // 1 or 2
    private volatile boolean isHost = false;
    private volatile boolean connected = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeRunnerClient app = new MazeRunnerClient();
            app.setVisible(true);
        });
    }

    public MazeRunnerClient() {
        setTitle("Maze Runner - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 560);
        setLocationRelativeTo(null);

        root.add(buildConnectCard(), "connect");
        root.add(buildLobbyCard(), "lobby");
        root.add(buildGameCard(), "game");
        add(root);

        cards.show(root, "connect");
    }

    // ===== Connect Card =====
    private JComponent buildConnectCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(30,34,42));
        p.setBorder(new EmptyBorder(24,24,24,24));

        JLabel title = new JLabel("MAZE RUNNER - 접속", SwingConstants.LEFT);
        title.setForeground(new Color(255,205,96));
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 22));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new GridLayout(0,1,8,8));

        tfName = new JTextField("Player_" + (int)(Math.random()*9000+1000));
        tfIp   = new JTextField("127.0.0.1");
        style(tfName); style(tfIp);

        btnConnect = new JButton("서버 접속");
        btnConnect.addActionListener(this::doConnect);

        form.add(labelWrap("닉네임", tfName));
        form.add(labelWrap("서버 IP", tfIp));
        form.add(btnConnect);

        p.add(title, BorderLayout.NORTH);
        p.add(form, BorderLayout.CENTER);
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

    private void doConnect(ActionEvent e) {
        String ip = tfIp.getText().trim();
        String name = tfName.getText().trim();
        if (ip.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임과 IP를 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnConnect.setEnabled(false);

        new Thread(() -> {
            try {
                socket = new Socket(ip, 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;

                // 수신 스레드
                new Thread(this::recvLoop, "recv").start();

                // JOIN 전송
                send("JOIN " + name);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "서버에 접속했습니다.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    cards.show(root, "lobby");
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "접속 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> btnConnect.setEnabled(true));
            }
        }).start();
    }

    private void send(String s) {
        if (out != null) out.println(s);
    }

    // ===== Lobby Card =====
    private JComponent buildLobbyCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(26,31,39));
        p.setBorder(new EmptyBorder(16,16,16,16));

        JLabel title = new JLabel("로비", SwingConstants.LEFT);
        title.setForeground(new Color(255,205,96));
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 20));

        lblRole = new JLabel("역할: -");
        lblRole.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(lblRole, BorderLayout.EAST);

        lblP1 = new JLabel("PLAYER 1: 미접속");
        lblP2 = new JLabel("PLAYER 2: 미접속");
        lblP1.setForeground(Color.WHITE);
        lblP2.setForeground(Color.WHITE);

        JPanel mid = new JPanel(new GridLayout(2,1,6,6));
        mid.setOpaque(false);
        mid.add(lblP1);
        mid.add(lblP2);

        btnReady = new JButton("READY");
        btnReady.addActionListener(e -> {
            // 토글: 버튼 텍스트로 판단 안 하고 서버 상태가 진리지만, 간단 목업용
            boolean toReady = btnReady.getText().equals("READY");
            send("READY " + (toReady ? "1" : "0"));
            btnReady.setText(toReady ? "UNREADY" : "READY");
        });

        btnStart = new JButton("START (HOST)");
        btnStart.setEnabled(false);
        btnStart.addActionListener(e -> send("START"));

        btnBack = new JButton("연결 종료");
        btnBack.addActionListener(e -> {
            try { if (socket != null) socket.close(); } catch (IOException ignored){}
            socket = null; in = null; out = null; connected = false; myRole = 0;
            lblRole.setText("역할: -");
            btnReady.setText("READY");
            cards.show(root, "connect");
        });

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(btnReady);
        right.add(Box.createVerticalStrut(8));
        right.add(btnStart);
        right.add(Box.createVerticalStrut(8));
        right.add(btnBack);

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

    // ===== Game Card (플레이스홀더) =====
    private JComponent buildGameCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(22,22,26));
        JLabel l = new JLabel("게임 시작! (여기서 미로 화면으로 전환)", SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private void recvLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                final String msg = line.trim();
                if (msg.startsWith("ROLE")) {
                    String[] sp = msg.split("\\s+");
                    if (sp.length >= 2) {
                        myRole = Integer.parseInt(sp[1]);
                        isHost = (myRole == 1);
                        SwingUtilities.invokeLater(() -> lblRole.setText("역할: " + (isHost ? "HOST(1)" : "CLIENT(2)")));
                    }
                } else if (msg.startsWith("STATE|")) {
                    // STATE|p1Present|p1Name|p1Ready|p2Present|p2Name|p2Ready|canStart
                    String[] sp = msg.split("\\|", -1);
                    // index: 0=STATE, 1=p1Present, 2=p1Name, 3=p1Ready, 4=p2Present, 5=p2Name, 6=p2Ready, 7=canStart
                    if (sp.length >= 8) {
                        boolean p1Present = "1".equals(sp[1]);
                        String p1Name = sp[2];
                        boolean p1Ready = "1".equals(sp[3]);

                        boolean p2Present = "1".equals(sp[4]);
                        String p2Name = sp[5];
                        boolean p2Ready = "1".equals(sp[6]);

                        boolean canStart = "1".equals(sp[7]);

                        SwingUtilities.invokeLater(() -> {
                            lblP1.setText("PLAYER 1: " + (p1Present ? p1Name : "미접속") + (p1Ready ? " [READY]" : ""));
                            lblP2.setText("PLAYER 2: " + (p2Present ? p2Name : "미접속") + (p2Ready ? " [READY]" : ""));
                            lblState.setText(canStart
                                    ? (isHost ? "두 명 모두 READY! START 가능" : "두 명 모두 READY! 호스트 대기 중")
                                    : "상대 접속/READY 대기…");
                            btnStart.setEnabled(isHost && canStart);
                        });
                    }
                } else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(() -> cards.show(root, "game"));
                } else if (msg.equals("FULL")) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "방이 가득 찼습니다.", "알림", JOptionPane.WARNING_MESSAGE));
                }
            }
        } catch (IOException e) {
            // 연결 종료
        } finally {
            connected = false;
        }
    }
}
