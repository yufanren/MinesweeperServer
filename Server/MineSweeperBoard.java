package Server;

public final class MineSweeperBoard {
  /*states for revealed:
  -1: revealed;
  0:  hidden;
  1:  hidden flagged;
  2:  hidden flag crossed;
   */
  private boolean [][] mines;
  private byte [][] revealed;
  private int timeRemain;
  private boolean GAME_WON = false;
  private boolean GAME_LOST = false;

  public MineSweeperBoard() {
    int y_SIZE = 16;
    int x_SIZE = 16;
    int numMines = 40;

    timeRemain = 1000;
    mines = new boolean[y_SIZE][x_SIZE];
    revealed = new byte[y_SIZE][x_SIZE];
    //lay down numMines numbers of mines randomly on the board.
    while (numMines > 0) {
      int x = (int) Math.floor(Math.random() * x_SIZE);
      int y = (int) Math.floor(Math.random() * y_SIZE);
      if (!mines[y][x]) {
        mines[y][x] = true;
        numMines--;
      }
    }
  }

  /*construct a MineSweeperBoard from a string.
    String is generated using toString() method.
   */
  public MineSweeperBoard(String board) {
    String[] fields = board.split(",");
    timeRemain = Integer.parseInt(fields[0]);
    GAME_WON = fields[1].equals("t");
    GAME_LOST = fields[2].equals("t");

    String[] mineRows = fields[3].split(" ");
    String[] revealedRows = fields[4].split(" ");
    mines = new boolean[mineRows.length][mineRows[0].length()];
    revealed = new byte[mineRows.length][mineRows[0].length()];

    for (int i = 0; i < mines.length; i++) {
      for (int j = 0; j < mines[0].length; j++) {
        char mine = mineRows[i].charAt(j);
        if (mine == '1')
          mines[i][j] = true;
        char reveal = revealedRows[i].charAt(j);
        switch (reveal) {
          case '_':
            revealed[i][j] = -1;
            break;
          case 'F':
            revealed[i][j] = 1;
            break;
          case 'X':
            revealed[i][j] = 2;
            break;
        }
      }
    }
  }

  /*
  Make copy of a Board.
  */
  public void copyBoard(MineSweeperBoard board) {
    if (board == null)
      return;
    this.mines = board.mines;
    this.revealed = board.revealed;
    this.timeRemain = board.timeRemain;
    this.GAME_WON = board.GAME_WON;
    this.GAME_LOST = board.GAME_LOST;
  }

  public boolean[][] getMines() {
    return mines;
  }

  public byte[][] getRevealed() {
    return revealed;
  }

  public boolean getGameWon() {
    return GAME_WON;
  }

  public boolean getGameLost() {
    return GAME_LOST;
  }

  public int getTimeRemain() {
    return timeRemain;
  }

  void setGameLost() {
    GAME_LOST = true;
    lose();
  }

  void setGameWon() {
    GAME_WON = true;
  }

  void decrementTime() {
    if (timeRemain > 0) timeRemain--;
  }

  /*
  * If revealed cell is empty, try to reveal adjacent cells that
  * are also empty. Position (y, x) called on cascade should have
   * no mine on or next to it and is already revealed.
  * */
  private void cascade(int row, int col) {
    int numRows = mines.length;
    int numCols = mines[0].length;
    for (int y = -1; y <= 1; y++) {
      for (int x = -1; x <= 1; x++) {
        int yPos = row + y;
        int xPos = col + x;
        if (isValid(yPos, xPos, numRows, numCols) && revealed[yPos][xPos] >= 0) {
          revealed[yPos][xPos] = -1;
          if (getMineCount(yPos, xPos) == 0)
            cascade(yPos, xPos);
        }
      }
    }
  }

  //Attempts to flag a tile
  void setFlag(int y, int x) {
    if (revealed[y][x] == 2)
      revealed[y][x] = 0;
    else if (revealed[y][x] >= 0)
      revealed[y][x]++;
  }

  //Attempts to reveal a tile
  void reveal(int y, int x) {
    if (revealed[y][x] == 0) {
      revealed[y][x]--;
      if (mines[y][x])
        setGameLost();
      else if (getMineCount(y, x) == 0)
        cascade(y, x);
    }
  }

  //Definitely reveal all bomb tiles, for when losing the game
  void lose() {
    for (int i = 0; i < revealed.length; i++) {
      for (int j = 0; j < revealed[0].length; j++) {
        if (mines[i][j])
          revealed[i][j] = -1;
      }
    }
  }

  //Convert the board state to a string.
  @Override
  public String toString() {
    return String.valueOf(timeRemain) + ',' + (GAME_WON ? 't' : 'f') + ',' +
            (GAME_LOST ? 't' : 'f') + ',' + minesToString() + ',' + revealedToString();
  }

  //convert the mines board to a string.
  private String minesToString() {
    StringBuilder result = new StringBuilder();
    for (boolean[] mine : mines) {
      for (int j = 0; j < mines[0].length; j++) {
        result.append(mine[j] ? '1' : '0');
      }
      result.append(' ');
    }
    return result.toString();
  }

  //convert the revealed board to a string.
  private String revealedToString() {
    StringBuilder result = new StringBuilder();
    for (byte[] bytes : revealed) {
      for (int j = 0; j < revealed[0].length; j++) {
        byte state = bytes[j];
        switch (state) {
          case -1:
            result.append('_');
            break;
          case 0:
            result.append('O');
            break;
          case 1:
            result.append('F');
            break;
          case 2:
            result.append('X');
            break;
        }
      }
      result.append(' ');
    }
    return result.toString();
  }

  //check if the current board state qualify for winning the game.
  boolean checkWinCon() {
    for (byte i = 0; i < revealed.length; i++) {
      for (byte j = 0; j < revealed[0].length; j++) {
        if (revealed[i][j] >= 0 && !mines[i][j])
          return false;
      }
    }
    return true;
  }

  //Count the number of mines surrounding this tile.
  private int getMineCount(int y, int x) {
    int numRows = mines.length;
    int numCols = mines[0].length;
    int count = 0;
    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        if (!(i == 0 && j == 0) && isValid(y + i, x + j, numRows, numCols) && mines[y + i][x + j])
          count++;
      }
    }
    return count;
  }

  //check if this tile is within boundary of the board.
  private boolean isValid(int y, int x, int Y_AXIS, int X_AXIS) {
    return y >= 0 && y < Y_AXIS && x >= 0 && x < X_AXIS;
  }
}
