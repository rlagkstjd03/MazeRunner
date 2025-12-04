package maze_game;

public class Player {
    //플레이어 위치
    private int x;
    private int y;
    private int [][]maze;
    public enum Direction{
        UP, DOWN, RIGHT, LEFT
    }

    private Direction direction;

    public Player(int startX, int startY, int [][]maze){
        this.x = startX;
        this.y = startY;
        this.maze = maze;
        this.direction= Direction.DOWN; //초기값
    }

    private boolean canMoveTo(int nx, int ny){
        if(ny<0 || ny>=maze.length){
            return false;
        }

        if(nx<0 || nx>=maze[0].length){
            return false;
        }

        if(maze[ny][nx]==0){
            return false;
        }

        return true;
    }

    public void mvUP(){
        direction = Direction.UP; //방향 바꾸기

        int nx = x;
        int ny = y-1;

        if(canMoveTo(nx, ny)){
            y = ny;
        }
    }

    public void mvDOWN(){
        direction = Direction.DOWN; //방향 바꾸기

        int nx = x;
        int ny = y+1;

        if(canMoveTo(nx, ny)){
            y = ny;
        }
    }

    public void mvRIGHT(){
        direction = Direction.RIGHT; //방향 바꾸기

        int nx = x+1;
        int ny = y;

        if(canMoveTo(nx, ny)){
            x = nx;
        }
    }

    public void mvLEFT(){
        direction = Direction.LEFT; //방향 바꾸기

        int nx = x-1;
        int ny = y;

        if(canMoveTo(nx, ny)){
            x = nx;
        }
    }

    public void setPosition(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){return x;}
    public int getY(){return y;}
    public Direction getDirection(){return direction;}
}
