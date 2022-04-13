package Server;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.Timer;

/*
* This handler class communicates with the game client, performs all
* game mechanics related actions and sends updated game state to client.
* */
public final class MineSweeperHandler implements Runnable{
  final private Socket socket;
  private final JTextArea output;
  private final MineSweeperBoard board;
  private final int clientNo;
  DataInputStream inputFromClient;
  DataOutputStream outputToClient;
  Connection conn;

  private PreparedStatement getInfo, saveGame, loadGame, getScore, updateRank;
  Timer timer = null;

  public MineSweeperHandler(Socket socket, int clientNo, JTextArea output) {
    this.socket = socket;
    this.output = output;
    this.clientNo = clientNo;
    board = new MineSweeperBoard();

    try {
      conn = DriverManager.getConnection("jdbc:sqlite:resource/minesweeper.db");
      getInfo = conn.prepareStatement("Select info from save where id < 6;");
      saveGame = conn.prepareStatement("Update save Set info = ?, save = ? Where id = ?;");
      loadGame = conn.prepareStatement("Select save from save where id = ?;");
      getScore = conn.prepareStatement("Select name, score from scores where rank < 6;");
      updateRank = conn.prepareStatement("Update scores Set name = ?, score = ? Where rank = ?;");
    } catch (SQLException e) {
      System.err.println("Connection error: " + e);
      System.exit(1);
    }
  }

  /*
  * Commands from game clients:
  * UPDATE: A request is made to interact with the game board.
  * NEW:  Start a new game.
  * GETLOAD:  Client wants to load game, asks the server to get info of saved games.
  * LOADFROM: Client asks server to load the game state with the specified id.
  * GETSAVE:  Client wants to save game, asks the server to get info of saved games.
  * SAVETO: Client asks server to save current game state to the specified id slot.
  * SCORE:  Retrieve top 5 scores for display.
  * EXIT: Client exited the session and disconnects from server.
  * */
  @Override
  public void run() {

    try {
      inputFromClient = new DataInputStream(socket.getInputStream());
      outputToClient = new DataOutputStream(socket.getOutputStream());

      label:
      while (true) {
        String message = inputFromClient.readUTF();
        String[] command = message.split(" ", 2);
        String id = command[0];

        switch (id) {
          case "UPDATE":
            String[] parameters = command[1].split(",");

            byte y = Byte.parseByte(parameters[1]);
            byte x = Byte.parseByte(parameters[2]);

            synchronized (board) {
              if (parameters[0].equals("0"))
                board.reveal(y, x);
              else if (parameters[0].equals("1"))
                board.setFlag(y, x);
              if ((!board.getGameWon()) && board.checkWinCon()) {
                board.setGameWon();
                checkHighScore();
              }
              outputToClient.writeUTF("BOARD\t" + board);
            }
            break;
          case "NEW":
            synchronized (board) {
              board.copyBoard(new MineSweeperBoard());
            }
            if (timer == null) {
              timer = new Timer();
              timer.schedule(new Clock(), 1000, 1000);
            }
            outputToClient.writeUTF("BOARD\t" + board);
            break;
          case "GETLOAD":
            PreparedStatement stld = getInfo;
            ResultSet r = stld.executeQuery();
            StringBuilder s = new StringBuilder();
            while (r.next()) {
              Object o = r.getObject(1);
              s.append(o.toString()).append(",");
            }
            outputToClient.writeUTF("LOADINFO\t" + s);
            r.close();
            break;
          case "LOADFROM":
            PreparedStatement load = loadGame;
            load.setInt(1, Integer.parseInt(command[1]));
            ResultSet game = load.executeQuery();
            game.next();
            Object g = game.getObject(1);
            MineSweeperBoard loadedBoard = null;
            try {
              loadedBoard = new MineSweeperBoard(g.toString());
            } catch (Exception e) {
              output.append("Client " + clientNo + " failed to load game from slot " + command[1] + "!");
            }
            synchronized (board) {
              board.copyBoard(loadedBoard);
            }
            outputToClient.writeUTF("BOARD\t" + board);
            game.close();
            break;
          case "GETSAVE":
            PreparedStatement stmt = getInfo;
            ResultSet rs = stmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
              Object o = rs.getObject(1);
              sb.append(o.toString()).append(",");
            }
            outputToClient.writeUTF("SAVEINFO\t" + sb);
            rs.close();
            break;
          case "SAVETO":
            PreparedStatement save = saveGame;
            save.setString(1, Calendar.getInstance().getTime().toString()
                    + ' ' + board.getTimeRemain() + 's');
            save.setString(2, board.toString());
            save.setInt(3, Integer.parseInt(command[1]));
            save.executeUpdate();
            break;
          case "SCORE":
            PreparedStatement scores = getScore;
            ResultSet ss = scores.executeQuery();
            StringBuilder scoreString = new StringBuilder();
            while (ss.next()) {
              String o1 = ss.getObject(1).toString();
              String o2 = ss.getObject(2).toString();
              String separator = "";
              for (int i = 0; i < 20 - o1.length() - o2.length(); i++)
                separator += " ";
              scoreString.append(o1).append(separator).append(o2).append(",");
            }
            outputToClient.writeUTF("SCORES\t" + scoreString);
            ss.close();
            break;
          case "NAME":
            PreparedStatement getScoreForUpdate = getScore;
            ResultSet ss2 = getScoreForUpdate.executeQuery();
            List<scoreRecord> rank = new ArrayList<>();
            while (ss2.next()) {
              String s1 = ss2.getObject(1).toString();
              String s2 = ss2.getObject(2).toString();
              rank.add(new scoreRecord(s1, Integer.parseInt(s2)));
            }
            String[] newRecord = command[1].split(",", 2);
            rank.add(new scoreRecord(newRecord[0], Integer.parseInt(newRecord[1])));
            rank.sort(Collections.reverseOrder());
            for (int i = 0; i < 5; i++) {
              scoreRecord sr = rank.get(i);
              //System.out.println(sr.name + " " + sr.score);
              updateRank.setString(1, sr.name);
              updateRank.setInt(2, sr.score);
              updateRank.setInt(3, i + 1);
              updateRank.executeUpdate();
            }
            ss2.close();
            break;
          case "EXIT":
            if (timer != null)
              timer.cancel();
            outputToClient.writeUTF("EXIT");
            break label;
        }
      }
    } catch (IOException e) {
      output.append("Client " + clientNo + " disconnected, session ending...");
    } catch (SQLException se) {
      output.append("Client " + clientNo + " failed SQL query");
    }finally {
      //getInfo, saveGame, loadGame, getScore, updateRank
      try {
        output.append("Client " + clientNo + " disconnected");
        getInfo.close();
        saveGame.close();
        loadGame.close();
        getScore.close();
        updateRank.close();
        conn.close();
        socket.close();
      } catch (IOException | SQLException e) {
        e.printStackTrace();
      }
    }
  }

  //check if current winning user's score is good enough to be top 5
  private void checkHighScore() {
    PreparedStatement scores = getScore;
    List<scoreRecord> list = new ArrayList<>();

    try {
      ResultSet ss = scores.executeQuery();
      while (ss.next()) {
        String name = ss.getObject(1).toString();
        int score = Integer.parseInt(ss.getObject(2).toString());
        list.add(new scoreRecord(name, score));
      }
      Collections.sort(list);
      if (board.getTimeRemain() > list.get(0).score) {
        outputToClient.writeUTF("GETNAME");
      }
      ss.close();
    } catch (SQLException | IOException se) {
      se.printStackTrace();
    }
  }

  /*
  * clock that decrease time remain on the current game every second
  * */
  private class Clock extends TimerTask {
    @Override
    public void run() {
      if (board.getTimeRemain() <= 0 || board.getGameLost() || board.getGameWon())
        return;
      synchronized (board) {
        board.decrementTime();
        int time = board.getTimeRemain();
        if (time <= 0) {
          board.setGameLost();
          try {
            outputToClient.writeUTF("BOARD\t" + board);
          } catch (IOException e) {
            System.out.println("Client " + clientNo + " disconnected, session ending...");
          }
          return;
        }
        try {
          outputToClient.writeUTF("TIME\t" + time);
        } catch (IOException e) {
          timer.cancel();
        }
      }
    }
  }

  //Class used for comparing and sorting top scores.
  private static class scoreRecord implements Comparable<scoreRecord>{
    String name;
    int score;

    scoreRecord (String name, int score) {
      this.name = name;
      this.score = score;
    }

    @Override
    public int compareTo(scoreRecord o) {
      return this.score - o.score;
    }
  }
}
