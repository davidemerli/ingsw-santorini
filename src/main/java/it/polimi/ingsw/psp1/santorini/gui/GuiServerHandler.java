package it.polimi.ingsw.psp1.santorini.gui;

import it.polimi.ingsw.psp1.santorini.gui.controllers.*;
import it.polimi.ingsw.psp1.santorini.model.map.Point;
import it.polimi.ingsw.psp1.santorini.model.map.Worker;
import it.polimi.ingsw.psp1.santorini.network.Client;
import it.polimi.ingsw.psp1.santorini.network.ServerHandler;
import it.polimi.ingsw.psp1.santorini.network.packets.client.ClientKeepAlive;
import it.polimi.ingsw.psp1.santorini.network.packets.server.*;
import javafx.scene.paint.Color;

import java.util.stream.IntStream;

public class GuiServerHandler extends ServerHandler {


    public GuiServerHandler(Client client) {
        super(client);

        client.enableDebug();

        GuiObserver guiObserver = new GuiObserver(client, this);

        ChooseGameSceneController.getInstance().addObserver(guiObserver);
        NameSelectionController.getInstance().addObserver(guiObserver);
        IpSelectionController.getInstance().addObserver(guiObserver);
        GameSceneController.getInstance().addObserver(guiObserver);
        ChoosePowersController.getInstance().addObserver(guiObserver);
        StartingPlayerController.getInstance().addObserver(guiObserver);
    }

    @Override
    public void handleGameData(ServerGameData packet) {
        super.handleGameData(packet);

        if (packet.isForced()) {
            GameSceneController.getInstance().reset();

            for (Point p : packet.getGameMap().getAllSquares()) {
                int level = packet.getGameMap().getLevel(p);
                boolean hasDome = packet.getGameMap().hasDome(p);

                IntStream.range(0, hasDome ? level - 1 : level)
                        .forEach(i -> GameSceneController.getInstance().addBlockAt(p.x, p.y, false));

                if(hasDome) {
                    GameSceneController.getInstance().addBlockAt(p.x, p.y, true);
                }
            }

            for (PlayerData player : packet.getPlayerData()) {
                boolean isOwn = player.getName().equals(playerName);
                Color color = getPlayerColorMap().get(player.getName()).getColor();

                player.getWorkers().stream()
                        .map(Worker::getPosition)
                        .forEach(p -> GameSceneController.getInstance().addWorker(p.x, p.y, color, isOwn));
            }
        }
    }

    @Override
    public void handleRequest(ServerAskRequest packet) {
        super.handleRequest(packet);

        for (int i = 0; i < 100; i++) {
            client.sendPacket(new ClientKeepAlive());
        }

        //TODO: request types to decently written strings
        switch (packet.getRequestType()) {
            case CHOOSE_POWERS:
            case SELECT_POWER:
                Gui.getInstance().changeSceneAsync(EnumScene.CHOOSE_POWERS, EnumTransition.DOWN);
                break;
            case SELECT_STARTING_PLAYER:
                Gui.getInstance().changeSceneAsync(EnumScene.STARTING_PLAYER, EnumTransition.DOWN);

                for (PlayerData playerData : getPlayerDataList()) {
                    StartingPlayerController.getInstance().addPlayer(playerData.getName(), playerData.getPower());
                }
                break;
        }

        GameSceneController.getInstance().showRequest(packet.getRequestType().toString());
    }

    @Override
    public void handlePlayerUpdate(ServerSendPlayerUpdate packet) {
        int index = getPlayerDataList().indexOf(packet.getPlayerData());

        boolean updatePowerImage = index != -1 && getPlayerDataList().get(index).getPower() == null;

        super.handlePlayerUpdate(packet);

        if (updatePowerImage) {
            WaitGodSelectionController.getInstance().addPlayerPower(packet.getPlayerData().getName(),
                    packet.getPlayerData().getPower());
        }

        GameSceneController.getInstance().showInteract(packet.shouldShowInteraction());
    }

    @Override
    public void handleReceivedMoves(ServerMovePossibilities packet) {
        super.handleReceivedMoves(packet);

        if (isYourTurn()) {
            Gui.getInstance().changeSceneAsync(EnumScene.GAME, EnumTransition.DOWN);

            GameSceneController.getInstance().showValidMoves(packet.getValidMoves());
        }
    }

    @Override
    public void handleError(ServerInvalidPacket packet) {

    }

    @Override
    public void handlePlayerMove(ServerPlayerMove packet) {
        boolean ownData = packet.getPlayerData().getName().equals(playerName);

        switch (packet.getMoveType()) {
            case MOVE:
                ServerPlayerMove.PlayerMove move = (ServerPlayerMove.PlayerMove) packet.getMove();

                GameSceneController.getInstance().moveWorker(move.getSrc(), move.getDest(), ownData);
                break;
            case BUILD:
                ServerPlayerMove.PlayerBuild build = (ServerPlayerMove.PlayerBuild) packet.getMove();

                GameSceneController.getInstance().addBlockAt(build.getDest().x, build.getDest().y, build.forceDome());

                break;
            case PLACE_WORKER:
                ServerPlayerMove.PlayerPlaceWorker worker = (ServerPlayerMove.PlayerPlaceWorker) packet.getMove();

                Color color = getPlayerColorMap().get(packet.getPlayerData().getName()).getColor().darker();

                GameSceneController.getInstance().addWorker(worker.getDest().x, worker.getDest().y,
                        packet.getPlayerData().getName().equals("masterrace") ? null : color,
                        isYourTurn());
                break;
        }
    }

    @Override
    public void handlePowerList(ServerPowerList packet) {
        super.handlePowerList(packet);

        if (isYourTurn()) {
            ChoosePowersController.getInstance().addGods(packet.getAvailablePowers(), packet.getToSelect());
        }
    }

    @Override
    public void handlePlayerConnected(ServerConnectedToGame packet) {
        super.handlePlayerConnected(packet);

        WaitGodSelectionController.getInstance().addPlayer(packet.getName());
    }

    @Override
    public void onDisconnect() {
        Gui.getInstance().changeSceneAsync(EnumScene.IP_SELECT, EnumTransition.DOWN);
    }


    @Override
    public void reset() {
        super.reset();

        GameSceneController.getInstance().reset();

        WaitGodSelectionController.getInstance().reset();
    }
}