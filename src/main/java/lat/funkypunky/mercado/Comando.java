package lat.funkypunky.mercado;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /deco [categoria] para jugadores y /fmercado info|recargar para el staff. */
final class Comando implements TabExecutor {

    private final FunkyMercado plugin;

    Comando(FunkyMercado plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("fmercado")) return admin(s, args);

        if (!(s instanceof Player p)) {
            s.sendMessage(Texto.c("&cSolo desde el juego."));
            return true;
        }
        if (!p.hasPermission("funkymercado.usar")) {
            p.sendMessage(Texto.c("&cNo tienes permiso."));
            return true;
        }
        if (args.length == 0) {
            plugin.tienda().abrirPrincipal(p);
        } else {
            plugin.tienda().abrirCategoria(p, args[0].toLowerCase(Locale.ROOT));
        }
        return true;
    }

    private boolean admin(CommandSender s, String[] args) {
        if (!s.hasPermission("funkymercado.admin")) {
            s.sendMessage(Texto.c("&cNo tienes permiso."));
            return true;
        }
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("recargar")) {
            plugin.recargar();
            s.sendMessage(Texto.c("&d[FunkyMercado] &fConfig recargado: &d" + plugin.ajustes().categorias.size()
                    + " &fcategorías, &d" + plugin.ajustes().totalProductos() + " &fproductos."));
            return true;
        }
        Ajustes a = plugin.ajustes();
        s.sendMessage(Texto.c("&d[FunkyMercado] &fVersión &d" + plugin.getPluginMeta().getVersion()));
        s.sendMessage(Texto.c("&7Economía (Vault): " + (plugin.eco().disponible() ? "&aconectada" : "&cNO disponible")));
        s.sendMessage(Texto.c("&7Zona: " + (a.zonaActiva
                ? "&f" + a.zonaMundo + " (" + a.minX + ", " + a.minY + ", " + a.minZ + ") → ("
                + a.maxX + ", " + a.maxY + ", " + a.maxZ + ")"
                : "&eapagada (se compra en cualquier lugar)")));
        for (Ajustes.Categoria c : a.categorias.values()) {
            s.sendMessage(Texto.c("&7- &d" + c.clave() + "&7: " + c.familias().size() + " familias"));
            for (Ajustes.Familia f : c.familias().values()) {
                s.sendMessage(Texto.c("&7    " + f.clave() + ": " + f.productos().size() + " productos, "
                        + Texto.dinero(f.precio()) + " por " + a.cantidad));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> opciones = new ArrayList<>();
        if (args.length != 1) return opciones;
        if (cmd.getName().equalsIgnoreCase("fmercado")) {
            if (s.hasPermission("funkymercado.admin")) opciones.addAll(List.of("info", "recargar"));
        } else {
            opciones.addAll(plugin.ajustes().categorias.keySet());
        }
        String pre = args[0].toLowerCase(Locale.ROOT);
        opciones.removeIf(o -> !o.startsWith(pre));
        return opciones;
    }
}
