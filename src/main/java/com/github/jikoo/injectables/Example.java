package com.github.jikoo.injectables;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import com.github.jikoo.commandinjector.InjectableCommand;

public class Example extends InjectableCommand {

	protected Example(Plugin plugin) {
		super(plugin, "example");
		this.setPermission("example.permission");
		this.setPermissionMessage("Nope, not happening.");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		sender.sendMessage("Example.");
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		return Arrays.asList("Example");
	}

}
