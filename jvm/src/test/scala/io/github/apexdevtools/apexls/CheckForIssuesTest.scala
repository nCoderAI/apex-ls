package io.github.apexdevtools.apexls

import org.scalatest.funsuite.AnyFunSuite

class CheckForIssuesTest extends AnyFunSuite {

  test("exit status is issues when errors are present") {
    assert(CheckForIssues.exitStatus(hasErrors = true, hasWarnings = false, hasUnused = false) == 4)
    assert(CheckForIssues.exitStatus(hasErrors = true, hasWarnings = true, hasUnused = true) == 4)
  }

  test("exit status is warning-only when warnings are present without errors or unused") {
    assert(CheckForIssues.exitStatus(hasErrors = false, hasWarnings = true, hasUnused = false) == 5)
  }

  test("exit status is unused-only when unused issues are present without errors or warnings") {
    assert(CheckForIssues.exitStatus(hasErrors = false, hasWarnings = false, hasUnused = true) == 6)
  }

  test("exit status is warnings-and-unused when both are present without errors") {
    assert(CheckForIssues.exitStatus(hasErrors = false, hasWarnings = true, hasUnused = true) == 7)
  }

  test("exit status is ok when no errors, warnings, or unused issues are present") {
    assert(
      CheckForIssues.exitStatus(hasErrors = false, hasWarnings = false, hasUnused = false) == 0
    )
  }
}
