package com.github.jikoo.commandinjector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandInjector extends JavaPlugin {

	private final Map<String, Command> overridden = new HashMap<>();
	private final String overrideFormat = "Overriding %s by %s. Aliases: %s";

	private HashMap<String, Command> cmdMapKnownCommands;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		try {
			cmdMapKnownCommands = getInternalCommandMap();
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException
				| NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
			getLogger().severe("Could not fetch SimpleCommandMap from CraftServer, commands will fail to register.");
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		/*
		 * Obviously, classes could be loaded and injected when the commands are run. However, this
		 * being a quick and dirty example, we'll just do it when the plugin is enabled.
		 * 
		 * Obviously, in an ordinary use case, this is not the plugin enabling the commands -
		 * enableCommand would be called from another plugin/module.
		 */
		for (String clazzName : getConfig().getStringList("commands")) {
			enableCommand(clazzName, this);
		}
	}

	private HashMap<String, Command> getInternalCommandMap() throws NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchFieldException {
		Method getCommandMap = getServer().getClass().getMethod("getCommandMap");
		SimpleCommandMap cmdMap = (SimpleCommandMap) getCommandMap.invoke(getServer());
		if (cmdMap == null) {
			return null;
		}
		Field field = cmdMap.getClass().getDeclaredField("knownCommands");
		field.setAccessible(true);
		return (HashMap<String, Command>) field.get(cmdMap);
	}

	public void enableCommand(String commandClazz, Plugin owner) {
		try {
			Class<? extends InjectableCommand> clazz = (Class<? extends InjectableCommand>) Class.forName(commandClazz);
			// Quick and dirty: All commands must have a matching constructor.
			Constructor<? extends InjectableCommand> constructor = clazz.getConstructor(Plugin.class);
			InjectableCommand command = constructor.newInstance(owner);
			if (cmdMapKnownCommands.containsKey(command.getName())) {
				Command override = cmdMapKnownCommands.remove(command.getName());
				overridden.put(command.getName(), override);
				logOverride(command.getName(), override);
			}
			cmdMapKnownCommands.put(command.getName(), command);
			for (String alias : command.getAliases()) {
				if (cmdMapKnownCommands.containsKey(alias)) {
					Command override = cmdMapKnownCommands.remove(alias);
					overridden.put(alias, override);
					logOverride(alias, override);
				}
				cmdMapKnownCommands.put(alias, command);
			}
		} catch (ClassNotFoundException | ClassCastException | NoSuchMethodException
				| SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void logOverride(String alias, Command command) {
		this.getLogger().info(String.format(overrideFormat, alias,
				command instanceof PluginIdentifiableCommand
						? ((PluginIdentifiableCommand) command).getPlugin().getName()
						: this.getServer().getVersion(),
				command.getAliases().toString()));
	}

	@Override
	public void onDisable() {
		if (cmdMapKnownCommands == null) {
			return;
		}

		for (String clazzName : getConfig().getStringList("commands")) {
			disableCommand(clazzName, this);
		}
	}

	public void disableCommand(String commandClazz, Plugin owner) {
		try {
			// Quick and dirty: This could be done more efficiently by storing commands to class names, but again, time.
			Class<? extends InjectableCommand> clazz = (Class<? extends InjectableCommand>) Class.forName(commandClazz);
			Constructor<? extends InjectableCommand> constructor = clazz.getConstructor(Plugin.class);
			InjectableCommand command = constructor.newInstance(owner);
			if (overridden.containsKey(command.getName())) {
				cmdMapKnownCommands.put(command.getName(), overridden.remove(command.getName()));
			} else {
				cmdMapKnownCommands.remove(command.getName());
			}
			for (String alias : command.getAliases()) {
				if (overridden.containsKey(alias)) {
					cmdMapKnownCommands.put(alias, overridden.remove(alias));
				} else {
					cmdMapKnownCommands.remove(alias);
				}
			}
		} catch (ClassNotFoundException | ClassCastException | NoSuchMethodException
				| SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
