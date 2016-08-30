package tonius.simplyjetpacks.client.handler;

import tonius.simplyjetpacks.SimplyJetpacks;
import tonius.simplyjetpacks.client.audio.SoundJetpack;
import tonius.simplyjetpacks.config.Config;
import tonius.simplyjetpacks.handler.SyncHandler;
import tonius.simplyjetpacks.item.rewrite.ItemJetpack;
import tonius.simplyjetpacks.item.rewrite.Jetpack;
import tonius.simplyjetpacks.setup.ParticleType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Iterator;

public class ClientTickHandler
{
	private static final Minecraft mc = Minecraft.getMinecraft();
	private static ParticleType lastJetpackState = null;
	private static boolean wearingJetpack = false;
	private static boolean sprintKeyCheck = false;

	private static Field sprintToggleTimer = null;

	private static final int numItems = Jetpack.values().length;

	public ClientTickHandler() {
		try {
			sprintToggleTimer = ReflectionHelper.findField(EntityPlayerSP.class,  "sprintToggleTimer", "field_71156_d");
		}

		catch (Exception e) {
			SimplyJetpacks.logger.error("Unable to find field \"sprintToggleTimer\"");
			e.printStackTrace();
		}
	}

	private static void tickStart()
	{
		if(mc.thePlayer == null)
		{
			return;
		}

		ParticleType jetpackState = null;
		ItemStack armor = mc.thePlayer.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
		if(armor != null && armor.getItem() instanceof ItemJetpack)
		{
			int i = MathHelper.clamp_int(armor.getItemDamage(), 0, numItems - 1);
			Jetpack jetpack = Jetpack.getTypeFromMeta(i);
			if(jetpack != null)
			{
				jetpackState = jetpack.getDisplayParticleType(armor, (ItemJetpack) armor.getItem(), mc.thePlayer);
			}
			wearingJetpack = true;
		}
		else
		{
			wearingJetpack = false;
		}

		if(jetpackState != lastJetpackState)
		{
			lastJetpackState = jetpackState;
			SyncHandler.processJetpackUpdate(mc.thePlayer.getEntityId(), jetpackState);
		}
	}

	private static void tickEnd() throws IllegalAccessException {
		if(mc.thePlayer == null || mc.theWorld == null)
		{
			return;
		}

		if(!mc.isGamePaused())
		{
			Iterator<Integer> itr = SyncHandler.getJetpackStates().keySet().iterator();
			int currentEntity;
			while(itr.hasNext())
			{
				currentEntity = itr.next();
				Entity entity = mc.theWorld.getEntityByID(currentEntity);
				if(entity == null || !(entity instanceof EntityLivingBase) || entity.dimension != mc.thePlayer.dimension)
				{
					itr.remove();
				}
				else
				{
					ParticleType particle = SyncHandler.getJetpackStates().get(currentEntity);
					if(particle != null)
					{
						if(entity.isInWater() && particle != ParticleType.NONE)
						{
							particle = ParticleType.BUBBLE;
						}
						SimplyJetpacks.proxy.showJetpackParticles(mc.theWorld, (EntityLivingBase) entity, particle);
						if(Config.jetpackSounds && !SoundJetpack.isPlayingFor(entity.getEntityId()))
						{
							Minecraft.getMinecraft().getSoundHandler().playSound(new SoundJetpack((EntityLivingBase) entity));
						}
					}
					else
					{
						itr.remove();
					}
				}
			}
		}

		if(sprintKeyCheck && mc.thePlayer.movementInput.moveForward < 1.0F)
		{
			sprintKeyCheck = false;
		}

		if(!Config.doubleTapSprintInAir || !wearingJetpack || mc.thePlayer.onGround || mc.thePlayer.isSprinting() || mc.thePlayer.isHandActive() || mc.thePlayer.isPotionActive(MobEffects.POISON))
		{
			return;
		}

		if(!sprintKeyCheck && sprintToggleTimer != null && mc.thePlayer.movementInput.moveForward >= 1.0F && !mc.thePlayer.isCollidedHorizontally && (mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying))
		{
			if (sprintToggleTimer.getInt(mc.thePlayer) <= 0 && !mc.gameSettings.keyBindSprint.isKeyDown()) {
				sprintToggleTimer.setInt(mc.thePlayer, 7);
                sprintKeyCheck = true;
            }
            else {
				mc.thePlayer.setSprinting(true);
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
		else
		{
			try {
				tickEnd();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}