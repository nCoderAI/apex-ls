/*
Copyright (c) 2025 Kevin Jones, All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.
*/

package io.github.apexdevtools.apexls.mcp.tools;

import io.github.apexdevtools.apexls.mcp.bridge.ApexLsBridge;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP tool that explicitly refreshes a single file then returns diagnostics. Bypasses the
 * filesystem indexer and CacheFlusher daemon for sub-200ms validation latency.
 *
 * <p>Use this when you've written/modified a file and need immediate diagnostics without waiting for
 * the background indexer to detect the change (~2-3s). The refresh is high-priority and synchronous,
 * so diagnostics reflect the current file content on disk.
 */
public class ApexRefreshAndDiagnoseTool {

  private static final Logger logger = LoggerFactory.getLogger(ApexRefreshAndDiagnoseTool.class);
  private final ApexLsBridge bridge;

  public ApexRefreshAndDiagnoseTool(ApexLsBridge bridge) {
    this.bridge = bridge;
  }

  public McpServerFeatures.SyncToolSpecification getSpecification() {
    String schema = SchemaBuilder.createRefreshAndDiagnoseSchema();

    Tool tool =
        new Tool(
            "apex_refresh_and_diagnose",
            "Refreshes a specific file in the workspace index then returns all diagnostics. "
                + "Use this instead of sfdx_code_diagnostics when you have just written or modified "
                + "a file and need immediate validation — it triggers a synchronous high-priority "
                + "refresh (~50ms) instead of waiting for the background filesystem indexer (~2-3s).",
            schema);

    return new McpServerFeatures.SyncToolSpecification(tool, this::execute);
  }

  private CallToolResult execute(Object exchange, Map<String, Object> arguments) {
    try {
      String workspace = (String) arguments.get("workspace");
      String filePath = (String) arguments.get("filePath");
      boolean includeWarnings =
          arguments.get("includeWarnings") != null
              ? (Boolean) arguments.get("includeWarnings")
              : false;
      int maxIssuesPerFile =
          arguments.get("maxIssuesPerFile") != null
              ? ((Number) arguments.get("maxIssuesPerFile")).intValue()
              : 100;

      // Validate workspace argument
      CallToolResult validationResult = WorkspaceValidator.validateWorkspace(workspace);
      if (validationResult != null) {
        return validationResult;
      }

      if (filePath == null || filePath.trim().isEmpty()) {
        return new CallToolResult("Error: filePath is required", true);
      }

      // Execute refresh + diagnostics via bridge
      CompletableFuture<String> future =
          bridge.refreshAndDiagnose(workspace, filePath, includeWarnings, maxIssuesPerFile);
      String issuesJson = future.join();

      if (issuesJson != null && !issuesJson.trim().isEmpty()) {
        return new CallToolResult(issuesJson, false);
      } else {
        return new CallToolResult(
            "No issues found after refresh - code analysis passed successfully", false);
      }

    } catch (Exception ex) {
      logger.error("Error during refresh and diagnose", ex);
      return new CallToolResult("Error during refresh and diagnose: " + ex.getMessage(), true);
    }
  }
}
