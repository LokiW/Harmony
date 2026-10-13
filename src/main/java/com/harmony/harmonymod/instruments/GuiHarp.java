package com.harmony.harmonymod.instruments;

import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyNetwork;
import com.harmony.harmonymod.sounds.Phrases;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Sheet music for composing the harp's phrases. Each of the 8 columns is a step of the phrase: click a
 * line or space to put a note there (click it again to remove it), several notes in a column make a
 * chord, and right clicking a note toggles a sharp. Tabs along the top pick the slot; the one open
 * when the sheet closes is the one the harp plays.
 *
 * The staff covers the note block range, F#3 to F#5: 15 line and space positions from F3 up to F5.
 */
@SideOnly(Side.CLIENT)
public class GuiHarp extends GuiScreen {
	private static final ResourceLocation TEXTURE = new ResourceLocation(HarmonyMod.MODID, "textures/gui/harp_sheet.png");

	private static final int WIDTH = 232;
	private static final int HEIGHT = 150;
	private static final int COLUMNS = Phrases.MAX_STEPS;
	private static final int POSITIONS = 15;
	// Pixels between a line and the next space
	private static final int STEP_Y = 4;
	private static final int COLUMN_X0 = 50;
	private static final int COLUMN_W = 22;
	private static final int STAFF_TOP = 30;

	private static final int PAPER = 0xFFF1E4C3;
	private static final int PAPER_EDGE = 0xFF8B6B3E;
	private static final int TAB_INACTIVE = 0xFFD9C79D;
	private static final int INK = 0xFF2B1D0E;
	private static final int FAINT = 0xFFC9B48A;
	private static final int GHOST = 0x662B1D0E;

	// Semitones above F3 of the natural note at each position: F G A B C D E F G A B C D E F
	private static final int[] SEMITONES = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24};
	// Positions of B and E, which have no sharp (it would be C or F)
	private static final boolean[] NO_SHARP = new boolean[POSITIONS];
	static {
		NO_SHARP[3] = NO_SHARP[6] = NO_SHARP[10] = NO_SHARP[13] = true;
	}

	private final ItemStack harp;
	// Pitches (0-24) in each column of each slot
	private final List<List<SortedSet<Integer>>> slots = new ArrayList<List<SortedSet<Integer>>>();
	private int active;

	// Preview playback
	private final List<SortedSet<Integer>> preview = new ArrayList<SortedSet<Integer>>();
	private int previewTicks;

	private int left, top;

	public GuiHarp(ItemStack harp) {
		this.harp = harp;
		this.active = ItemHarp.getActive(harp);
		for (int s = 0; s < ItemHarp.SLOTS; s++) {
			List<SortedSet<Integer>> columns = new ArrayList<SortedSet<Integer>>();
			for (int c = 0; c < COLUMNS; c++) {
				columns.add(new TreeSet<Integer>());
			}
			List<SortedSet<Integer>> steps = Phrases.parse(ItemHarp.getSlot(harp, s));
			for (int c = 0; c < steps.size() && c < COLUMNS; c++) {
				for (int note : steps.get(c)) {
					columns.get(c).add(note & 0xFF);
				}
			}
			slots.add(columns);
		}
	}

	@Override
	public void initGui() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		buttonList.clear();
		int y = top + HEIGHT - 26;
		buttonList.add(new GuiButton(0, left + 12, y, 64, 20, StatCollector.translateToLocal("harmony.harp.play")));
		buttonList.add(new GuiButton(1, left + 84, y, 64, 20, StatCollector.translateToLocal("harmony.harp.clear")));
		buttonList.add(new GuiButton(2, left + 156, y, 64, 20, StatCollector.translateToLocal("gui.done")));
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private List<SortedSet<Integer>> columns() {
		return slots.get(active);
	}

	/*
	 * Note block pitch of the natural note at a position. F3 isn't in range, only F#3.
	 */
	private static int natural(int position) {
		return SEMITONES[position] - 1;
	}

	private static int pitch(int position, boolean sharp) {
		return position == 0 ? 0 : natural(position) + (sharp ? 1 : 0);
	}

	/*
	 * Position of a pitch on the staff, and whether it's sharp
	 */
	private static int positionOf(int pitch) {
		for (int p = 0; p < POSITIONS; p++) {
			if (pitch(p, false) == pitch) {
				return p;
			}
		}
		for (int p = 0; p < POSITIONS; p++) {
			if (pitch(p, true) == pitch) {
				return p;
			}
		}
		return 0;
	}

	private static boolean isSharp(int pitch) {
		int p = positionOf(pitch);
		return p == 0 || pitch(p, false) != pitch;
	}

	private int columnX(int column) {
		return left + COLUMN_X0 + column * COLUMN_W;
	}

	private int positionY(int position) {
		return top + STAFF_TOP + (POSITIONS - 1 - position) * STEP_Y;
	}

	/*
	 * Column and position under the mouse, or null
	 */
	private int[] spotAt(int mouseX, int mouseY) {
		int column = Math.round((mouseX - left - COLUMN_X0) / (float) COLUMN_W);
		int position = Math.round((top + STAFF_TOP + (POSITIONS - 1) * STEP_Y - mouseY) / (float) STEP_Y);
		if (column < 0 || column >= COLUMNS || position < 0 || position >= POSITIONS
				|| Math.abs(mouseX - columnX(column)) > 9 || Math.abs(mouseY - positionY(position)) > 2) {
			return null;
		}
		return new int[] {column, position};
	}

	/*
	 * The note at a spot (natural or sharp), or -1
	 */
	private int noteAt(int column, int position) {
		for (int pitch : columns().get(column)) {
			if (positionOf(pitch) == position) {
				return pitch;
			}
		}
		return -1;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);

		// Slot tabs
		for (int s = 0; s < ItemHarp.SLOTS; s++) {
			int x = left + 8 + s * 30;
			if (mouseX >= x && mouseX < x + 26 && mouseY >= top - 12 && mouseY < top + 2) {
				active = s;
				return;
			}
		}

		int[] spot = spotAt(mouseX, mouseY);
		if (spot == null) {
			return;
		}
		SortedSet<Integer> column = columns().get(spot[0]);
		int existing = noteAt(spot[0], spot[1]);
		if (button == 0) {
			if (existing >= 0) {
				column.remove(existing);
			} else {
				int pitch = pitch(spot[1], false);
				column.add(pitch);
				playNote(pitch);
			}
		} else if (button == 1 && existing >= 0 && spot[1] != 0 && !NO_SHARP[spot[1]]) {
			column.remove(existing);
			int pitch = pitch(spot[1], !isSharp(existing));
			column.add(pitch);
			playNote(pitch);
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			preview.clear();
			for (SortedSet<Integer> column : columns()) {
				if (!column.isEmpty()) {
					preview.add(column);
				}
			}
			previewTicks = 0;
		} else if (button.id == 1) {
			for (SortedSet<Integer> column : columns()) {
				column.clear();
			}
		} else if (button.id == 2) {
			mc.displayGuiScreen(null);
		}
	}

	@Override
	public void updateScreen() {
		if (!preview.isEmpty() && previewTicks++ % HarpPlayer.STEP_TICKS == 0) {
			for (int pitch : preview.remove(0)) {
				playNote(pitch);
			}
		}
	}

	/*
	 * Full volume at the player, as loud as really playing it (vanilla's GUI sounds are quiet)
	 */
	private void playNote(int pitch) {
		mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation("note.harp"), 1.0F,
				Phrases.soundPitch(pitch), (float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ));
	}

	/*
	 * Save onto the harp when the sheet closes
	 */
	@Override
	public void onGuiClosed() {
		List<List<SortedSet<Integer>>> steps = new ArrayList<List<SortedSet<Integer>>>();
		String[] phrases = new String[ItemHarp.SLOTS];
		for (int s = 0; s < ItemHarp.SLOTS; s++) {
			List<SortedSet<Integer>> slotSteps = new ArrayList<SortedSet<Integer>>();
			for (SortedSet<Integer> column : slots.get(s)) {
				if (!column.isEmpty()) {
					slotSteps.add(column);
				}
			}
			steps.add(slotSteps);
			phrases[s] = phraseKey(slotSteps);
		}
		HarmonyNetwork.channel.sendToServer(new HarpUpdateMessage(steps, active));
		// Update the client's copy too, so the tooltip is right before the server's update arrives
		ItemHarp.setSlots(harp, phrases, active);
	}

	private static String phraseKey(List<SortedSet<Integer>> pitchSteps) {
		List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
		for (SortedSet<Integer> pitches : pitchSteps) {
			SortedSet<Integer> step = new TreeSet<Integer>();
			for (int pitch : pitches) {
				step.add(Phrases.note(Phrases.HARP, pitch));
			}
			steps.add(step);
		}
		return Phrases.key(steps);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();

		// Slot tabs, the active one joined to the sheet
		for (int s = 0; s < ItemHarp.SLOTS; s++) {
			int x = left + 8 + s * 30;
			boolean on = s == active;
			drawRect(x - 1, top - 13, x + 27, top + (on ? 1 : 0), PAPER_EDGE);
			drawRect(x, top - 12, x + 26, top + (on ? 1 : 0), on ? PAPER : TAB_INACTIVE);
			String label = String.valueOf(s + 1);
			fontRendererObj.drawString(label, x + 13 - fontRendererObj.getStringWidth(label) / 2, top - 9, INK);
		}

		// Parchment
		drawRect(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, PAPER_EDGE);
		drawRect(left, top, left + WIDTH, top + HEIGHT, PAPER);
		if (active >= 0) {
			int x = left + 8 + active * 30;
			drawRect(x, top - 1, x + 26, top + 1, PAPER);
		}

		// Staff lines (E4 G4 B4 D5 F5), and faint guides for the ledger lines below it
		int staffLeft = left + 12, staffRight = left + WIDTH - 12;
		for (int p = 6; p <= 14; p += 2) {
			drawRect(staffLeft, positionY(p), staffRight, positionY(p) + 1, INK);
		}
		for (int c = 0; c < COLUMNS; c++) {
			for (int p = 0; p <= 4; p += 2) {
				drawRect(columnX(c) - 6, positionY(p), columnX(c) + 7, positionY(p) + 1, FAINT);
			}
		}
		// Bar line at the end, and faint separators between steps
		drawRect(staffRight - 1, positionY(14), staffRight, positionY(6) + 1, INK);
		for (int c = 1; c < COLUMNS; c++) {
			int x = columnX(c) - COLUMN_W / 2;
			drawRect(x, positionY(14), x + 1, positionY(6) + 1, FAINT);
		}

		// Treble clef
		mc.getTextureManager().bindTexture(TEXTURE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_BLEND);
		drawTexturedModalRect(left + 16, positionY(15) - 4, 0, 0, 16, 44);

		// Notes
		for (int c = 0; c < COLUMNS; c++) {
			for (int pitch : columns().get(c)) {
				int p = positionOf(pitch);
				drawNote(columnX(c), positionY(p), INK);
				if (isSharp(pitch)) {
					fontRendererObj.drawString("#", columnX(c) - 10, positionY(p) - 3, INK);
				}
			}
		}

		// Where a click would put a note
		int[] spot = spotAt(mouseX, mouseY);
		if (spot != null && noteAt(spot[0], spot[1]) < 0) {
			drawNote(columnX(spot[0]), positionY(spot[1]), GHOST);
		}

		// The phrase as animals hear it
		List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
		for (SortedSet<Integer> column : columns()) {
			if (!column.isEmpty()) {
				steps.add(column);
			}
		}
		String phrase = steps.isEmpty() ? StatCollector.translateToLocal("harmony.harp.tooltip.empty") : phraseKey(steps);
		phrase = fontRendererObj.trimStringToWidth(phrase, WIDTH - 24);
		fontRendererObj.drawString(phrase, left + 12, positionY(0) + 10, PAPER_EDGE);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawNote(int x, int y, int color) {
		drawRect(x - 3, y - 1, x + 4, y + 2, color);
		drawRect(x - 2, y - 2, x + 3, y + 3, color);
	}
}
