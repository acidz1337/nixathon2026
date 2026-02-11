package com.nixathon.nixathon.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CombatAction {
  String type;
  Integer amount;
  Integer targetId;
  Integer troopCount;
}
