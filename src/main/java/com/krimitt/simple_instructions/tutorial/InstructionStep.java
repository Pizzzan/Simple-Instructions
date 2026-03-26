package com.krimitt.simple_instructions.tutorial;

public class InstructionStep {
	private String id;
	private String title;
	private String description;
	private ActionType actionType;
	private String targetKey;
	private int requiredCount;
	private String icon;

	public InstructionStep() {
	}

	public InstructionStep(String id, String title, String description, ActionType actionType, String targetKey, int requiredCount, String icon) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.actionType = actionType;
		this.targetKey = targetKey;
		this.requiredCount = requiredCount;
		this.icon = icon;
	}

	public String getId() { return id; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public ActionType getActionType() { return actionType; }
	public String getTargetKey() { return targetKey; }
	public int getRequiredCount() { return requiredCount; }
	public String getIcon() { return icon; }

	public void setId(String id) { this.id = id; }
	public void setTitle(String title) { this.title = title; }
	public void setDescription(String description) { this.description = description; }
	public void setActionType(ActionType actionType) { this.actionType = actionType; }
	public void setTargetKey(String targetKey) { this.targetKey = targetKey; }
	public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
	public void setIcon(String icon) { this.icon = icon; }

	public InstructionStep copy() {
		return new InstructionStep(id, title, description, actionType, targetKey, requiredCount, icon);
	}
}
