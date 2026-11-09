package io.github.andi005.qram.inventory;

/** A scan arrived for a session that is already finished. The API answers 409 Conflict. */
public class SessionFinishedException extends RuntimeException {

  public SessionFinishedException(long sessionId) {
    super("Inventory session " + sessionId + " is finished");
  }
}
