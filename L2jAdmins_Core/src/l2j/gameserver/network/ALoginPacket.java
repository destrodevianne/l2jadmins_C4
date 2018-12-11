package l2j.gameserver.network;

/**
 * @author -Wooden-
 */
public abstract class ALoginPacket
{
	private final byte[] decrypt;
	private int off;
	
	public ALoginPacket(byte[] decrypt)
	{
		this.decrypt = decrypt;
		off = 1; // skip packet type id
	}
	
	public int readD()
	{
		int result = decrypt[off++] & 0xff;
		result |= (decrypt[off++] << 8) & 0xff00;
		result |= (decrypt[off++] << 0x10) & 0xff0000;
		result |= (decrypt[off++] << 0x18) & 0xff000000;
		return result;
	}
	
	public int readC()
	{
		int result = decrypt[off++] & 0xff;
		return result;
	}
	
	public int readH()
	{
		int result = decrypt[off++] & 0xff;
		result |= (decrypt[off++] << 8) & 0xff00;
		return result;
	}
	
	public double readF()
	{
		long result = decrypt[off++] & 0xff;
		result |= (decrypt[off++] << 8) & 0xff00;
		result |= (decrypt[off++] << 0x10) & 0xff0000;
		result |= (decrypt[off++] << 0x18) & 0xff000000;
		result |= (decrypt[off++] << 0x20) & 0xff00000000l;
		result |= (decrypt[off++] << 0x28) & 0xff0000000000l;
		result |= (decrypt[off++] << 0x30) & 0xff000000000000l;
		result |= (decrypt[off++] << 0x38) & 0xff00000000000000l;
		return Double.longBitsToDouble(result);
	}
	
	public String readS()
	{
		String result = null;
		try
		{
			result = new String(decrypt, off, decrypt.length - off, "UTF-16LE");
			result = result.substring(0, result.indexOf(0x00));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		off += (result.length() * 2) + 2;
		return result;
	}
	
	public final byte[] readB(int length)
	{
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++)
		{
			result[i] = decrypt[off + i];
		}
		off += length;
		return result;
	}
}
