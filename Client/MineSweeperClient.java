package Client;

import Server.MineSweeperBoard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/*
* This game client display the game state to the player and send requests
* to the game server. It does not directly interact with the game .
* */
public final class MineSweeperClient extends JFrame implements Runnable {

  Socket socket;
  private DataOutputStream toServer;
  private DataInputStream fromServer;
  private MineSweeperBoard board = new MineSweeperBoard();

  private JPanel topPanel, midPanel, bottomPanel;
  private JTextField nameField;

  public MineSweeperClient(String title) {
    super(title);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    try {
      socket = new Socket("localhost", 8000);
      fromServer = new DataInputStream(socket.getInputStream());
      toServer = new DataOutputStream(socket.getOutputStream());

      toServer.writeUTF("NEW");
      board = new MineSweeperBoard(fromServer.readUTF().split("\t", 2)[1]);
    } catch (IOException ie) {
      System.err.println("Connectin failed! Make sure JDBC driver is added and Minesweeper Server is running.");
      System.exit(1);
    }

    this.setSize(244, 350);
    setResizable(false);
    createMenus();
    setupClientPanels();
    nameField = new JTextField(10);

    Thread t = new Thread(this);
    t.start();
  }

  private JPanel addMidPanel()
  {
    int Y_AXIS = board.getMines().length;
    int X_AXIS = board.getMines()[0].length;
    JPanel midPanel = new JPanel(new GridLayout(Y_AXIS, X_AXIS));
    byte[][] imgMap = getImg();

    MineSweeperListener listener = new MineSweeperListener();

    for (int i = 0; i < Y_AXIS; i++) {
      for (int j = 0; j < X_AXIS; j++) {
        ImagePanel panel = new ImagePanel("resource/" + Integer.toString(imgMap[i][j]) + ".png", i, j);
        panel.addMouseListener(listener);
        midPanel.add(panel);
      }
    }
    midPanel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
    return midPanel;
  }

  /*Return a mapping of each cell to the image it displays.*/
  private byte[][] getImg()
  {
    int Y_AXIS = board.getMines().length;
    int X_AXIS = board.getMines()[0].length;
    byte[][] result = new byte[Y_AXIS][X_AXIS];
    byte[][] revealed = board.getRevealed();
    boolean[][] mines = board.getMines();

    for (int i = 0; i < Y_AXIS; i++) {
      for (int j = 0; j < X_AXIS; j++) {
        int id = revealed[i][j];
        if (id < 0) {
          if (mines[i][j])
            result[i][j] = 9;
          else {
            byte count = 0;
            for (int y = -1; y <= 1; y++) {
              for (int x = -1; x <= 1; x++) {
                if (!(y == 0 && x == 0)) {
                  if (isValid(i + y, j + x, Y_AXIS, X_AXIS) && mines[i + y][j + x])
                    count++;
                }
              }
            }
            result[i][j] = count;
          }
        }
        else {
          if (id == 0) result[i][j] = 10;
          else if (id == 1) result[i][j] = 11;
          else result[i][j] = 12;
        }
      }
    }
    return result;
  }

  //check if this tile is within boundary of the board.
  private boolean isValid(int y, int x, int Y_AXIS, int X_AXIS) {
    return y >= 0 && y < Y_AXIS && x >= 0 && x < X_AXIS;
  }

  private JPanel addBottomPanel()
  {
    int Y_AXIS = board.getMines().length;
    int X_AXIS = board.getMines()[0].length;
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    int count = 40;
    byte[][] revealed = board.getRevealed();
    for (int i = 0; i < Y_AXIS; i++) {
      for (int j = 0; j < X_AXIS; j++) {
        if (revealed[i][j] > 0)
          count--;
      }
    }
    JLabel mineLabel = new JLabel(Integer.toString(count));
    bottomPanel.add(mineLabel);
    return bottomPanel;
  }

  private JPanel addTopPanel()
  {
    JPanel topPanel = new JPanel();
    JLabel timeLabel = new JLabel("Time Remaining: " + board.getTimeRemain());
    topPanel.add(timeLabel);
    return topPanel;
  }

  private void createMenus() {
    JMenuBar menuBar = new JMenuBar();
    this.setJMenuBar(menuBar);

    JMenu menu = new JMenu("File");

    menu.add(createFileNewItem());
    menu.add(createFileOpenItem());
    menu.add(createFileSaveItem());
    menu.add(createFileExitItem());

    menuBar.add(menu);
    menuBar.add(createHighScoresItem());
  }

  private JMenuItem createHighScoresItem() {

    JMenuItem item = new JMenuItem("Top Scores");
    class MenuItemListener implements ActionListener
    {
      public void actionPerformed(ActionEvent event)
      {
        try {
          toServer.writeUTF("SCORE");
        } catch (IOException e) {
          System.err.println("Failed to start new game!");
          e.printStackTrace();
        }
      }
    }
    ActionListener listener = new MenuItemListener();
    item.addActionListener(listener);
    return item;
  }

  public JMenuItem createFileNewItem()
  {
    JMenuItem item = new JMenuItem("New");
    class MenuItemListener implements ActionListener
    {
      public void actionPerformed(ActionEvent event)
      {
        try {
          toServer.writeUTF("NEW");
        } catch (IOException e) {
          System.err.println("Failed to start new game!");
          e.printStackTrace();
        }
      }
    }
    ActionListener listener = new MenuItemListener();
    item.addActionListener(listener);
    return item;
  }

  public JMenuItem createFileOpenItem()
  {
    JMenuItem item = new JMenuItem("Open");
    class MenuItemListener implements ActionListener
    {
      public void actionPerformed(ActionEvent event)
      {
        try {
          toServer.writeUTF("GETLOAD");
        } catch (IOException e) {
          System.err.println("Failed to load game!");
          e.printStackTrace();
        }
      }
    }
    ActionListener listener = new MenuItemListener();
    item.addActionListener(listener);
    return item;
  }

  public JMenuItem createFileSaveItem()
  {
    JMenuItem item = new JMenuItem("Save");
    class MenuItemListener implements ActionListener
    {
      public void actionPerformed(ActionEvent event)
      {
        try {
          toServer.writeUTF("GETSAVE");
        } catch (IOException e) {
          System.err.println("Failed to save game!");
          e.printStackTrace();
        }
      }
    }
    ActionListener listener = new MenuItemListener();
    item.addActionListener(listener);
    return item;
  }

  public JMenuItem createFileExitItem()
  {
    JMenuItem item = new JMenuItem("Exit");
    class MenuItemListener implements ActionListener
    {
      public void actionPerformed(ActionEvent event) {
        exit();
      }
    }
    ActionListener listener = new MenuItemListener();
    item.addActionListener(listener);
    return item;
  }

  /*
   * A pop up panel for loading games.
   * */
  private void createLoadPanel(String[] loadInfo) {
    Object[] options = new Object[5];
    JButton button;
    for (int i = 0; i < 5; i++) {
      final int position = i + 1;
      button = new JButton(loadInfo[i]);
      button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          try {
            toServer.writeUTF("LOADFROM " + position);
          } catch (IOException ie) {
            ie.printStackTrace();
          }
          closeWindow();
        }
      });
      options[i] = button;
    }
    SwingUtilities.invokeLater(() -> JOptionPane.showOptionDialog(null,
            "Pick a saved game to load", "Load Game",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, options, options[0]));
  }

  /*
  * A pop up panel for saving games.
  * */
  private void createSavePanel(String[] saveInfo) {
    Object[] options = new Object[5];
    JButton button;
    for (int i = 0; i < 5; i++) {
      final int position = i + 1;
      button = new JButton(saveInfo[i]);
      button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          try {
            toServer.writeUTF("SAVETO " + position);
          } catch (IOException ie) {
            ie.printStackTrace();
          }
          closeWindow();
        }
      });
      options[i] = button;
    }
    SwingUtilities.invokeLater(() -> JOptionPane.showOptionDialog(null,
            "Pick a slot to save", "Save Game",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, options, options[0]));
  }

  private void refreshClient() {
    this.remove(topPanel);
    this.remove(midPanel);
    this.remove(bottomPanel);
    setupClientPanels();
    this.revalidate();
    this.repaint();
  }

  private void setupClientPanels() {
    topPanel = addTopPanel();
    midPanel = addMidPanel();
    bottomPanel = addBottomPanel();
    this.add(topPanel, BorderLayout.NORTH);
    this.add(midPanel, BorderLayout.CENTER);
    this.add(bottomPanel, BorderLayout.SOUTH);
  }

  private void refreshTime(String time) {
    this.remove(topPanel);
    topPanel = new JPanel();
    JLabel timeLabel = new JLabel("Time Remaining: " + time);
    topPanel.add(timeLabel);
    this.add(topPanel, BorderLayout.NORTH);
    this.revalidate();
    this.repaint();
  }

  /*
  * Messages sent form the server:
  * TIME: the server updated the time remaining on the current game
  * BOARD: server sent new game state to be displayed
  * LOADINFO: server sent info of the saved games, ready to take request for loading game
  * SAVEINFO: server sent info of the saved games, readt to take request for saving game
  * SCORES: display top 5 scores in a pop up window
  * EXIT: server acknowledges that the game client is about to terminate
  * */
  @Override
  public void run()
  {
    boolean exit = false;
    while (!exit) {
      try {
        String[] command = fromServer.readUTF().split("\t", 2);
        String id = command[0];
        switch (id) {
          case "TIME":
            SwingUtilities.invokeLater(() -> refreshTime(command[1]));
            break;
          case "BOARD":
            board = new MineSweeperBoard(command[1]);
            SwingUtilities.invokeLater(() -> refreshClient());

            if (board.getGameLost())
              SwingUtilities.invokeLater(
                      () -> JOptionPane.showMessageDialog(null, "You lost!"));
            else if (board.getGameWon())
              SwingUtilities.invokeLater(() ->
                      JOptionPane.showMessageDialog(null,
                              "You won! Your score is " + board.getTimeRemain()));
            break;
          case "LOADINFO":
            String[] loadInfo = command[1].split(",");
            createLoadPanel(loadInfo);
            break;
          case "SAVEINFO":
            String[] saveInfo = command[1].split(",");
            createSavePanel(saveInfo);
            break;
          case "SCORES":
            String[] scores = command[1].split(",");
            Runnable r = () -> {
              StringBuilder html = new StringBuilder("<html><body width='%1s'><h3>Top Scores</h3><pre>");
              for (String score : scores) {
                html.append(score).append("<br/>");
              }
              html.append("</pre>");
              int w = 120;
              JOptionPane.showMessageDialog(null, String.format(html.toString(), w));
            };
            SwingUtilities.invokeLater(r);
            break;
          case "GETNAME":
            SwingUtilities.invokeLater(() -> {
                        JPanel namePanel = new JPanel(new BorderLayout());
                        namePanel.add(new JLabel("You achieved a high score! Enter your name:"), BorderLayout.NORTH);
                        nameField.setText(null);
                        namePanel.add(nameField, BorderLayout.CENTER);
                        int okCxl = JOptionPane.showConfirmDialog(null, namePanel, null, JOptionPane.OK_CANCEL_OPTION);
                        if (okCxl == JOptionPane.OK_OPTION) {
                          String name = nameField.getText();
                          try {
                            toServer.writeUTF("NAME " + name + "," + board.getTimeRemain());
                          } catch (IOException e) {
                            e.printStackTrace();
                          }
                        }
                      });
            break;
          case "EXIT":
            exit = true;
            break;
        }
      } catch (EOFException ee) {
        System.err.println("Server not found, session terminate.");
        ee.printStackTrace();
        System.exit(-1);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private class MineSweeperListener implements MouseListener {

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
      if (board.getGameLost() || board.getGameWon())
        return;
      ImagePanel clicked = (ImagePanel) e.getSource();
      int b;
      if (e.getButton() == MouseEvent.BUTTON1)
        b = 0;
      else if (e.getButton() == MouseEvent.BUTTON3)
        b = 1;
      else return;

      int x = clicked.findX();
      int y = clicked.findY();
      try {
        toServer.writeUTF("UPDATE " + b + ',' + y + ',' + x);
      } catch (EOFException | SocketException se) {
        JOptionPane.showMessageDialog(null,"Server connection lost, exiting program.");
        System.exit(1);
      }  catch (IOException ie) {
        ie.printStackTrace();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
  }

  private void exit() {
    try {
      toServer.writeUTF("EXIT");
    } catch (IOException e) {
      System.err.println("Error notifying server");
    }
    System.exit(0);
  }

  //Close all popup windows for the JFrame
  private void closeWindow() {
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      if (window instanceof JDialog) {
        JDialog dialog = (JDialog) window;
        if (dialog.getContentPane().getComponentCount() == 1
                && dialog.getContentPane().getComponent(0) instanceof JOptionPane){
          dialog.dispose();
        }
      }
    }
  }

  public static void main(String[] args) {
    MineSweeperClient mc = new MineSweeperClient("Minesweeper");
    mc.setVisible(true);
  }
}
