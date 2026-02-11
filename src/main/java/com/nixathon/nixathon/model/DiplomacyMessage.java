package com.nixathon.nixathon.model;

import lombok.Data;

@Data
public class DiplomacyMessage {
  public int playerId;
  public DiplomacyAction action;
}
