package lat.funkypunky.mercado;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** FunkyMercado: tienda de decoración del mercado de FUNKY PUNKY MC. Solo vende: el dinero sale de la economía. */
public final class FunkyMercado extends JavaPlugin {

    private Ajustes ajustes;
    private Economia eco;
    private Registro registro;
    private Tienda tienda;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ajustes = new Ajustes(getConfig(), getLogger());
        eco = new Economia();
        registro = new Registro(new File(getDataFolder(), "registro.log"), () -> ajustes.zona, getLogger());
        tienda = new Tienda(this);

        Comando c = new Comando(this);
        for (String nombre : new String[]{"deco", "fmercado"}) {
            PluginCommand cmd = getCommand(nombre);
            if (cmd != null) {
                cmd.setExecutor(c);
                cmd.setTabCompleter(c);
            }
        }
        getServer().getPluginManager().registerEvents(tienda, this);

        // La economía la registra Essentials al arrancar: se revisa cuando ya cargaron todos los plugins.
        getServer().getScheduler().runTask(this, () -> {
            if (!eco.disponible()) {
                getLogger().severe("No hay economía de Vault (VaultUnlocked + Essentials). Las compras van a fallar.");
            }
        });
        getLogger().info("FunkyMercado " + getPluginMeta().getVersion() + " listo: "
                + ajustes.categorias.size() + " categorías, " + ajustes.totalProductos() + " productos.");
    }

    void recargar() {
        reloadConfig();
        ajustes = new Ajustes(getConfig(), getLogger());
    }

    Ajustes ajustes() {
        return ajustes;
    }

    Economia eco() {
        return eco;
    }

    Registro registro() {
        return registro;
    }

    Tienda tienda() {
        return tienda;
    }
}
