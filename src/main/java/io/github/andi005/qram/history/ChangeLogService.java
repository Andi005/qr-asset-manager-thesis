package io.github.andi005.qram.history;

import io.github.andi005.qram.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the change log. {@link #record} must be called inside the same transaction as
 * the change itself (propagation MANDATORY), so a change and its log line are saved together or not
 * at all.
 */
@Service
public class ChangeLogService {

  private static final int MAX_DETAILS = 4000;

  private final ChangeLogRepository entries;
  private final CurrentUser currentUser;
  private final Clock clock;

  public ChangeLogService(ChangeLogRepository entries, CurrentUser currentUser, Clock clock) {
    this.entries = entries;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(EntityType type, Long id, String action, String details) {
    String text = details;
    if (text != null && text.length() > MAX_DETAILS) {
      text = text.substring(0, MAX_DETAILS - 1) + "…";
    }
    entries.save(
        new ChangeLogEntry(type, id, action, currentUser.username(), clock.instant(), text));
  }

  @Transactional(readOnly = true)
  public List<ChangeLogEntry> forEntity(EntityType type, Long id) {
    return entries.findByEntityTypeAndEntityIdOrderByAtDescIdDesc(type, id);
  }

  @Transactional(readOnly = true)
  public Page<ChangeLogEntry> recent(int page, int size) {
    return entries.findAllByOrderByAtDescIdDesc(PageRequest.of(Math.max(page, 0), size));
  }
}
