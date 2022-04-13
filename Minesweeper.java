public class Minesweeper {
/*
* This program is only for the convenience of player.
*
* To open additional clients, run Client.MineSweeperClient directly.
*
* In this program, exiting the game client will close the server as well.
* If this is not desired behavior, run Server.MineSweeperServer directly.
* */
  public static void main (String[] args) {
    Server.MineSweeperServer.main(null);
    Client.MineSweeperClient.main(null);
  }
}
