package l2j.gameserver.model.skills.conditions;

import l2j.gameserver.model.skills.stats.Env;

/**
 * @author Steuf
 */
public class ConditionWithSkill extends Condition
{
	final boolean skill;
	
	public ConditionWithSkill(boolean skill)
	{
		this.skill = skill;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (!skill && (env.getSkill() != null))
		{
			return false;
		}
		return true;
	}
}
