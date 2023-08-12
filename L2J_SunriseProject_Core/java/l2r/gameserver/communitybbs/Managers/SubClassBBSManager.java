package l2r.gameserver.communitybbs.Managers;

import java.util.Iterator;
import java.util.Set;

import l2r.Config;
import l2r.gameserver.cache.HtmCache;
import l2r.gameserver.communitybbs.BoardsManager;
import l2r.gameserver.communitybbs.SunriseBoards.subClass.SubClassConfigs;
import l2r.gameserver.data.xml.impl.ClassListData;
import l2r.gameserver.enums.Race;
import l2r.gameserver.enums.ZoneIdType;
import l2r.gameserver.model.actor.instance.L2PcInstance;
import l2r.gameserver.model.base.ClassId;
import l2r.gameserver.model.base.PlayerClass;
import l2r.gameserver.model.base.SubClass;
import l2r.gameserver.model.entity.olympiad.OlympiadManager;
import l2r.gameserver.model.quest.Quest;
import l2r.gameserver.model.quest.QuestState;
import l2r.gameserver.network.SystemMessageId;
import l2r.gameserver.network.serverpackets.ShowBoard;
import l2r.util.StringUtil;

import gr.sr.main.Conditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubClassBBSManager extends BaseBBSManager
{
	
	private static Logger _log = LoggerFactory.getLogger(SubClassBBSManager.class);
	public String _subManagerBBSCommand = SubClassConfigs.BYPASS_COMMAND;
	
	@Override
	public void cbByPass(String command, L2PcInstance activeChar)
	{
		if (!SubClassConfigs.ENABLE_CB_SUB_CLASS_MANAGER)
		{
			activeChar.sendMessage("This function is disabled by admin.");
			return;
		}
		
		String path = "data/html/CommunityBoard/subclass/";
		String filepath = "";
		String content = "";
		
		if (command.equals(_subManagerBBSCommand + ""))
		{
			BoardsManager.getInstance().addBypass(activeChar, "Subclass Command", command);
			filepath = path + "subsmain.htm";
			content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), filepath);
			separateAndSend(content, activeChar);
		}
		
		else if (command.startsWith(_subManagerBBSCommand + "_Subclass"))
		{
			if (!SubClassConfigs.ALLOW_SUB_MANAGER_PEACE_ZONE_ONLY && !activeChar.isInsideZone(ZoneIdType.PEACE))
			{
				activeChar.sendMessage("You cannot use this function outside peace zone.");
				return;
			}
			// Subclasses may not be changed while a skill is in use.
			else if (activeChar.isCastingNow() || activeChar.isAllSkillsDisabled())
			{
				activeChar.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			else if (activeChar.isInCombat())
			{
				activeChar.sendMessage("Sub classes may not be created or changed while being in combat.");
				return;
			}
			else if (OlympiadManager.getInstance().isRegistered(activeChar))
			{
				activeChar.sendMessage("You cannot change subclass when registered for Olympiad.");
				return;
			}
			else if (activeChar.isCursedWeaponEquipped())
			{
				activeChar.sendMessage("You cannot change Subclass while Cursed weapon equiped!");
				return;
			}
			
			if (activeChar.getTransformation() != null)
			{
				content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_NoTransformed.htm");
				separateAndSend(content, activeChar);
				return;
			}
			// Subclasses may not be changed while a summon is active.
			if (activeChar.hasSummon())
			{
				content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_NoSummon.htm");
				separateAndSend(content, activeChar);
				return;
			}
			// Subclasses may not be changed while you have exceeded your inventory limit.
			if (!activeChar.isInventoryUnder90(true))
			{
				activeChar.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_INVENTORY_FULL);
				return;
			}
			// Subclasses may not be changed while a you are over your weight limit.
			if (activeChar.getWeightPenalty() >= 2)
			{
				activeChar.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
				return;
			}
			
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			try
			{
				cmdChoice = Integer.parseInt(command.substring(18, 19).trim());
				
				int endIndex = command.indexOf(' ', 20);
				if (endIndex == -1)
				{
					endIndex = command.length();
				}
				
				if (command.length() > 20)
				{
					paramOne = Integer.parseInt(command.substring(20, endIndex).trim());
					if (command.length() > endIndex)
					{
						paramTwo = Integer.parseInt(command.substring(endIndex).trim());
					}
				}
			}
			catch (Exception NumberFormatException)
			{
				_log.warn(SubClassBBSManager.class.getName() + ": Wrong numeric values for command " + command);
			}
			
			Set<PlayerClass> subsAvailable = null;
			switch (cmdChoice)
			{
				case 0: // Subclass change menu
					content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), getSubClassMenu(activeChar.getRace()));
					break;
				case 1: // Add Subclass - Initial
					// Avoid giving player an option to add a new sub class, if they have max sub-classes already.
					if (activeChar.getTotalSubClasses() >= Config.MAX_SUBCLASS)
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), getSubClassFail());
						break;
					}
					
					subsAvailable = Conditions.getAvailableSubClasses(activeChar);
					if ((subsAvailable != null) && !subsAvailable.isEmpty())
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Add.htm");
						final StringBuilder content1 = StringUtil.startAppend(2000);
						for (PlayerClass subClass : subsAvailable)
						{
							
							StringUtil.append(content1, "<table width=500><tr><td align=center><button value=\"", ClassListData.getInstance().getClass(subClass.ordinal()).getClassName(), "\" action=\"bypass %command%_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", ClassListData.getInstance().getClass(subClass.ordinal()).getClassName(), "\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\"fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\"></td></tr><table>");
						}
						content = content.replace("%list%", content1.toString());
					}
					else
					{
						if ((activeChar.getRace() == Race.ELF) || (activeChar.getRace() == Race.DARK_ELF))
						{
							HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Fail_Elves.htm");
							separateAndSend(content, activeChar);
						}
						else if (activeChar.getRace() == Race.KAMAEL)
						{
							HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Fail_Kamael.htm");
							separateAndSend(content, activeChar);
						}
						else
						{
							activeChar.sendMessage("There are no sub classes available at this time.");
						}
						
						return;
					}
					break;
				case 2: // Change Class - Initial
					
					if (activeChar.getSubClasses().isEmpty())
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ChangeNo.htm");
						separateAndSend(content, activeChar);
					}
					else
					{
						final StringBuilder content2 = StringUtil.startAppend(2000);
						if (Conditions.checkVillageMaster(activeChar.getBaseClass()))
						{
							StringUtil.append(content2, "<table width=500><tr><td align=center><button value=", ClassListData.getInstance().getClass(activeChar.getBaseClass()).getClientCode(), " action=\"bypass %command%_Subclass 5 0\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\">", "</td></tr><table>");
						}
						
						for (Iterator<SubClass> subList = Conditions.iterSubClasses(activeChar); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							if (Conditions.checkVillageMaster(subClass.getClassDefinition()))
							{
								StringUtil.append(content2, "<table width=500><tr><td align=center><button value=", ClassListData.getInstance().getClass(subClass.getClassId()).getClientCode(), " action=\"bypass %command%_Subclass 5 ", String.valueOf(subClass.getClassIndex()), "\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\">", "</td></tr></table>");
							}
						}
						
						if (content2.length() > 0)
						{
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Change.htm");
							content = content.replace("%list%", content2.toString());
							separateAndSend(content, activeChar);
						}
						else
						{
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ChangeNotFound.htm");
							separateAndSend(content, activeChar);
						}
					}
					
					break;
				case 3: // Change/Cancel Subclass - Initial
					if ((activeChar.getSubClasses() == null) || activeChar.getSubClasses().isEmpty())
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyEmpty.htm");
						separateAndSend(content, activeChar);
						break;
					}
					
					// custom value
					if (activeChar.getTotalSubClasses() > 3)
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyCustom.htm");
						final StringBuilder content3 = StringUtil.startAppend(200);
						int classIndex = 1;
						
						for (Iterator<SubClass> subList = iterSubClasses(activeChar); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<table width=500><tr><td align=center><button value=", ClassListData.getInstance().getClass(subClass.getClassId()).getClientCode(), " action=\"bypass %command%_Subclass 6 ", String.valueOf(subClass.getClassIndex()), "\">", "</td></tr></table>");
						}
						content = content.replace("%list%", String.valueOf(content3));
						separateAndSend(content, activeChar);
					}
					else
					{
						// retail html contain only 3 subclasses
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Modify.htm");
						if (activeChar.getSubClasses().containsKey(1))
						{
							content = content.replace("%sub1%", ClassListData.getInstance().getClass(activeChar.getSubClasses().get(1).getClassId()).getClientCode());
							separateAndSend(content, activeChar);
						}
						else
						{
							content = content.replace("<tr><td align=center><button value=\"%sub1%\" action=\"bypass %command%_Subclass 6 1\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\"></td></tr>", "");
							separateAndSend(content, activeChar);
						}
						
						if (activeChar.getSubClasses().containsKey(2))
						{
							content = content.replace("%sub2%", ClassListData.getInstance().getClass(activeChar.getSubClasses().get(2).getClassId()).getClientCode());
							separateAndSend(content, activeChar);
						}
						else
						{
							content = content.replace("<tr><td align=center><button value=\"%sub2%\" action=\"bypass %command%_Subclass 6 1\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\"></td></tr>", "");
							separateAndSend(content, activeChar);
						}
						
						if (activeChar.getSubClasses().containsKey(3))
						{
							content = content.replace("%sub3%", ClassListData.getInstance().getClass(activeChar.getSubClasses().get(3).getClassId()).getClientCode());
							separateAndSend(content, activeChar);
						}
						else
						{
							content = content.replace("<tr><td align=center><button value=\"%sub3%\" action=\"bypass %command%_Subclass 6 1\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\"></td></tr>", "");
							separateAndSend(content, activeChar);
						}
					}
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					/**
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice.
					 */
					if (!activeChar.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
					{
						// _log.warn(L2VillageMasterInstance.class.getSimpleName() + ": activeChar " + activeChar.getName() + " has performed a subclass change too fast");
						return;
					}
					
					boolean allowAddition = true;
					
					if (activeChar.getTotalSubClasses() >= Config.MAX_SUBCLASS)
					{
						allowAddition = false;
					}
					
					if (activeChar.getLevel() < 75)
					{
						allowAddition = false;
					}
					
					if (allowAddition)
					{
						if (!activeChar.getSubClasses().isEmpty())
						{
							for (Iterator<SubClass> subList = Conditions.iterSubClasses(activeChar); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								
								if (subClass.getLevel() < 75)
								{
									allowAddition = false;
									break;
								}
							}
						}
					}
					
					/**
					 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with adding
					 * the sub-class.
					 */
					if (allowAddition && !Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
					{
						allowAddition = Conditions.checkQuests(activeChar);
					}
					
					if (allowAddition && Conditions.isValidNewSubClass(activeChar, paramOne))
					{
						if (!activeChar.addSubClass(paramOne, activeChar.getTotalSubClasses() + 1))
						{
							return;
						}
						
						activeChar.setActiveClass(activeChar.getTotalSubClasses());
						
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_AddOk.htm");
						activeChar.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
					}
					else
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), getSubClassFail());
					}
					break;
				case 5: // Change Class - Action
					/**
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice. Note: paramOne = classIndex
					 */
					if (!activeChar.getFloodProtectors().getSubclass().tryPerformAction("change class"))
					{
						// _log.warn(L2ServicesManagerInstance.class.getName() + ": Player " + player.getName() + " has performed a subclass change too fast");
						return;
					}
					
					if (activeChar.getClassIndex() == paramOne)
					{
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_Current.htm");
						separateAndSend(content, activeChar);
						break;
					}
					
					if (paramOne == 0)
					{
						if (!Conditions.checkVillageMaster(activeChar.getBaseClass()))
						{
							return;
						}
					}
					else
					{
						try
						{
							if (!Conditions.checkVillageMaster(activeChar.getSubClasses().get(paramOne).getClassDefinition()))
							{
								return;
							}
						}
						catch (NullPointerException e)
						{
							return;
						}
					}
					
					activeChar.setActiveClass(paramOne);
					activeChar.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED); // Transfer completed.
					return;
				case 6: // Change/Cancel Subclass - Choice
					// validity check
					if ((paramOne < 1) || (paramOne > Config.MAX_SUBCLASS))
					{
						return;
					}
					
					subsAvailable = getAvailableSubClasses(activeChar);
					// another validity check
					if ((subsAvailable == null) || subsAvailable.isEmpty())
					{
						// TODO: Retail message
						activeChar.sendMessage("There are no sub classes available at this time.");
						return;
					}
					
					final StringBuilder content6 = StringUtil.startAppend(2000);
					for (PlayerClass subClass : subsAvailable)
					{
						StringUtil.append(content6, "<table width=500><tr><td align=center><button value=", ClassListData.getInstance().getClass(subClass.ordinal()).getClientCode(), " action=\"bypass %command%_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\" msg=\"1445;", "\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm\">", "</td></tr></table>");
					}
					
					switch (paramOne)
					{
						case 1:
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyChoice1.htm");
							break;
						case 2:
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyChoice2.htm");
							break;
						case 3:
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyChoice3.htm");
							break;
						default:
							content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyChoice.htm");
					}
					content = content.replace("%list%", content6.toString());
					separateAndSend(content, activeChar);
					break;
				case 7: // Change Subclass - Action
					/**
					 * Warning: the information about this subclass will be removed from the subclass list even if false!
					 */
					if (!activeChar.getFloodProtectors().getSubclass().tryPerformAction("change class"))
					{
						_log.warn(SubClassBBSManager.class.getSimpleName() + ": Player " + activeChar.getName() + " has performed a subclass change too fast");
						return;
					}
					
					if (!isValidNewSubClass(activeChar, paramTwo))
					{
						return;
					}
					
					if (activeChar.modifySubClass(paramOne, paramTwo))
					{
						activeChar.abortCast();
						activeChar.stopAllEffectsExceptThoseThatLastThroughDeath(); // all effects from old subclass stopped!
						activeChar.stopAllEffectsNotStayOnSubclassChange();
						activeChar.stopCubics();
						activeChar.setActiveClass(paramOne);
						
						content = HtmCache.getInstance().getHtm(activeChar, activeChar.getHtmlPrefix(), "data/html/CommunityBoard/subclass/SubClass_ModifyOk.htm");
						separateAndSend(content, activeChar);
						
						activeChar.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
					}
					else
					{
						/**
						 * This isn't good! modifySubClass() removed subclass from memory we must update _classIndex! Else IndexOutOfBoundsException can turn up some place down the line along with other seemingly unrelated problems.
						 */
						activeChar.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						
						activeChar.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			content = content.replace("%objectId%", String.valueOf(activeChar.getObjectId()));
			separateAndSend(content, activeChar);
		}
	}
	
	protected String getSubClassMenu(Race race)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE || (race != Race.KAMAEL))
		{
			return "data/html/CommunityBoard/subclass/SubClass.htm";
		}
		
		return "data/html/CommunityBoard/subclass/SubClass_NoOther.htm";
	}
	
	protected String getSubClassFail()
	{
		return "data/html/CommunityBoard/subclass/SubClass_Fail.htm";
	}
	
	protected boolean checkQuests(L2PcInstance player)
	{
		// Noble players can add Sub-Classes without quests
		if (player.isNoble())
		{
			return true;
		}
		
		QuestState qs = player.getQuestState(Quest.FATES_WHISPER);
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		qs = player.getQuestState(Quest.MIMIRS_ELIXIR);
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns list of available subclasses Base class and already used subclasses removed
	 * @param player
	 * @return
	 */
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class. If the race of your main class is Kamael, you may not subclass any other race If the race of your main class is NOT Kamael, you may not subclass any Kamael class You may not
		 * select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight
		 * and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and Spellhowler Also, Kamael have a special, hidden 4 subclass, the inspector, which can only be taken if you have already completed the other
		 * two Kamael subclasses
		 */
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		
		if ((availSubs != null) && !availSubs.isEmpty())
		{
			for (Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				PlayerClass pclass = availSub.next();
				
				// check for the village master
				if (!checkVillageMaster(pclass))
				{
					availSub.remove();
					continue;
				}
				
				// scan for already used subclasses
				int availClassId = pclass.ordinal();
				ClassId cid = ClassId.getClassId(availClassId);
				SubClass prevSubClass;
				ClassId subClassId;
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					prevSubClass = subList.next();
					subClassId = ClassId.getClassId(prevSubClass.getClassId());
					
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		
		return availSubs;
	}
	
	/**
	 * Check new subclass classId for validity (villagemaster race/type, is not contains in previous subclasses, is contains in allowed subclasses) Base class not added into allowed subclasses.
	 * @param player
	 * @param classId
	 * @return
	 */
	private final boolean isValidNewSubClass(L2PcInstance player, int classId)
	{
		if (!checkVillageMaster(classId))
		{
			return false;
		}
		
		final ClassId cid = ClassId.values()[classId];
		SubClass sub;
		ClassId subClassId;
		for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			sub = subList.next();
			subClassId = ClassId.values()[sub.getClassId()];
			
			if (subClassId.equalsOrChildOf(cid))
			{
				return false;
			}
		}
		
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if ((availSubs == null) || availSubs.isEmpty())
		{
			return false;
		}
		
		boolean found = false;
		for (PlayerClass pclass : availSubs)
		{
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	protected boolean checkVillageMasterRace(PlayerClass pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(PlayerClass pclass)
	{
		return true;
	}
	
	/**
	 * Returns true if this classId allowed for master
	 * @param classId
	 * @return
	 */
	public final boolean checkVillageMaster(int classId)
	{
		return checkVillageMaster(PlayerClass.values()[classId]);
	}
	
	/**
	 * Returns true if this PlayerClass is allowed for master
	 * @param pclass
	 * @return
	 */
	public final boolean checkVillageMaster(PlayerClass pclass)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE)
		{
			return true;
		}
		
		return checkVillageMasterRace(pclass) && checkVillageMasterTeachType(pclass);
	}
	
	private static final Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	@Override
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		html = html.replace("\t", "");
		html = html.replace("%command%", _subManagerBBSCommand);
		if (html.length() < 8180)
		{
			acha.sendPacket(new ShowBoard(html, "101"));
			acha.sendPacket(new ShowBoard(null, "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (8180 * 2))
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, html.length()), "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (8180 * 3))
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, 8180 * 2), "102"));
			acha.sendPacket(new ShowBoard(html.substring(8180 * 2, html.length()), "103"));
		}
	}
	
	@Override
	public void parsewrite(String url, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		
	}
	
	public static SubClassBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SubClassBBSManager _instance = new SubClassBBSManager();
	}
}