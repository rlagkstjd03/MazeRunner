package Server;

import maze_game.MakeMaze;
import java.io.*;
import java.net.*;
import java.util.*;

public class MazeRunnerServer {
    private static final int PORT = 5000;

    private final List<Client> clients = Collections.synchronizedList(new ArrayList<>(2));
    private final GameState gameState = new GameState();

    public static void main(String[] args) {
        new MazeRunnerServer().start();
    }

    void start() {
        System.out.println("[Server] Listening on " + PORT);
        try (ServerSocket ss = new ServerSocket(PORT)) {

            while (true) {
                Socket s = ss.accept();

                if (clients.size() >= 2) {
                    new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true)
                            .println("FULL");
                    s.close();
                    continue;
                }

                int role = clients.size() + 1;
                Client c = new Client(s, role);
                clients.add(c);

                new Thread(() -> handle(c)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* ============= 클라이언트 개별 처리 스레드 ============= */
    private void handle(Client c) {
        try {
            c.out.println("ROLE " + c.role);
            broadcastState();

            String line;
            while ((line = c.in.readLine()) != null) {
                line = line.trim();

                /* JOIN */
                if (line.startsWith("JOIN ")) {
                    c.name = line.substring(5).trim();
                    broadcastState();
                }

                /* READY */
                else if (line.startsWith("READY ")) {
                    c.ready = "1".equals(line.substring(6).trim());
                    broadcastState();
                }

                /* START 요청 (P1만 가능) */
                else if (line.equals("START")) {
                    if (c.role == 1 &&
                            clients.size() == 2 &&
                            clients.get(0).ready &&
                            clients.get(1).ready) {

                        // 미로 생성
                        System.out.println("[Server] Generating maze...");
                        gameState.generateMaze();

                        // 미로 전송
                        broadcast(gameState.mazeToString());

                        // 출구 전송
                        broadcast("EXIT|" + gameState.exitX + "|" + gameState.exitY);

                        // 게임 시작 신호
                        broadcast("START");

                        // 초기 좌표 브로드캐스트
                        broadcastPositions();

                        System.out.println("[Server] Maze + START sent.");
                    }
                }

                /* ===================== MOVE 처리 ====================== */
                else if (line.startsWith("MOVE ")) {
                    String dir = line.substring(5).trim();

                    // P1인지 P2인지 구분 후 이동
                    if (c.role == 1) gameState.movePlayer1(dir);
                    else if (c.role == 2) gameState.movePlayer2(dir);

                    // ====== ★ 승리 체크 추가 ★ ======
                    if (gameState.p1x == gameState.exitX && gameState.p1y == gameState.exitY) {
                        broadcast("WIN|1");        // 플레이어 1 승리
                        continue; // 더 이상 MOVE 처리 안함
                    }

                    if (gameState.p2x == gameState.exitX && gameState.p2y == gameState.exitY) {
                        broadcast("WIN|2");        // 플레이어 2 승리
                        continue;
                    }
                    // =================================

                    // 모든 클라에게 위치 알려줌
                    broadcastPositions();
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] Client disconnected (role " + c.role + ")");
        } finally {
            try { c.socket.close(); } catch (IOException ignored){}
            clients.remove(c);
            broadcastState();
        }
    }


    /* 전체 로비 상태 브로드캐스트 */
    private void broadcastState() {
        boolean p1Present = clients.stream().anyMatch(cl -> cl.role == 1);
        boolean p2Present = clients.stream().anyMatch(cl -> cl.role == 2);

        String p1Name = clients.stream()
                .filter(cl -> cl.role == 1)
                .map(cl -> cl.name).findFirst().orElse("");

        String p2Name = clients.stream()
                .filter(cl -> cl.role == 2)
                .map(cl -> cl.name).findFirst().orElse("");

        boolean p1Ready = clients.stream().anyMatch(cl -> cl.role == 1 && cl.ready);
        boolean p2Ready = clients.stream().anyMatch(cl -> cl.role == 2 && cl.ready);

        boolean canStart = p1Present && p2Present && p1Ready && p2Ready;

        String msg = String.join("|",
                "STATE",
                p1Present ? "1" : "0",
                safe(p1Name),
                p1Ready ? "1" : "0",
                p2Present ? "1" : "0",
                safe(p2Name),
                p2Ready ? "1" : "0",
                canStart ? "1" : "0"
        );

        broadcast(msg);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("|", " ");
    }

    /* 메시지 전체 브로드캐스트 */
    private void broadcast(String msg) {
        synchronized (clients) {
            for (Client cl : clients) cl.out.println(msg);
        }
    }

    /* 좌표 브로드캐스트 */
    private void broadcastPositions() {
        broadcast("P1_POS|" + gameState.p1x + "|" + gameState.p1y);
        broadcast("P2_POS|" + gameState.p2x + "|" + gameState.p2y);
    }


    /* ========================= Client 클래스 ========================= */
    private static class Client {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;
        final int role;

        String name = "";
        boolean ready = false;

        Client(Socket s, int role) throws IOException {
            this.socket = s;
            this.role = role;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
        }
    }
}
