package tonius.simplyjetpacks.client.handler.deprecated;

import tonius.simplyjetpacks.config.Config;
import tonius.simplyjetpacks.handler.SyncHandler;
import tonius.simplyjetpacks.network.PacketHandler;
import tonius.simplyjetpacks.network.message.MessageKeyboardSync;
import tonius.simplyjetpacks.setup.ModKey;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class KeyHandler
{
	static final Minecraft mc = Minecraft.getMinecraft();
	static final List<SJKeyBinding> keyBindings = new ArrayList<SJKeyBinding>();

	public static final SJKeyBinding keyTogglePrimary = new SJKeyBinding("toggle.primary", Keyboard.KEY_F, ModKey.TOGGLE_PRIMARY);
	public static final SJKeyBinding keyToggleSecondary = new SJKeyBinding("toggle.secondary", Keyboard.KEY_NONE, ModKey.TOGGLE_SECONDARY);
	public static final SJKeyBinding keyModePrimary = new SJKeyBinding("mode.primary", Keyboard.KEY_C, ModKey.MODE_PRIMARY);
	public static final SJKeyBinding keyModeSecondary = new SJKeyBinding("mode.secondary", Keyboard.KEY_NONE, ModKey.MODE_SECONDARY);
	public static final SJKeyBinding keyOpenPackGUI = new SJKeyBinding("openPackGUI", Keyboard.KEY_U, ModKey.OPEN_PACK_GUI);

	private static int flyKey;
	private static int descendKey;
	private static boolean lastFlyState = false;
	private static boolean lastDescendState = false;
	private static boolean lastForwardState = false;
	private static boolean lastBackwardState = false;
	private static boolean lastLeftState = false;
	private static boolean lastRightState = false;

	public static void updateCustomKeybinds(String flyKeyName, String descendKeyName)
	{
		flyKey = Keyboard.getKeyIndex(flyKeyName);
		descendKey = Keyboard.getKeyIndex(descendKeyName);
	}

	private static void tickStart()
	{
		if(mc.thePlayer != null)
		{
			boolean flyState;
			boolean descendState;
			if(Config.customControls)
			{
				flyState = mc.inGameHasFocus && Keyboard.isKeyDown(flyKey);
				descendState = mc.inGameHasFocus && Keyboard.isKeyDown(descendKey);
			}
			else
			{
				flyState = mc.gameSettings.keyBindJump.isKeyDown();
				descendState = mc.gameSettings.keyBindSneak.isKeyDown();
			}

			boolean forwardState = mc.gameSettings.keyBindForward.isKeyDown();
			boolean backwardState = mc.gameSettings.keyBindBack.isKeyDown();
			boolean leftState = mc.gameSettings.keyBindLeft.isKeyDown();
			boolean rightState = mc.gameSettings.keyBindRight.isKeyDown();

			if(flyState != lastFlyState || descendState != lastDescendState || forwardState != lastForwardState || backwardState != lastBackwardState || leftState != lastLeftState || rightState != lastRightState)
			{
				lastFlyState = flyState;
				lastDescendState = descendState;

				lastForwardState = forwardState;
				lastBackwardState = backwardState;
				lastLeftState = leftState;
				lastRightState = rightState;
				PacketHandler.instance.sendToServer(new MessageKeyboardSync(flyState, descendState, forwardState, backwardState, leftState, rightState));
				SyncHandler.processKeyUpdate(mc.thePlayer, flyState, descendState, forwardState, backwardState, leftState, rightState);
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(ClientTickEvent evt)
	{
		if(evt.phase == Phase.START)
		{
			tickStart();
		}
	}

	@SubscribeEvent
	public void onKey(InputEvent evt)
	{
		if(!mc.inGameHasFocus)
		{
			return;
		}
		for(SJKeyBinding key : keyBindings)
		{
			if(key.isPressed())
			{
				key.handleKeyPress();
			}
		}
	}
}