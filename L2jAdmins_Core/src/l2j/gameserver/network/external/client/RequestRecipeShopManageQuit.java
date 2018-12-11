package l2j.gameserver.network.external.client;

import l2j.gameserver.model.actor.instance.L2PcInstance;
import l2j.gameserver.model.privatestore.PcStoreType;
import l2j.gameserver.network.AClientPacket;

/**
 * This class ... cd(dd)
 * @version $Revision: 1.1.2.2.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestRecipeShopManageQuit extends AClientPacket
{
	@Override
	protected void readImpl()
	{
		//
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		player.getPrivateStore().setStoreType(PcStoreType.NONE);
		player.broadcastUserInfo();
		player.standUp();
	}
}
