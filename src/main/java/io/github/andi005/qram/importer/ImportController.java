package io.github.andi005.qram.importer;

import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** The CSV import page (ADMIN only): upload a file, see the report. Files are never stored. */
@Controller
@RequestMapping("/import")
class ImportController {

  private final ImportService importService;

  ImportController(ImportService importService) {
    this.importService = importService;
  }

  @GetMapping
  String form(Model model) {
    model.addAttribute("batches", importService.recentBatches());
    return "import/form";
  }

  @PostMapping
  String upload(@RequestParam("file") MultipartFile file, Model model) throws IOException {
    model.addAttribute(
        "report", importService.importCsv(file.getOriginalFilename(), file.getBytes()));
    model.addAttribute("batches", importService.recentBatches());
    return "import/form";
  }
}
