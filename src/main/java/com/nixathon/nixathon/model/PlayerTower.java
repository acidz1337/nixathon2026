package com.nixathon.nixathon.model;

import lombok.Data;

@Data
public class PlayerTower {
  public int playerId;
  public int hp;
  public int armor;
  public int resources;
  public int level;
}