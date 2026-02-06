package com.example.todo;

public enum Priority {
  HIGH("High", "text-bg-danger"),
  MEDIUM("Medium", "text-bg-warning"),
  LOW("Low", "text-bg-secondary");

  private final String label;
  private final String cssClass;

  Priority(String label, String cssClass) {
    this.label = label;
    this.cssClass = cssClass;
  }

  public String getLabel() {
    return label;
  }

  public String getCssClass() {
    return cssClass;
  }
}
