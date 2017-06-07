package com.nisovin.magicspells.spells.command;

import java.util.HashSet;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class BindSpell extends CommandSpell {
	
	private HashSet<CastItem> bindableItems;
	private boolean allowBindToFist;
	private String strUsage;
	private String strNoSpell;
	private String strCantBindSpell;
	private String strCantBindItem;

	public BindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> bindables = getConfigStringList("bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<CastItem>();
			for (String s : bindables) {
				bindableItems.add(new CastItem(s));
			}
		}
		allowBindToFist = getConfigBoolean("allow-bind-to-fist", false);
		strUsage = getConfigString("str-usage", "You must specify a spell name and hold an item in your hand.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
		strCantBindItem = getConfigString("str-cant-bind-item", "That spell cannot be bound to that item.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length == 0) {
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				Spell spell = MagicSpells.getSpellByInGameName(Util.arrayJoin(args, ' '));
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				if (spell == null || spellbook == null) {
					// fail - no such spell, or no spellbook
					sendMessage(player, strNoSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spellbook.hasSpell(spell)) {
					// fail - doesn't know spell
					sendMessage(player, strNoSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spell.canCastWithItem()) {
					// fail - spell can't be bound
					sendMessage(player, strCantBindSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					CastItem castItem = new CastItem(player.getItemInHand());
					MagicSpells.debug(3, "Trying to bind spell '" + spell.getInternalName() + "' to cast item " + castItem.toString() + "...");
					if (castItem.getItemTypeId() == 0 && !allowBindToFist) {
						sendMessage(player, strCantBindItem);
						return PostCastAction.ALREADY_HANDLED;
					} else if (bindableItems != null && !bindableItems.contains(castItem)) {
						sendMessage(player, strCantBindItem);
						return PostCastAction.ALREADY_HANDLED;
					} else if (!spell.canBind(castItem)) {
						String msg = spell.getCantBindError();
						if (msg == null) msg = strCantBindItem;
						sendMessage(player, msg);
						return PostCastAction.ALREADY_HANDLED;
					} else {
						MagicSpells.debug(3, "    Performing bind...");
						spellbook.addCastItem(spell, castItem);
						spellbook.save();
						MagicSpells.debug(3, "    Bind successful.");
						sendMessage(player, formatMessage(strCastSelf, "%s", spell.getName()));
						playSpellEffects(EffectPosition.CASTER, player);
						return PostCastAction.NO_MESSAGES;
					}
				}
			}
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player) {
			// only one arg
			if (partial.contains(" ")) {
				return null;
			}
			
			// tab complete spellname from spellbook
			return tabCompleteSpellName(sender, partial);
		}
		return null;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

}
