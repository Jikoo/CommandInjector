package com.github.jikoo.commandinjector;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

public abstract class InjectableCommand extends Command implements PluginIdentifiableCommand {

	private final Plugin plugin;

	protected InjectableCommand(Plugin plugin, String name) {
		super(name);
		this.plugin = plugin;
	}

	protected InjectableCommand(Plugin plugin, String name, String description, String usageMessage, String permission, List<String> aliases) {
		super(name, description, usageMessage, aliases);
		this.plugin = plugin;
		this.setPermission(permission);
	}

	@Override
	public Plugin getPlugin() {
		return this.plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (this.getPermission() != null && !sender.hasPermission(this.getPermission())) {
			sender.sendMessage(this.getPermissionMessage());
			return false;
		}
		try {
			if (onCommand(sender, label, args)) {
				return true;
			}
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "An error occurred processing this command. Please make sure your parameters are correct.");
			e.printStackTrace();
		}
		sender.sendMessage(this.getUsage());
		return true;
	}

	protected abstract boolean onCommand(CommandSender sender, String label, String[] args);
}
