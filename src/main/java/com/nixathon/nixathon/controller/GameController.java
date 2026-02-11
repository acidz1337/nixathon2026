package com.nixathon.nixathon.controller;

import com.nixathon.nixathon.model.CombatAction;
import com.nixathon.nixathon.model.CombatDeclaration;
import com.nixathon.nixathon.model.DiplomacyAction;
import com.nixathon.nixathon.model.GameState;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

  @PostMapping("/negotiate")
  public List<DiplomacyAction> negotiate(@RequestBody GameState state) {

    System.out.println(state);
    return List.of();
  }

  @PostMapping("/combat")
  public List<CombatAction> combat(@RequestBody GameState state) {

    System.out.println(state);
    return armorThenUpgrade(state);
  }

  public List<CombatAction> armorOnly(GameState state) {

    int myId = state.playerTower.playerId;
    int currentArmor = state.playerTower.armor;
    int resources = state.playerTower.resources;

    int incomingDamage = 0;

    if (state.combatActions != null) {
      for (CombatDeclaration c : state.combatActions) {
        if (c.action != null && c.action.targetId == myId) {
          incomingDamage += c.action.troopCount;
        }
      }
    }

    // Rule 1 & 2
    if (incomingDamage == 0 || currentArmor >= incomingDamage) {
      return List.of();
    }

    // Rule 3
    int armorNeeded = incomingDamage - currentArmor;
    int armorToBuild = Math.min(armorNeeded, resources);

    if (armorToBuild <= 0) {
      return List.of();
    }

    CombatAction action = new CombatAction();
    action.setType("armor");
    action.setAmount(armorToBuild);

    return List.of(action);
  }

  public List<CombatAction> armorThenUpgrade(GameState state) {

    List<CombatAction> actions = new ArrayList<>();

    int myId = state.playerTower.playerId;
    int currentArmor = state.playerTower.armor;
    int resources = state.playerTower.resources;
    int level = state.playerTower.level;

    // 1️⃣ Calculate incoming damage
    int incomingDamage = 0;
    if (state.combatActions != null) {
      for (CombatDeclaration c : state.combatActions) {
        if (c.action != null && c.action.targetId == myId) {
          incomingDamage += c.action.troopCount;
        }
      }
    }

    // 2️⃣ Armor logic (same as before)
    int armorToBuild = Math.max(0, incomingDamage - currentArmor);
    armorToBuild = Math.min(armorToBuild, resources);
    if (armorToBuild > 0) {
      CombatAction armor = new CombatAction();
      armor.setType("armor");
      armor.setAmount(armorToBuild);
      actions.add(armor);
      resources -= armorToBuild;
    }

    // 3️⃣ Upgrade logic
    int upgradeCost = (int) Math.round(50 * Math.pow(1.75, level - 1));
    if (resources >= upgradeCost) {
      CombatAction upgrade = new CombatAction();
      upgrade.setType("upgrade");
      actions.add(upgrade);
    }

    return actions;
  }

}
