package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;

public class MatchMakerMessage extends ClientMessage {

  public String mod;
  public String state;

  public MatchMakerMessage(ClientMessageType command) {
    super(command);
  }

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
    this.mod = mod;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
