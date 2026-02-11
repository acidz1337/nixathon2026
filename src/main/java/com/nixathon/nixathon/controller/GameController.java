package com.nixathon.nixathon.controller;

import com.nixathon.nixathon.model.CombatAction;
import com.nixathon.nixathon.model.CombatDeclaration;
import com.nixathon.nixathon.model.DiplomacyAction;
import com.nixathon.nixathon.model.EnemyTower;
import com.nixathon.nixathon.model.GameState;
import com.nixathon.nixathon.model.PreviousAttack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

  // Store current allies across turns
  private final Set<Integer> allies = new HashSet<>();

  @PostMapping("/negotiate")
  public List<DiplomacyAction> negotiate(@RequestBody GameState state) {
    List<DiplomacyAction> diplomacy = new ArrayList<>();

    // --- Step 1: Identify biggest threat ---
    EnemyTower biggestThreat = state.enemyTowers.stream()
        .max(Comparator.comparingInt(e -> e.level))
        .orElse(null);

    if (biggestThreat == null) return diplomacy;

    // --- Step 2: Find an enemy attacking the biggest threat (possible ally) ---
    EnemyTower allyCandidate = state.enemyTowers.stream()
        .filter(e -> e.playerId != biggestThreat.playerId)
        .filter(e -> state.combatActions != null && state.combatActions.stream()
            .anyMatch(c -> c.playerId == e.playerId && c.action != null
                && c.action.targetId == biggestThreat.playerId))
        .min(Comparator.comparingInt(e -> e.hp)) // prefer weaker ally
        .orElse(null);

    if (allyCandidate != null) {
      DiplomacyAction peace = new DiplomacyAction();
      peace.setAllyId(allyCandidate.playerId);
      diplomacy.add(peace);

      // Store ally in memory for combat
      allies.add(allyCandidate.playerId);
    }

    return diplomacy;

//    List<DiplomacyAction> diplomacy = new ArrayList<>();
//
//    // Example: pick enemy who has lowest HP as temporary ally
//    EnemyTower allyCandidate = state.enemyTowers.stream()
//        .min(Comparator.comparingInt(e -> e.hp))
//        .orElse(null);
//
//    if (allyCandidate != null) {
//      DiplomacyAction peace = new DiplomacyAction();
//      peace.setAllyId(allyCandidate.playerId);
//      diplomacy.add(peace);
//
//      // Store ally in memory for combat
//      allies.add(allyCandidate.playerId);
//    }
//
//    return diplomacy;
  }

  @PostMapping("/combat")
  public List<CombatAction> combat(@RequestBody GameState state) {

    // Clean dead allies
    allies.removeIf(id -> state.enemyTowers.stream()
        .noneMatch(e -> e.playerId == id && e.hp > 0));

    return smartCombatt(state, allies);
  }

  public List<CombatAction> smartCombat(GameState state, Set<Integer> allies) {

    List<CombatAction> actions = new ArrayList<>();

    int myId = state.playerTower.playerId;
    int currentArmor = state.playerTower.armor;
    int resources = state.playerTower.resources;
    int level = state.playerTower.level;
    int turn = state.turn;

    // --- 1️⃣ Determine if late-game ---
    long aliveEnemies = state.enemyTowers.stream()
        .filter(e -> e.hp > 0 && e.playerId != myId)
        .count();

    boolean ignoreAllies = (aliveEnemies <= 1 || turn > 25);
    // ignore alliances if only 1 enemy left or fatigue phase

    // --- 2️⃣ Calculate incoming damage ---
    int incomingDamage = 0;
    if (state.previousAttacks != null) {
      for (PreviousAttack attack : state.previousAttacks) {
        if (attack.action != null && attack.action.targetId == myId) {
          incomingDamage += attack.action.troopCount;
        }
      }
    }

    // Optional: add fatigue damage after turn 25
    if (turn > 25) {
      int fatigueDamage = (turn - 25) * 2; // example scaling
      incomingDamage += fatigueDamage;
    }

    // --- 3️⃣ Armor logic ---
    int armorToBuild = Math.max(0, incomingDamage - currentArmor);
    armorToBuild = Math.min(armorToBuild, resources);
    if (armorToBuild > 0) {
      CombatAction armor = new CombatAction();
      armor.setType("armor");
      armor.setAmount(armorToBuild);
      actions.add(armor);
      resources -= armorToBuild;
    }

    // --- 4️⃣ Upgrade logic ---
    int upgradeCost = (int) Math.round(50 * Math.pow(1.75, level - 1));
    if (resources >= upgradeCost) {
      CombatAction upgrade = new CombatAction();
      upgrade.setType("upgrade");
      actions.add(upgrade);
      resources -= upgradeCost;
    }

    // --- 5️⃣ Attack logic ---
    List<EnemyTower> targets = state.enemyTowers.stream()
        .filter(e -> e.playerId != myId)
        .filter(e -> ignoreAllies || allies == null || !allies.contains(e.playerId))
        .filter(e -> e.hp > 0)
        .sorted(Comparator.comparingInt((EnemyTower e) -> e.hp)
            .thenComparingInt(e -> e.level)) // weakest HP first
        .collect(Collectors.toList());

    for (EnemyTower enemy : targets) {
      if (resources <= 0) break;

      // Only send enough troops to kill, or use remaining resources
      int troopCount = Math.min(resources, enemy.hp + enemy.armor);
      if (troopCount <= 0) continue;

      CombatAction attack = new CombatAction();
      attack.setType("attack");
      attack.setTargetId(enemy.playerId);
      attack.setTroopCount(troopCount);

      actions.add(attack);
      resources -= troopCount;
    }

    return actions;
  }


  public List<CombatAction> smartCombatt(GameState state, Set<Integer> allies) {

    List<CombatAction> actions = new ArrayList<>();

    int myId = state.playerTower.playerId;
    int currentArmor = state.playerTower.armor;
    int resources = state.playerTower.resources;
    int level = state.playerTower.level;
    int turn = state.turn;

    // --- 1️⃣ Late-game & 1v1 detection ---
    long aliveEnemies = state.enemyTowers.stream()
        .filter(e -> e.hp > 0 && e.playerId != myId)
        .count();
    boolean ignoreAllies = (aliveEnemies <= 1 || turn > 25);
    boolean oneVsOne = aliveEnemies == 1;

    // --- 2️⃣ Calculate incoming damage ---
    int incomingDamage = 0;
    if (state.previousAttacks != null) {
      for (PreviousAttack attack : state.previousAttacks) {
        if (attack.action != null && attack.action.targetId == myId) {
          incomingDamage += attack.action.troopCount;
        }
      }
    }

    // Optional: fatigue damage after turn 25
    if (turn > 25) {
      int fatigueDamage = (turn - 25) * 2; // example scaling
      incomingDamage += fatigueDamage;
    }

    // --- 3️⃣ Armor logic ---
    // Only build armor if threat significant or not 1v1
    if (!oneVsOne && incomingDamage > currentArmor / 2) {
      int armorToBuild = Math.max(0, incomingDamage - currentArmor);
      armorToBuild = Math.min(armorToBuild, resources);
      if (armorToBuild > 0) {
        CombatAction armor = new CombatAction();
        armor.setType("armor");
        armor.setAmount(armorToBuild);
        actions.add(armor);
        resources -= armorToBuild;
      }
    }

    // --- 4️⃣ Upgrade logic ---
    int upgradeCost = (int) Math.round(50 * Math.pow(1.75, level - 1));
    if (resources >= upgradeCost && (oneVsOne || resources - upgradeCost > 10)) {
      CombatAction upgrade = new CombatAction();
      upgrade.setType("upgrade");
      actions.add(upgrade);
      resources -= upgradeCost;
    }

    // --- 5️⃣ Attack logic ---
    List<EnemyTower> targets = state.enemyTowers.stream()
        .filter(e -> e.playerId != myId)
        .filter(e -> ignoreAllies || allies == null || !allies.contains(e.playerId))
        .filter(e -> e.hp > 0)
        .sorted(Comparator.<EnemyTower>comparingInt(
                e -> e.hp + estimateIncomingDamage(e, state)) // predicted threat
            .thenComparingInt(e -> -e.level)) // higher level first if tie
        .collect(Collectors.toList());

    for (EnemyTower enemy : targets) {
      if (resources <= 0) break;

      int troopCount = Math.min(resources, enemy.hp + enemy.armor);
      if (troopCount <= 0) continue;

      CombatAction attack = new CombatAction();
      attack.setType("attack");
      attack.setTargetId(enemy.playerId);
      attack.setTroopCount(troopCount);

      actions.add(attack);
      resources -= troopCount;
    }

    return actions;
  }

  // Helper to estimate enemy threat based on previous attacks
  private int estimateIncomingDamage(EnemyTower enemy, GameState state) {
    if (state.previousAttacks == null) return 0;
    return state.previousAttacks.stream()
        .filter(a -> a.playerId == enemy.playerId)
        .mapToInt(a -> a.action != null ? a.action.troopCount : 0)
        .sum();
  }
}
