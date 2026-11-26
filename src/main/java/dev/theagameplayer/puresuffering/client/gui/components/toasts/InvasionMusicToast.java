package dev.theagameplayer.puresuffering.client.gui.components.toasts;

import java.util.List;

import dev.theagameplayer.puresuffering.invasion.InvasionDifficulty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

public final class InvasionMusicToast implements Toast {
	private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
	private static final MutableComponent MUSIC_TEXT = Component.translatable("puresuffering.toast.music");
	private final ItemStack recordItem;
	private final MutableComponent name;
	private final int color1, color2;

	public InvasionMusicToast(final String pName, final InvasionDifficulty pDifficulty) {
		final Minecraft mc = Minecraft.getInstance();
		final Item[] records = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(item -> {
			return JukeboxSong.fromStack(item.getDefaultInstance()).isPresent();
		}).toArray(Item[]::new);
		this.recordItem = records.length == 0 ? ItemStack.EMPTY : new ItemStack(records[mc.level.getRandom().nextInt(records.length)]);
		this.name = Component.literal(pName);
		this.color1 = pDifficulty.getColor(true);
		this.color2 = pDifficulty.getColor(false);
	}

	private Visibility visibility=Visibility.SHOW;
    public Visibility getWantedVisibility(){return visibility;}
    public void update(ToastManager manager,long time){visibility=time>=5000.0D*manager.getNotificationDisplayTimeMultiplier()?Visibility.HIDE:Visibility.SHOW;}
    public void extractRenderState(final GuiGraphicsExtractor pGuiGraphics, final Font font, final long pTimeSinceLastVisible) {
		pGuiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
		if (pTimeSinceLastVisible < 1500L) {
			final int i = Mth.floor(Mth.clamp((float)(1500L - pTimeSinceLastVisible) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
			pGuiGraphics.text(font, MUSIC_TEXT, 30, 11, this.color1 | i, false);
		} else {
			final List<FormattedCharSequence> list = font.split(this.name, 125);
			final int i = Mth.floor(Mth.clamp((float)(pTimeSinceLastVisible - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
            int l = this.height()/2 - list.size() * 9/2;
            for (final FormattedCharSequence formattedCharSequence : list) {
                pGuiGraphics.text(font, formattedCharSequence, 30, l, this.color2 | i, false);
            	l += 9;
            }
		}
		pGuiGraphics.fakeItem(this.recordItem, 8, 8);
	}
}
