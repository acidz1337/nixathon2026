package com.nixathon.nixathon.model;

import lombok.Data;

@Data
public class CombatDeclaration {
  public int playerId;
  public AttackAction action;
}