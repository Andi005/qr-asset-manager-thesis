import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scores System Usability Scale (SUS) questionnaires (Brooke, 1996).
 *
 * <p>Usage (no build needed, Java 25 runs a single source file directly):
 *
 * <pre>
 *   java tools/SusScore.java evaluation/data/sus.csv
 *   java tools/SusScore.java --self-test
 * </pre>
 *
 * <p>Input: a CSV file with the header {@code participant,q1,...,q10} and one row per participant,
 * answers 1-5. Separator {@code ,} or {@code ;}.
 *
 * <p>Scoring: odd items contribute (answer − 1), even items (5 − answer); the sum × 2.5 gives a
 * score from 0 to 100. Around 68 is average. It is not a percentage.
 */
public class SusScore {

  record Result(String participant, double score) {}

  public static void main(String[] args) throws IOException {
    if (args.length == 1 && args[0].equals("--self-test")) {
      selfTest();
      return;
    }
    if (args.length != 1) {
      System.err.println("Usage: java tools/SusScore.java <sus.csv> | --self-test");
      System.exit(2);
    }
    List<Result> results = scoreFile(Path.of(args[0]));
    if (results.isEmpty()) {
      System.err.println("No participants found in " + args[0]);
      System.exit(1);
    }
    double sum = 0;
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (Result r : results) {
      System.out.printf(Locale.ROOT, "%-8s %6.1f%n", r.participant(), r.score());
      sum += r.score();
      min = Math.min(min, r.score());
      max = Math.max(max, r.score());
    }
    double mean = sum / results.size();
    double variance = 0;
    for (Result r : results) {
      variance += Math.pow(r.score() - mean, 2);
    }
    double sd = results.size() > 1 ? Math.sqrt(variance / (results.size() - 1)) : 0;
    System.out.println("--------------------");
    System.out.printf(Locale.ROOT, "n = %d%n", results.size());
    System.out.printf(Locale.ROOT, "mean = %.1f   sd = %.1f   min = %.1f   max = %.1f%n",
        mean, sd, min, max);
  }

  static List<Result> scoreFile(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    List<Result> results = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) { // line 0 is the header
      String line = lines.get(i).strip();
      if (line.isEmpty()) {
        continue;
      }
      String[] cells = line.split("[,;]");
      if (cells.length != 11) {
        throw new IllegalArgumentException(
            "Line " + (i + 1) + ": expected participant + 10 answers, found " + cells.length);
      }
      int[] answers = new int[10];
      for (int q = 0; q < 10; q++) {
        answers[q] = Integer.parseInt(cells[q + 1].strip());
      }
      try {
        results.add(new Result(cells[0].strip(), score(answers)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    return results;
  }

  /** The SUS score (0-100) for ten answers in questionnaire order. */
  static double score(int[] answers) {
    if (answers.length != 10) {
      throw new IllegalArgumentException("SUS needs exactly 10 answers");
    }
    int sum = 0;
    for (int i = 0; i < 10; i++) {
      int a = answers[i];
      if (a < 1 || a > 5) {
        throw new IllegalArgumentException("answer " + (i + 1) + " must be 1-5, was " + a);
      }
      boolean odd = (i + 1) % 2 == 1;
      sum += odd ? a - 1 : 5 - a;
    }
    return sum * 2.5;
  }

  private static void selfTest() {
    check(score(new int[] {5, 1, 5, 1, 5, 1, 5, 1, 5, 1}) == 100.0, "best answers give 100");
    check(score(new int[] {1, 5, 1, 5, 1, 5, 1, 5, 1, 5}) == 0.0, "worst answers give 0");
    check(score(new int[] {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}) == 50.0, "all neutral gives 50");
    // Worked example: odd items 3+3+3+4+3 = 16, even items 3+4+3+4+3 = 17, total 33 × 2.5 = 82.5
    check(score(new int[] {4, 2, 4, 1, 4, 2, 5, 1, 4, 2}) == 82.5, "worked example gives 82.5");
    boolean refused = false;
    try {
      score(new int[] {0, 1, 1, 1, 1, 1, 1, 1, 1, 1});
    } catch (IllegalArgumentException e) {
      refused = true;
    }
    check(refused, "answers outside 1-5 are refused");
    System.out.println("SusScore self-test: all checks passed.");
  }

  private static void check(boolean ok, String what) {
    if (!ok) {
      throw new AssertionError("Self-test failed: " + what);
    }
  }
}
