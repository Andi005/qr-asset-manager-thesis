package io.github.andi005.qram.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import io.github.andi005.qram.IntegrationTest;
import io.github.andi005.qram.TestData;
import io.github.andi005.qram.asset.Asset;
import io.github.andi005.qram.employee.Employee;
import io.github.andi005.qram.history.ChangeLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * "All or nothing": when writing the change-log line fails, the assignment must be undone too.
 * Not @Transactional on purpose: the service's own transaction must commit or roll back for real.
 * The change log is replaced by a mock that fails on "ASSIGNED".
 */
@IntegrationTest
class AssignmentRollbackTest {

  @Autowired private AssignmentService assignmentService;
  @Autowired private AssignmentRepository assignments;
  @Autowired private TestData testData;
  @MockitoBean private ChangeLogService changeLog;

  @AfterEach
  void logout() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("A failing change-log write rolls back the assignment")
  void failingLogRollsBackAssignment() {
    TestData.loginAsAdmin();
    Asset asset = testData.asset();
    Employee employee = testData.employee();
    doThrow(new IllegalStateException("log is down"))
        .when(changeLog)
        .record(any(), anyLong(), eq("ASSIGNED"), anyString());

    assertThatThrownBy(() -> assignmentService.assign(asset.getId(), employee.getId(), null))
        .hasMessage("log is down");

    assertThat(assignments.findByAssetIdAndReturnedAtIsNull(asset.getId())).isEmpty();
  }
}
