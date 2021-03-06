package l2j.gameserver.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2j.Config;
import l2j.gameserver.model.actor.L2Npc;
import l2j.gameserver.model.actor.instance.L2PcInstance;
import l2j.gameserver.model.actor.manager.character.itemcontainer.Inventory;
import l2j.gameserver.model.items.Item;
import l2j.gameserver.model.items.ItemArmor;
import l2j.gameserver.model.items.ItemWeapon;
import l2j.gameserver.model.items.instance.ItemInstance;
import l2j.gameserver.model.multisell.MultisellContainer;
import l2j.gameserver.model.multisell.MultisellEntry;
import l2j.gameserver.model.multisell.MultisellIngredient;
import l2j.gameserver.network.external.server.MultiSellList;
import l2j.util.UtilPrint;
import l2j.util.XmlParser;
import l2j.util.file.filter.XMLFilter;

/**
 * @author anonymous
 * @author fissban
 */
public class MultisellData extends XmlParser
{
	private final List<MultisellContainer> entries = new ArrayList<>();
	
	private int currentId = 0;
	
	public MultisellData()
	{
		//
	}
	
	@Override
	public void load()
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/xml/multisell");
		
		XMLFilter XML_FILTER = new XMLFilter();
		
		for (File f : dir.listFiles())
		{
			if (XML_FILTER.accept(f))
			{
				currentId = Integer.valueOf(f.getName().replace(".xml", ""));
				loadFile(f);
			}
		}
		
		UtilPrint.result("MultisellData", "Loaded multisell", entries.size());
	}
	
	public void reload()
	{
		//
	}
	
	@Override
	protected void parseFile()
	{
		NamedNodeMap attrs = null;
		
		MultisellContainer container = new MultisellContainer();
		container.setListId(currentId);
		
		for (Node nodeList : getNodes("list"))
		{
			attrs = nodeList.getAttributes();
			boolean applyTaxes = parseBoolean(attrs, "applyTaxes");
			boolean maintainEnchantment = parseBoolean(attrs, "maintainEnchantment");
			
			container.setApplyTaxes(applyTaxes);
			container.setMaintainEnchantment(maintainEnchantment);
			
			for (Node nodeItems = nodeList.getFirstChild(); nodeItems != null; nodeItems = nodeItems.getNextSibling())
			{
				if (nodeItems.getNodeName().equals("item"))
				{
					attrs = nodeItems.getAttributes();
					// var id = parseInt(attrs, "id");
					
					for (Node product = nodeItems.getFirstChild(); product != null; product = product.getNextSibling())
					{
						attrs = product.getAttributes();
						
						MultisellEntry entry = new MultisellEntry();
						
						int entryId = parseInt(attrs, "id");
						int entryCount = parseInt(attrs, "count");
						int entryEnchant = 0;
						try
						{
							entryEnchant = parseInt(attrs, "enchant");
						}
						catch (Exception e)
						{
							//
						}
						
						switch (product.getNodeName())
						{
							case "ingredient":
								entry.addIngredient(new MultisellIngredient(entryId, entryCount, entryEnchant));
								break;
							case "production":
								entry.addProduct(new MultisellIngredient(entryId, entryCount, entryEnchant));
								break;
						}
					}
				}
			}
		}
		
		entries.add(container);
	}
	
	public MultisellContainer getList(int id)
	{
		for (MultisellContainer list : entries)
		{
			if (list.getListId() == id)
			{
				return list;
			}
		}
		
		LOG.error("MultisellData: cant find list with id: " + id);
		return null;
	}
	
	/**
	 * This will generate the multisell list for the items. There exist various parameters in multisells that affect the way they will appear: 1) inventory only: * if true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary"
	 * ingredients we mean weapon and armor. * if false, show the entire list. 2) maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise... * If true, then the product will match the enchantment level of the
	 * ingredient. if the player has multiple items that match the ingredient list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level. For example: If the player has a crystal staff +1 and a crystal staff +3 and
	 * goes to exchange it at the mammon, the list should have all exchange possibilities for the +1 staff, followed by all possibilities for the +3 staff. * If false, then any level ingredient will be considered equal and product will always be at +0 3) apply taxes: affects the amount of adena and
	 * ancient adena in ingredients.
	 * @param  listId
	 * @param  inventoryOnly
	 * @param  player
	 * @param  merchant
	 * @return
	 */
	private MultisellContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, L2Npc merchant)
	{
		MultisellContainer listTemplate = MultisellData.getInstance().getList(listId);
		MultisellContainer list = new MultisellContainer();
		
		if (listTemplate == null)
		{
			return list;
		}
		
		list = new MultisellContainer();
		list.setListId(listId);
		if ((merchant != null) && (merchant.getId() != 0) && !listTemplate.checkNpcId(merchant.getId()))
		{
			listTemplate.addNpcId(merchant.getId());
		}
		
		if (inventoryOnly)
		{
			if (player == null)
			{
				return list;
			}
			
			List<ItemInstance> items;
			if (listTemplate.getMaintainEnchantment())
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false);
			}
			
			int enchantLevel;
			for (ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if (!item.isWear() && ((item.getItem() instanceof ItemArmor) || (item.getItem() instanceof ItemWeapon)))
				{
					enchantLevel = (listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0);
					// loop through the entries to see which ones we wish to include
					for (MultisellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (MultisellIngredient ing : ent.getIngredients())
						{
							if (item.getId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						
						// manipulate the ingredients of the template entry for this particular INSTANCE shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
						{
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, merchant));
						}
					}
				}
			} // end for each inventory item.
		} // end if "inventory-only"
		else
		// this is a list-all type
		{
			// if no taxes are applied, no modifications are needed
			for (MultisellEntry ent : listTemplate.getEntries())
			{
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, merchant));
			}
		}
		
		return list;
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private MultisellEntry prepareEntry(MultisellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, L2Npc merchant)
	{
		MultisellEntry newEntry = new MultisellEntry();
		newEntry.setEntryId((templateEntry.getEntryId() * 100000) + enchantLevel);
		
		for (MultisellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultisellIngredient newIngredient = new MultisellIngredient(ing);
			
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (applyTaxes && ((ing.getItemId() == Inventory.ADENA_ID) || (ing.getItemId() == Inventory.ANCIENT_ADENA_ID)))
			{
				
				double taxRate = 0.0;
				if ((merchant != null) && (merchant.getCastle() != null))
				{
					taxRate = merchant.getCastle().getTaxRate();
				}
				
				if (ing.getItemId() == Inventory.ADENA_ID)
				{
					int taxAmount = (int) Math.round(ing.getItemCount() * taxRate);
					newIngredient.setItemCount(ing.getItemCount() + taxAmount);
				}
				else
				// ancient adena
				{
					// add the ancient adena count normally
					newEntry.addIngredient(newIngredient);
					double taxableCount = (ing.getItemCount() * 5.0) / 6;
					if (taxRate == 0)
					{
						continue;
					}
					newIngredient = new MultisellIngredient(Inventory.ADENA_ID, (int) Math.round(taxableCount * taxRate));
				}
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment)
			{
				Item tempItem = ItemData.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof ItemArmor) || (tempItem instanceof ItemWeapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Now modify the enchantment level of products, if necessary
		for (MultisellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultisellIngredient newIngredient = new MultisellIngredient(ing);
			
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				Item tempItem = ItemData.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof ItemArmor) || (tempItem instanceof ItemWeapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	public void createMultiSell(int listId, L2PcInstance player, boolean inventoryOnly, L2Npc merchant)
	{
		MultisellContainer list = generateMultiSell(listId, inventoryOnly, player, merchant);
		MultisellContainer temp = new MultisellContainer();
		int page = 1;
		
		temp.setListId(list.getListId());
		for (MultisellEntry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page, 0));
				page++;
				temp = new MultisellContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
	}
	
	public static MultisellData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData INSTANCE = new MultisellData();
	}
}
