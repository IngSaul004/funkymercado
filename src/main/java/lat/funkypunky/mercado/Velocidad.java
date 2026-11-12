package lat.funkypunky.mercado;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Velocidad dentro del mercado.
 *
 * Cada segundo, a quien esté dentro de la zona se le da Velocidad por 2 segundos. Al salir, nadie la renueva
 * y se acaba sola en menos de 2 s. Nunca se quita ningún efecto:
 *  - Si ya tiene Velocidad igual o más fuerte y más larga (ruleta, poción), Minecraft conserva la suya.
 *  - Si tiene una más débil (Velocidad I de poción), Minecraft la guarda "debajo" y la regresa al terminar esta.
 * Sin ícono ni partículas: con 2 s de duración el ícono parpadearía todo el tiempo.
 */
final class Velocidad implements Runnable {

    private static final int DURACION_TICKS = 40;

    private final FunkyMercado plugin;

    Velocidad(FunkyMercado plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Ajustes a = plugin.ajustes();
        if (!a.velocidadActiva || !a.zonaActiva) return;
        int amplificador = a.velocidadNivel - 1; // nivel II = amplificador 1
        for (Player p : Bukkit.getOnlinePlayers()) {
            GameMode gm = p.getGameMode();
            if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) continue;
            if (!a.dentroDeZona(p.getLocation())) continue;
            PotionEffect actual = p.getPotionEffect(PotionEffectType.SPEED);
            // Ya tiene una igual o más fuerte que dura más que la nuestra: no se toca.
            if (actual != null && actual.getAmplifier() >= amplificador
                    && (actual.isInfinite() || actual.getDuration() > DURACION_TICKS)) {
                continue;
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURACION_TICKS, amplificador,
                    true, false, false)); // ambiental, sin partículas, sin ícono
        }
    }
}
