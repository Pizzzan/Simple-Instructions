package com.krimitt.simple_instructions.tutorial;

import com.krimitt.simple_instructions.SimpleInstructions;
import com.krimitt.simple_instructions.mixin.client.KeyBindingAccessor;
import com.krimitt.simple_instructions.tutorial.ActionType;
import com.krimitt.simple_instructions.tutorial.InstructionStep;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InstructionManager {
	public static final InstructionManager INSTANCE = new InstructionManager();

	private static final int INITIAL_DELAY_TICKS = 60;

	private List<InstructionStep> steps = Collections.emptyList();
	private String currentSetId = "default";
	private int currentIndex = -1;
	private AnimationState state = AnimationState.IDLE;
	private int progressCount = 0;
	private long stateStartTime = 0;
	private boolean active = false;
	private int delayTicks = 0;
	private boolean pendingStart = false;
	private SoundInstance currentSound = null;
	private boolean wasKeyDown = false;

	private boolean testMode = false;
	private boolean testSingleStep = false;
	private Runnable onTestComplete = null;

	private InstructionManager() {
	}

	public void onWorldJoin() {
		if (!ModConfig.isTutorialEnabled() || !ModConfig.isShowOnWorldJoin()) return;

		Set<String> completed = CompletionPersistence.loadCompleted();
		String setId = "default";
		if (completed.contains(setId)) {
			return;
		}
		List<InstructionStep> loaded = ModConfig.getSteps();
		if (loaded.isEmpty()) {
			return;
		}
		this.steps = new ArrayList<>(loaded);
		this.currentSetId = setId;

		int savedIndex = CompletionPersistence.loadCurrentStep();
		if (savedIndex > 0 && savedIndex < loaded.size()) {
			this.currentIndex = savedIndex;
		} else {
			this.currentIndex = 0;
		}

		this.delayTicks = INITIAL_DELAY_TICKS;
		this.pendingStart = true;
		this.testMode = false;
		this.testSingleStep = false;
	}

	public void startTest(int fromStep, boolean singleStep, Runnable onComplete) {
		List<InstructionStep> loaded = ModConfig.getSteps();
		if (loaded.isEmpty() || fromStep < 0 || fromStep >= loaded.size()) return;

		abort();
		this.steps = new ArrayList<>(loaded);
		this.currentSetId = "test";
		this.testMode = true;
		this.testSingleStep = singleStep;
		this.onTestComplete = onComplete;
		this.currentIndex = fromStep;
		this.progressCount = 0;
		this.wasKeyDown = false;
		this.active = true;
		this.pendingStart = false;
		transitionTo(AnimationState.SLIDING_IN);
	}

	public void abort() {
		if (!active && !pendingStart) return;
		active = false;
		pendingStart = false;
		state = AnimationState.IDLE;
		Runnable cb = onTestComplete;
		testMode = false;
		testSingleStep = false;
		onTestComplete = null;
		if (cb != null) cb.run();
	}

	public void skipAll() {
		if (!active) return;
		active = false;
		state = AnimationState.DONE;
		if (!testMode) {
			CompletionPersistence.markCompleted(currentSetId);
			CompletionPersistence.saveCurrentStep(0); //clear saved progress
		}
		Runnable cb = onTestComplete;
		testMode = false;
		testSingleStep = false;
		onTestComplete = null;
		if (cb != null) cb.run();
	}

	public void tick() {
		if (pendingStart) {
			delayTicks--;
			if (delayTicks <= 0) {
				pendingStart = false;
				startFromCurrentIndex();
			}
			return;
		}

		if (!active) return;

		long now = Util.getMeasuringTimeMs();
		long elapsed = now - stateStartTime;

		switch (state) {
			case SLIDING_IN -> {
				if (elapsed >= ModConfig.getRevealDurationMs()) {
					transitionTo(AnimationState.WAITING);
				}
			}
			case WAITING -> {
				tickWaiting();
			}
			case COMPLETING -> {
				if (elapsed >= ModConfig.getCompletingDurationMs()) {
					transitionTo(AnimationState.SLIDING_OUT);
				}
			}
			case SLIDING_OUT -> {
				if (elapsed >= ModConfig.getDismissDurationMs()) {
					advanceOrFinish();
				}
			}
			default -> {}
		}
	}

	private void tickWaiting() {
		if (currentIndex < 0 || currentIndex >= steps.size()) return;
		InstructionStep step = steps.get(currentIndex);

		ActionType type = step.getActionType();

		if (type == ActionType.WAIT_DURATION) {
			progressCount++;
			if (progressCount >= step.getRequiredCount()) {
				playCompletionSound();
				transitionTo(AnimationState.COMPLETING);
			}
			return;
		}

		if (type == ActionType.DISMISS) {
			tickDismiss();
			return;
		}

		String targetKey = step.getTargetKey();
		boolean isDown;

		if (targetKey.startsWith("key.")) {
			InputUtil.Key inputKey = InputUtil.fromTranslationKey(targetKey);
			long window = MinecraftClient.getInstance().getWindow().getHandle();
			if (inputKey.getCategory() == InputUtil.Type.MOUSE) {
				isDown = GLFW.glfwGetMouseButton(window, inputKey.getCode()) == GLFW.GLFW_PRESS;
			} else {
				isDown = InputUtil.isKeyPressed(window, inputKey.getCode());
			}
		} else {
			KeyBinding key = KeyBindResolver.resolve(targetKey);
			if (key == null) return;
			isDown = isKeyDown(key);
		}

		if (type == ActionType.KEY_PRESS) {
			if (isDown && !wasKeyDown) {
				progressCount++;
			}
		} else if (type == ActionType.KEY_HOLD) {
			if (isDown) {
				progressCount++;
			}
		}

		wasKeyDown = isDown;

		if (progressCount >= step.getRequiredCount()) {
			playCompletionSound();
			transitionTo(AnimationState.COMPLETING);
		}
	}

	private boolean isKeyDown(KeyBinding keyBinding) {
		InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
		long window = MinecraftClient.getInstance().getWindow().getHandle();
		if (boundKey.getCategory() == InputUtil.Type.KEYSYM) {
			return InputUtil.isKeyPressed(window, boundKey.getCode());
		} else if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(window, boundKey.getCode()) == GLFW.GLFW_PRESS;
		}
		return false;
	}

	private void tickDismiss() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null) return;
		long window = client.getWindow().getHandle();

		boolean anyInput = false;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_SPACE)
			|| InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_ENTER)
			|| InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_E)
			|| GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {
			anyInput = true;
		}

		if (anyInput && !wasKeyDown) {
			playCompletionSound();
			transitionTo(AnimationState.COMPLETING);
		}
		wasKeyDown = anyInput;
	}

	private void advanceOrFinish() {
		if (testSingleStep) {
			active = false;
			state = AnimationState.DONE;
			Runnable cb = onTestComplete;
			testMode = false;
			testSingleStep = false;
			onTestComplete = null;
			if (cb != null) cb.run();
			return;
		}

		if (currentIndex + 1 < steps.size()) {
			currentIndex++;
			progressCount = 0;
			wasKeyDown = false;
			transitionTo(AnimationState.SLIDING_IN);
			if (!testMode) {
				CompletionPersistence.saveCurrentStep(currentIndex);
			}
		} else {
			transitionTo(AnimationState.DONE);
			active = false;
			if (!testMode) {
				CompletionPersistence.markCompleted(currentSetId);
				CompletionPersistence.saveCurrentStep(0); //clear saved progress
			}
			SimpleInstructions.LOGGER.info("Tutorial '{}' completed", currentSetId);
			Runnable cb = onTestComplete;
			testMode = false;
			testSingleStep = false;
			onTestComplete = null;
			if (cb != null) cb.run();
		}
	}

	private void startFromCurrentIndex() {
		progressCount = 0;
		wasKeyDown = false;
		active = true;
		transitionTo(AnimationState.SLIDING_IN);
		SimpleInstructions.LOGGER.info("Starting tutorial '{}' from step {}", currentSetId, currentIndex);
	}

	private void transitionTo(AnimationState newState) {
		this.state = newState;
		this.stateStartTime = Util.getMeasuringTimeMs();
	}

	private void playCompletionSound() {
		if (!ModConfig.isEnableCompletionSound()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (currentSound != null) {
			client.getSoundManager().stop(currentSound);
		}
		float volume = ModConfig.getSoundVolume() / 100f;
		currentSound = PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, volume);
		client.getSoundManager().play(currentSound);
	}

	public boolean isActive() { return active; }
	public boolean isTestMode() { return testMode; }
	public AnimationState getState() { return state; }

	public InstructionStep getCurrentStep() {
		if (currentIndex >= 0 && currentIndex < steps.size()) {
			return steps.get(currentIndex);
		}
		return null;
	}

	public int getCurrentIndex() { return currentIndex; }
	public int getTotalSteps() { return steps.size(); }

	public float getProgress() {
		if (currentIndex < 0 || currentIndex >= steps.size()) return 0f;
		InstructionStep step = steps.get(currentIndex);
		if (step.getRequiredCount() <= 0) return 1f;
		return Math.min(1f, (float) progressCount / step.getRequiredCount());
	}

	public float getAnimationProgress() {
		long elapsed = Util.getMeasuringTimeMs() - stateStartTime;
		float raw;
		switch (state) {
			case SLIDING_IN -> {
				raw = Math.min(1f, (float) elapsed / ModConfig.getRevealDurationMs());
				return raw;
			}
			case SLIDING_OUT -> {
				raw = Math.min(1f, (float) elapsed / ModConfig.getDismissDurationMs());
				return easeInCubic(raw);
			}
			case WAITING, COMPLETING -> {
				return 1.0f;
			}
			default -> {
				return 0.0f;
			}
		}
	}

	private static float easeInCubic(float t) {
		return t * t * t;
	}
}
