package maze_game;

import Client.GamePanel;

import javax.swing.*;
import java.awt.*;

public class MazeGame extends JFrame {

    public MazeGame() {
        setTitle("미로 생성 테스트");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 미로 생성
        long seed = System.currentTimeMillis();
        MakeMaze make = new MakeMaze(41, 31, seed);
        int[][] maze = make.make();

        //미로 렌더링용 클라이언트 패널
        GamePanel panel = new GamePanel();
        panel.setMaze(maze);                // 미로 적용
        panel.setExit(make.getExitX(), make.getExitY());  // 출구 표시

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        panel.requestFocusInWindow(); // 패널에 포커스
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MazeGame::new);
    }
}
