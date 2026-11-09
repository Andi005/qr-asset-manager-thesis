package io.github.andi005.qram.history;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The full audit trail, newest first, 50 lines per page. */
@Controller
class HistoryController {

  private final ChangeLogService changeLog;

  HistoryController(ChangeLogService changeLog) {
    this.changeLog = changeLog;
  }

  @GetMapping("/history")
  String history(@RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute("entries", changeLog.recent(page, 50));
    return "history/list";
  }
}
