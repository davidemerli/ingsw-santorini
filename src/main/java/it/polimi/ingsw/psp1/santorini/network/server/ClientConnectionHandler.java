package it.polimi.ingsw.psp1.santorini.network.server;

import it.polimi.ingsw.psp1.santorini.model.Player;
import it.polimi.ingsw.psp1.santorini.network.ClientHandler;
import it.polimi.ingsw.psp1.santorini.network.ServerHandler;
import it.polimi.ingsw.psp1.santorini.network.packets.Packet;
import it.polimi.ingsw.psp1.santorini.network.packets.client.*;
import it.polimi.ingsw.psp1.santorini.network.packets.server.ServerInvalidPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientConnectionHandler implements Runnable, ClientHandler {

    private final Server server;

    private final Socket clientSocket;
    private final ExecutorService executionQueue;

    private final ObjectInputStream objectInputStream;
    private final ObjectOutputStream objectOutputStream;

    ClientConnectionHandler(Server server, Socket clientSocket) throws IOException {
        this.server = server;
        this.clientSocket = clientSocket;
        this.executionQueue = Executors.newSingleThreadExecutor();

        this.objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
        this.objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    public void sendPacket(Packet<ServerHandler> packet) {
        try {
            objectOutputStream.writeObject(packet);
            objectOutputStream.flush();
        } catch (IOException e) {
            //TODO: what if send packet fails? try to resend?
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            Object object = objectInputStream.readObject();

            ((Packet<ClientHandler>) object).processPacket(this);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlePlayerSetName(ClientSetName packet) {
        Player player = new Player(packet.getName());
        try {
            server.addToWait(player, this);
        } catch (UnsupportedOperationException e) {
            sendPacket(new ServerInvalidPacket("There is a player with the same name. Please choose another name"));
        }
    }

    @Override
    public void handleCreateGame(ClientCreateGame packet) {
        try {
            server.createGame(this);
        }catch (UnsupportedOperationException e) {
            sendPacket(new ServerInvalidPacket("A game is already created"));
        }
    }

    @Override
    public void handleJoinGame(ClientJoinGame packet) {

    }

    @Override
    public void handlePowerChoosing(ClientChoosePower clientChoosePower) {

    }

    @Override
    public void handleSquareSelect(ClientSelectSquare clientSelectSquare) {

    }

    @Override
    public void handleInteractionToggle(ClientToggleInteraction clientToggleInteraction) {

    }

    @Override
    public void handlePlayerForfeit(ClientForfeit clientForfeit) {

    }

    @Override
    public void handleKeepAlive(ClientKeepAlive clientKeepAlive) {

    }

    @Override
    public void handleRequestGameData(ClientRequestGameData clientRequestGameData) {

    }

    @Override
    public void handleWorkerSelection(ClientSelectWorker clientSelectWorker) {

    }

    @Override
    public void handleSetPlayerNumber(ClientSetPlayerNumber clientSetPlayerNumber) {

    }
}
