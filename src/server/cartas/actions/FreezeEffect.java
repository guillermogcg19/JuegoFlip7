package server.cartas.actions;

import server.cartas.IActionCardEffect;
import server.game.IGameRoom;
import server.game.IJugador;
import java.io.IOException;
import java.util.List;

public class FreezeEffect implements IActionCardEffect {

    @Override
    public void ejecutar(IJugador jugadorActual, IGameRoom sala) throws IOException {
        List<IJugador> vivos = sala.getJugadoresVivosExcepto(jugadorActual);

        if (vivos.isEmpty()) {
            sala.agregarLog(jugadorActual.getNombre() + " intentó Freeze pero no hay objetivos.");
            return;
        }

        jugadorActual.enviar("Seleccione jugador a congelar:");

        for (int i = 0; i < vivos.size(); i++) {
            jugadorActual.enviar((i + 1) + ") " + vivos.get(i).getNombre());
        }

        int op = jugadorActual.leerOpcion(1, vivos.size());
        IJugador objetivo = vivos.get(op - 1);

        objetivo.setCongelado(true);
        sala.agregarLog(jugadorActual.getNombre() + " aplicó Freeze a " + objetivo.getNombre());
    }
}