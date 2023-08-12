package l2r.gameserver.communitybbs.SunriseBoards.subClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import gr.sr.configsEngine.AbstractConfigs;
import gr.sr.utils.FileProperties;

/**
 * @author vGodFather
 */
public class SubClassConfigs extends AbstractConfigs
{
	private static final String SUB_CLASS_CONFIG_FILE = "./config/extra/SubClassConfigs.ini";
	
	public static boolean ENABLE_CB_SUB_CLASS_MANAGER;
	public static boolean ALLOW_SUB_MANAGER_PEACE_ZONE_ONLY;
	public static String BYPASS_COMMAND;
	
	@Override
	public void loadConfigs()
	{
		// Load drop calculator Server L2Properties file (if exists)
		FileProperties SubClassManager = new FileProperties();
		final File subManager = new File(SUB_CLASS_CONFIG_FILE);
		try (InputStream is = new FileInputStream(subManager))
		{
			SubClassManager.load(is);
		}
		catch (Exception e)
		{
			_log.error("Error while loading sub class manager system settings!", e);
		}
		
		ENABLE_CB_SUB_CLASS_MANAGER = Boolean.parseBoolean(SubClassManager.getProperty("EnableCBSubClassManager", "False"));
		ALLOW_SUB_MANAGER_PEACE_ZONE_ONLY = Boolean.parseBoolean(SubClassManager.getProperty("CBSubManagerPeaceZoneOnly", "False"));
		BYPASS_COMMAND = SubClassManager.getProperty("BypassCommand", "_bbsmemo");
		
	}
	
	public static SubClassConfigs getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SubClassConfigs _instance = new SubClassConfigs();
	}
}