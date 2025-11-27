package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class MazeRunnerServer {
    private static final int PORT = 5000;

    private final List<Client> clients = Collections.synchronizedList(new ArrayList<>(2));
    private final GameState gameState = new GameState();  // ★ 추가

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

    private void handle(Client c) {
        try {
            c.out.println("ROLE " + c.role);
            broadcastState();

            String line;
            while ((line = c.in.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("JOIN ")) {
                    c.name = line.substring(5).trim();
                    broadcastState();
                }

                else if (line.startsWith("READY ")) {
                    c.ready = "1".equals(line.substring(6).trim());
                    broadcastState();
                }

                else if (line.equals("START")) {
                    if (c.role == 1 &&
                            clients.size() == 2 &&
                            clients.get(0).ready &&
                            clients.get(1).ready) {

                        System.out.println("[Server] Generating maze...");
                        gameState.generateMaze();

                        // ★ 미로 전송
                        broadcast(gameState.mazeToString());

                        // ★ 출구 전송
                        broadcast("EXIT|" + gameState.getExitX() + "|" + gameState.getExitY());

                        // ★ 게임 시작 신호
                        broadcast("START");

                        System.out.println("[Server] Maze + START sent.");
                    }
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

    private void broadcastState() {
        boolean p1Present = clients.stream().anyMatch(cl -> cl.role == 1);
        boolean p2Present = clients.stream().anyMatch(cl -> cl.role == 2);

        String p1Name = clients.stream().filter(cl -> cl.role == 1).map(cl -> cl.name).findFirst().orElse("");
        String p2Name = clients.stream().filter(cl -> cl.role == 2).map(cl -> cl.name).findFirst().orElse("");

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

    private String safe(String s) { return s == null ? "" : s.replace("|", " "); }

    private void broadcast(String msg) {
        synchronized (clients) {
            for (Client cl : clients) cl.out.println(msg);
        }
    }

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
