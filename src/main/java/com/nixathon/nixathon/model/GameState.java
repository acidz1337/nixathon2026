package com.nixathon.nixathon.model;

import java.util.List;
import lombok.Data;

@Data
public class GameState {
  public int gameId;
  public int turn;

  public PlayerTower playerTower;

  public List<EnemyTower> enemyTowers;

  // NEGOTIATION phase
  public List<CombatDeclaration> combatActions;

  // Only present in combat
  public List<DiplomacyMessage> diplomacy;
  public List<PreviousAttack> previousAttacks;
}
