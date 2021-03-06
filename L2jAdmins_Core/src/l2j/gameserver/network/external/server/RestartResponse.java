package l2j.gameserver.network.external.server;

import l2j.gameserver.network.AServerPacket;

/**
 * This class ...
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class RestartResponse extends AServerPacket
{
	public static final RestartResponse STATIC_PACKET = new RestartResponse();
	
	@Override
	public void writeImpl()
	{
		writeC(0x5f);
		
		writeD(0x01); // 01-ok
	}
}
