package vazkii.quark.base.client.config.screen;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public abstract class AbstractInputScreen<T> extends AbstractQScreen {
	
	Button resetButton, doneButton;
	boolean errored = false;
	
	T val;
	
	public AbstractInputScreen(Screen parent) {
		super(parent);
	}
	
	@Override
	public void render(PoseStack mstack, int mouseX, int mouseY, float pticks) {
		renderBackground(mstack);

		super.render(mstack, mouseX, mouseY, pticks);
	}
	
	@Override
	protected final void init() {
		super.init();
		
		int pad = 3;
		int bWidth = 121;
		int left = (width - (bWidth + pad) * 3) / 2;
		int vStart = height - 30;
		
		addRenderableWidget(new Button(left, vStart, bWidth, 20, new TranslatableComponent("quark.gui.config.default"), this::setDefault));
		addRenderableWidget(resetButton = new Button(left + bWidth + pad, vStart, bWidth, 20, new TranslatableComponent("quark.gui.config.discard"), this::reset));
		addRenderableWidget(doneButton = new Button(left + (bWidth + pad) * 2, vStart, bWidth, 20, new TranslatableComponent("gui.done"), this::save));
		
		onInit();
		update();
	}
	
	abstract void onInit();
	abstract T compute();
	abstract void setDefault();
	abstract void reset();
	abstract boolean isErrored();
	abstract boolean isDirty();
	abstract void commit();
	
	@Override
	public boolean keyPressed(int key, int mouseX, int mouseY) {
		switch(key) {
		case 256: // esc
			reset(null);
			return true;
		case 257: // enter
			if(!errored) {
				save(null);
				return true;
			}
		}
		
		return super.keyPressed(key, mouseX, mouseY);
	}

	final void setDefault(Button button) {
		setDefault();
		update();
		save(button);
	}
	
	final void reset(Button button) {
		reset();
		update();
		save(button);
	}
	
	final void save(Button button) {
		if(!errored) {
			commit();
			returnToParent(button);
		}
	}
	
	void update() {
		val = compute();
		errored = val == null || isErrored();
		
		resetButton.active = errored || isDirty();
		doneButton.active = !errored;
	}

}
