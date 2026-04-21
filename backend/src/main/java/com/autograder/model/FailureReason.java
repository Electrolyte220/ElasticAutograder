package com.autograder.model;

public enum FailureReason {
  NONE,
  TIMEOUT,
  RESOURCE_LIMIT,
  KUBERNETES_ERROR,
  RESULT_PARSE_ERROR,
  CONFIG_ERROR,
  UNKNOWN
}
