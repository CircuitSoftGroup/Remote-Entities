package de.kumpelblase2.remoteentities.api.thinking.goals;

import net.minecraft.server.v1_7_R4.ChunkCoordinates;
import net.minecraft.server.v1_7_R4.Vec3D;
import org.bukkit.Location;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.thinking.DesireBase;
import de.kumpelblase2.remoteentities.api.thinking.DesireType;
import de.kumpelblase2.remoteentities.nms.RandomPositionGenerator;
import de.kumpelblase2.remoteentities.utilities.NMSUtil;

public class DesireMoveTowardsRestriction extends DesireBase
{
	protected double m_x;
	protected double m_y;
	protected double m_z;

	@Deprecated
	public DesireMoveTowardsRestriction(RemoteEntity inEntity)
	{
		super(inEntity);
		this.m_type = DesireType.PRIMAL_INSTINCT;
	}

	public DesireMoveTowardsRestriction()
	{
		super();
		this.m_type = DesireType.PRIMAL_INSTINCT;
	}

	@Override
	public boolean shouldExecute()
	{
		if(this.getEntityHandle() == null || NMSUtil.isInHomeArea(this.getEntityHandle()))
			return false;
		else
		{
			ChunkCoordinates chunkCoords = NMSUtil.getChunkCoordinates(this.getEntityHandle());
			Vec3D vec = RandomPositionGenerator.a(this.getEntityHandle(), 16, 7, Vec3D.a(chunkCoords.x, chunkCoords.y, chunkCoords.z));
			if(vec == null)
				return false;
			else
			{
				this.m_x = vec.a;
				this.m_y = vec.b;
				this.m_z = vec.c;
				return true;
			}
		}
	}

	@Override
	public boolean canContinue()
	{
		return !this.getNavigation().g();
	}

	@Override
	public void startExecuting()
	{
		this.getRemoteEntity().move(new Location(this.getRemoteEntity().getBukkitEntity().getWorld(), this.m_x, this.m_y, this.m_z));
	}
}