package server.cartas;

import server.game.IGameRoom;
import server.game.IJugador;
import java.io.IOException;

public interface IActionCardEffect {
    void ejecutar(IJugador jugadorActual, IGameRoom sala) throws IOException;
}