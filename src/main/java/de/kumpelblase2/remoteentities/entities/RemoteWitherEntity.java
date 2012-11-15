package de.kumpelblase2.remoteentities.entities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.RemoteEntityHandle;
import de.kumpelblase2.remoteentities.api.RemoteProjectileType;
import de.kumpelblase2.remoteentities.api.events.RemoteEntityTouchEvent;
import de.kumpelblase2.remoteentities.api.features.InventoryFeature;
import de.kumpelblase2.remoteentities.api.thinking.InteractBehavior;
import de.kumpelblase2.remoteentities.api.thinking.Mind;
import de.kumpelblase2.remoteentities.api.thinking.PathfinderGoalSelectorHelper;
import de.kumpelblase2.remoteentities.api.thinking.TouchBehavior;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireAttackNearest;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireAttackTarget;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLookAtNearest;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLookRandomly;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireRangedAttack;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireSwell;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireWanderAround;
import de.kumpelblase2.remoteentities.utilities.ReflectionUtil;
import net.minecraft.server.DamageSource;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityWither;
import net.minecraft.server.World;

public class RemoteWitherEntity extends EntityWither implements RemoteEntityHandle
{
	private RemoteEntity m_remoteEntity;
	protected int m_maxHealth;
	public static int defaultMaxHealth = 300;
	protected int m_lastBouncedId;
	protected long m_lastBouncedTime;

	static
	{
		ReflectionUtil.registerEntityType(RemoteWitchEntity.class, "Wither", 10);
	}
	
	public RemoteWitherEntity(World world)
	{
		this(world, null);
	}
	
	public RemoteWitherEntity(World world, RemoteEntity inEntity)
	{
		super(world);
		this.m_remoteEntity = inEntity;
		this.m_maxHealth = defaultMaxHealth;
		new PathfinderGoalSelectorHelper(this.goalSelector).clearGoals();
		new PathfinderGoalSelectorHelper(this.targetSelector).clearGoals();
	}
	
	@Override
	public Inventory getInventory()
	{
		if(!this.m_remoteEntity.getFeatures().hasFeature(InventoryFeature.class))
			return null;
		
		return ((InventoryFeature)this.m_remoteEntity.getFeatures().getFeature(InventoryFeature.class)).getInventory();
	}
	
	@Override
	public void setupStandardGoals()
	{
		try
		{
			Mind mind = this.getRemoteEntity().getMind();
			mind.addMovementDesire(new DesireSwell(this.getRemoteEntity()), 0);
			mind.addMovementDesire(new DesireRangedAttack(this.getRemoteEntity(), RemoteProjectileType.FIREBALL, 40), 2);
			mind.addMovementDesire(new DesireWanderAround(this.getRemoteEntity()), 5);
			mind.addMovementDesire(new DesireLookAtNearest(this.getRemoteEntity(), EntityHuman.class, 8F), 6);
			mind.addMovementDesire(new DesireLookRandomly(this.getRemoteEntity()), 7);
			mind.addActionDesire(new DesireAttackTarget(this.getRemoteEntity(), 16, false, false), 1);
			mind.addActionDesire(new DesireAttackNearest(this.getRemoteEntity(), EntityLiving.class, 30F, false, 0), 2);
		}
		catch(Exception e)
		{
		}
	}

	@Override
	public RemoteEntity getRemoteEntity()
	{
		return this.m_remoteEntity;
	}
	
	@Override
	public void g(double x, double y, double z)
	{		
		if(this.m_remoteEntity != null && this.m_remoteEntity.isPushable() && !this.m_remoteEntity.isStationary())
			super.g(x, y, z);
	}
	
	@Override
	public void move(double d0, double d1, double d2)
	{
		if(this.m_remoteEntity != null && this.m_remoteEntity.isStationary())
			return;
		
		super.move(d0, d1, d2);
	}
	
	@Override
	public void setMaxHealth(int inHealth)
	{
		this.m_maxHealth = inHealth;
	}
	
	@Override
	public int getMaxHealth()
	{
		if(this.m_maxHealth == 0)
			return defaultMaxHealth;
		return this.m_maxHealth;
	}
	
	@Override
	public boolean bb()
	{
		return true;
	}
	
	@Override
	public void j_()
	{
		super.j_();
		if(this.getRemoteEntity() != null)
			this.getRemoteEntity().getMind().tick();
	}
	
	@Override
	public void b_(EntityHuman entity)
	{
		if(entity instanceof EntityPlayer && this.getRemoteEntity().getMind().canFeel() && this.getRemoteEntity().getMind().hasBehaviour("Touch"))
		{
			if (this.m_lastBouncedId != entity.id || System.currentTimeMillis() - this.m_lastBouncedTime > 1000)
			{
				if(entity.getBukkitEntity().getLocation().distanceSquared(getBukkitEntity().getLocation()) <= 1)
				{
					RemoteEntityTouchEvent event = new RemoteEntityTouchEvent(this.m_remoteEntity, entity.getBukkitEntity());
					Bukkit.getPluginManager().callEvent(event);
					if(event.isCancelled())
						return;
					
					((TouchBehavior)this.getRemoteEntity().getMind().getBehaviour("Touch")).onTouch((Player)entity.getBukkitEntity());
					this.m_lastBouncedTime = System.currentTimeMillis();
					this.m_lastBouncedId = entity.id;
				}
			}
		}
		super.b_(entity);
	}
	
	@Override
	public boolean c(EntityHuman entity)
	{
		if(entity instanceof EntityPlayer && this.getRemoteEntity().getMind().canFeel() && this.getRemoteEntity().getMind().hasBehaviour("Interact"))
		{
			((InteractBehavior)this.getRemoteEntity().getMind().getBehaviour("Interact")).onInteract((Player)entity.getBukkitEntity());
		}
		
		return super.c(entity);
	}
	
	@Override
	public void die(DamageSource damagesource)
	{
		this.getRemoteEntity().getMind().clearMovementDesires();
		this.getRemoteEntity().getMind().clearActionDesires();
		super.die(damagesource);
	}
}