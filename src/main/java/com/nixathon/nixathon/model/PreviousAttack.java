package com.nixathon.nixathon.model;

import lombok.Data;

@Data
public class PreviousAttack {
  public int playerId;
  public AttackAction action;
}
