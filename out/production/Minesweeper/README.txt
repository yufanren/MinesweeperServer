1.  Please add sqlite-jdbc-3.34.0.jar to external library before start.

2.  In this minesweeper game, the server plays the game.
Client only visualize the game state and communicate with the server.
Server allows 5 concurrent clients by default.

3.  Minesweeper.java is for convenience in starting the game.
To open additional clients, run Client.MineSweeperClient directly.

4.  Exiting the 1st game client will close the server as well.
If this is not desired behavior, run Server.MineSweeperServer directly.

5.  For easier grading:
saved game slot 4 is 2 steps from winning.
saved game slot 5 is about to run out of time.
