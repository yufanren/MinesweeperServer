package Server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class MineSweeperServer extends JFrame implements Runnable {

  private int clientNo = 0;
  JTextArea ta;

  public MineSweeperServer() {
    ta = new JTextArea(10,10);
    JScrollPane sp = new JScrollPane(ta);
    this.add(sp);
    this.setTitle("MultiThreadServer");
    this.setSize(400,200);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    Thread t = new Thread(this);
    t.start();
  }

  @Override
  public void run() {

    int MAX_CLIENT_ALLOWED = 5;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_CLIENT_ALLOWED);
    try {
      int port = 8000;
      ServerSocket serverSocket = new ServerSocket(port);
      ta.append("MineSweeperServer started at " + new Date() + '\n');

      while (true) {
        Socket socket = serverSocket.accept();
        clientNo++;
        InetAddress inetAddress = socket.getInetAddress();
        ta.append("Starting thread for client " + clientNo + ", ip address: "
                + inetAddress.getHostAddress() + '\n');

        MineSweeperHandler task = new MineSweeperHandler(socket, clientNo, ta);
        executor.execute(task);
      }
    } catch(IOException ie) {
      ie.printStackTrace();
    } finally {
      executor.shutdown();
    }
  }

  public static void main(String[] args) {
    MineSweeperServer ms = new MineSweeperServer();
    ms.setVisible(true);
  }
}
