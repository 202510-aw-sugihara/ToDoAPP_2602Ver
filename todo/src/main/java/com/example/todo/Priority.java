package com.example.todo;

public enum Priority {
  HIGH("高", "text-bg-danger"),
  MEDIUM("中", "text-bg-warning"),
  LOW("低", "text-bg-secondary");

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
