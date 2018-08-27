package net.rmelick.hanabi.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.rmelick.hanabi.backend.api.Player;
import net.rmelick.hanabi.backend.api.Tile;
import net.rmelick.hanabi.backend.api.ViewableGameState;
import net.rmelick.hanabi.backend.state.GameState;
import net.rmelick.hanabi.backend.state.PlayerState;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@RestController
@CrossOrigin
public class HanabiController {

  @RequestMapping("/gameState")
  public String getGameState() throws JsonProcessingException {
    ViewableGameState gameState = convertInternalGameState(new GameState(4));
    return new ObjectMapper().writeValueAsString(gameState);

  }

  private ViewableGameState convertInternalGameState(GameState internalGameState) {
    ViewableGameState externalGameState = new ViewableGameState();
    externalGameState.players = convertInternalPlayers(internalGameState.getPlayerStates());
    return externalGameState;
  }

  private List<Player> convertInternalPlayers(List<PlayerState> internalPlayers) {
    List<Player> externalPlayers = new ArrayList<>(internalPlayers.size());
    int currentPlayerIndex = 0;
    for (PlayerState internalPlayer : internalPlayers) {
      Player externalPlayer = new Player();
      externalPlayer.name = internalPlayer.getName();
      externalPlayer.playerIndex = internalPlayer.getPlayerIndex();

      if (internalPlayer.getPlayerIndex() == currentPlayerIndex) {
        externalPlayer.isCurrentPlayer = true;
        externalPlayer.tiles = convertInternalTilesCurrentPlayer(internalPlayer.getTiles());
      } else {
        externalPlayer.isCurrentPlayer = false;
        externalPlayer.tiles = convertInternalTilesFullInfo(internalPlayer.getTiles());
      }
      externalPlayers.add(externalPlayer);
    }
    return externalPlayers;
  }

  private List<Tile> convertInternalTilesFullInfo(List<net.rmelick.hanabi.backend.Tile> internalTiles) {
    List<Tile> externalTiles = new ArrayList<>(internalTiles.size());
    for (net.rmelick.hanabi.backend.Tile internalTile : internalTiles) {
      Tile externalTile = new Tile();
      externalTile.color = internalTile.getColor().getPrettyName();
      externalTile.id = internalTile.getId();
      externalTile.publicId = internalTile.getPublicId();
      externalTile.rank = internalTile.getRank().getValue();
      externalTiles.add(externalTile);
    }
    return externalTiles;
  }

  private List<Tile> convertInternalTilesCurrentPlayer(List<net.rmelick.hanabi.backend.Tile> internalTiles) {
    List<Tile> externalTiles = new ArrayList<>(internalTiles.size());
    for (net.rmelick.hanabi.backend.Tile internalTile : internalTiles) {
      Tile externalTile = new Tile();
      externalTile.publicId = internalTile.getPublicId();
      externalTiles.add(externalTile);
    }
    return externalTiles;
  }
}
