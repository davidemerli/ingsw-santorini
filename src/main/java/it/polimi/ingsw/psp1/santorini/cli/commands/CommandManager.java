package it.polimi.ingsw.psp1.santorini.cli.commands;

import it.polimi.ingsw.psp1.santorini.cli.CLIServerHandler;
import it.polimi.ingsw.psp1.santorini.cli.PrintUtils;
import it.polimi.ingsw.psp1.santorini.network.Client;

import java.util.*;

public class CommandManager {

    private final List<Command> commandList;

    public CommandManager() {
        this.commandList = new ArrayList<>();
        this.addCMDs();
    }

    public String runCommand(Client client, CLIServerHandler serverHandler, String input) {
        String[] arguments = input.split(" ");

        if (arguments.length > 0) {
            String cmd = arguments[0];
            Optional<Command> command = getCommand(cmd);

            if (command.isPresent()) {
                String[] subarray = Arrays.copyOfRange(arguments, 1, arguments.length);

                //if (input.matches(command.get().getPattern())) {
                try {
                    return command.get().onCommand(client, serverHandler, input, subarray);
                } catch (Exception ex) {
                    return "exception: " + ex.getClass() + " " + ex.getMessage() + " " + input + " " + subarray.length;
                }
                //}
//                return "Invalid argument, the usage for this command is: ";
            }
        }

        return "Invalid command, type help for the list of commands";
    }


    public Optional<Command> getCommand(String command) {
        return commandList.stream()
                .filter(c -> c.getName().equalsIgnoreCase(command) || c.getAliases().contains(command.toLowerCase()))
                .findFirst();
    }

    public void addCMDs() {
        commandList.add(new CommandConnect());
        commandList.add(new CommandCreateGame());
        commandList.add(new CommandSurrender());
        commandList.add(new CommandHelp());
        commandList.add(new CommandInteract());
        commandList.add(new CommandPlaceWorker());
        commandList.add(new CommandReload());
        commandList.add(new CommandSelect());
        commandList.add(new CommandSelectPower());
        commandList.add(new CommandSelectStartingPlayer());
        commandList.add(new CommandSelectWorker());
        commandList.add(new CommandSetName());
        commandList.add(new CommandDescription());
    }
}

