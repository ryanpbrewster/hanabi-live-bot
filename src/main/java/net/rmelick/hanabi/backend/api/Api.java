package net.rmelick.hanabi.backend.api;

import net.rmelick.hanabi.backend.Clue;

/**
 *
 */
public interface Api {
  // if you don't have a gameid, you get it back somehow
  Session joinGame(String gameId);

  // all of the methods below require your session to be in an active game already
  void startGame();

  void giveClue(int toPlayer, Clue clue);

  void play(int positionToPlay);

  void discard(int positionToDiscard);

  ViewableGameState getGameState();
}