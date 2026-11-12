package lat.funkypunky.mercado;

import lat.funkypunky.mercado.Ajustes.Categoria;
import lat.funkypunky.mercado.Ajustes.Familia;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Menús de cofre y compras (en Bedrock, Geyser los muestra igual que en Java).
 *
 * Contra duplicados y cobros mal hechos (misma lección que FunkySubastas):
 *  - Los menús solo MUESTRAN: todo clic se cancela, también en el inventario propio, shift-clic y arrastrar.
 *  - La compra vuelve a leer el config actual (precio y producto) justo antes de cobrar.
 *  - Orden: revisar zona -> revisar espacio -> revisar saldo -> cobrar -> entregar.
 *    Si la entrega fallara, se quita lo entregado y se devuelve el dinero. Nunca se tira nada al suelo.
 *  - El botón COMPRAR solo funciona una vez por menú (un doble clic no compra dos veces).
 */
final class Tienda implements Listener {

    /** Marca qué menú es y qué acción hay en cada casilla. */
    static final class Holder implements InventoryHolder {
        enum Tipo { PRINCIPAL, CATEGORIA, FAMILIA, CONFIRMAR }

        final Tipo tipo;
        final String categoria;
        final String familia;
        final Material producto;
        final String[] acciones = new String[54];
        boolean usado;
        Inventory inv;

        Holder(Tipo tipo, String categoria, String familia, Material producto) {
            this.tipo = tipo;
            this.categoria = categoria;
            this.familia = familia;
            this.producto = producto;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final FunkyMercado plugin;

    Tienda(FunkyMercado plugin) {
        this.plugin = plugin;
    }

    private Ajustes a() {
        return plugin.ajustes();
    }

    // ------------------------------------------------------------ Abrir menús

    /** Revisa la zona y avisa si está fuera. */
    boolean puedeUsarAqui(Player p) {
        if (a().dentroDeZona(p.getLocation())) return true;
        decir(p, a().msg("solo-mercado"));
        return false;
    }

    void abrirPrincipal(Player p) {
        if (!puedeUsarAqui(p)) return;
        List<Categoria> cats = new ArrayList<>(a().categorias.values());
        if (cats.size() == 1) { // con una sola categoría no tiene caso un menú intermedio
            abrirCategoria(p, cats.get(0).clave());
            return;
        }
        Holder h = new Holder(Holder.Tipo.PRINCIPAL, null, null, null);
        Inventory inv = crear(h, cats.size(), "&5&lTienda del mercado");
        for (int i = 0; i < cats.size(); i++) {
            Categoria c = cats.get(i);
            poner(h, casilla(i), icono(c.icono(), 1, c.titulo(),
                    "&7" + c.familias().size() + " tipos de bloque", "", "&eClic para ver"), "cat:" + c.clave());
        }
        botonesAbajo(h, p, false);
        p.openInventory(inv);
    }

    void abrirCategoria(Player p, String clave) {
        if (!puedeUsarAqui(p)) return;
        Categoria c = a().categorias.get(clave);
        if (c == null) {
            decir(p, a().msg("categoria-no-existe"));
            return;
        }
        List<Familia> fams = new ArrayList<>(c.familias().values());
        Holder h = new Holder(Holder.Tipo.CATEGORIA, c.clave(), null, null);
        Inventory inv = crear(h, fams.size(), c.titulo());
        for (int i = 0; i < fams.size(); i++) {
            Familia f = fams.get(i);
            poner(h, casilla(i), icono(f.icono(), 1, f.nombre(),
                    "&7" + f.productos().size() + " colores",
                    "&fPrecio: &d" + Texto.dinero(f.precio()) + " &7por stack de " + a().cantidad,
                    "", "&eClic para ver"), "fam:" + f.clave());
        }
        botonesAbajo(h, p, a().categorias.size() > 1);
        p.openInventory(inv);
    }

    void abrirFamilia(Player p, String cat, String fam) {
        if (!puedeUsarAqui(p)) return;
        Familia f = familia(cat, fam);
        if (f == null) {
            decir(p, a().msg("ya-no-esta"));
            p.closeInventory();
            return;
        }
        Holder h = new Holder(Holder.Tipo.FAMILIA, cat, fam, null);
        Inventory inv = crear(h, f.productos().size(), f.nombre());
        List<Material> prods = f.productos();
        for (int i = 0; i < prods.size(); i++) {
            poner(h, casilla(i), icono(prods.get(i), a().cantidad, null,
                    "&7Stack de " + a().cantidad,
                    "&fPrecio: &d" + Texto.dinero(f.precio()),
                    "", "&eClic para comprar"), "prod:" + prods.get(i).name());
        }
        botonesAbajo(h, p, true);
        p.openInventory(inv);
    }

    void abrirConfirmar(Player p, String cat, String fam, Material m) {
        if (!puedeUsarAqui(p)) return;
        Familia f = familia(cat, fam);
        if (f == null || !f.productos().contains(m)) {
            decir(p, a().msg("ya-no-esta"));
            p.closeInventory();
            return;
        }
        Holder h = new Holder(Holder.Tipo.CONFIRMAR, cat, fam, m);
        h.inv = Bukkit.createInventory(h, 27, Texto.c("&8Confirmar compra"));
        String precio = Texto.dinero(f.precio());
        String saldo = Texto.dinero(plugin.eco().saldo(p));
        poner(h, 11, icono(Material.LIME_CONCRETE, 1, "&a&lCOMPRAR",
                "&fPrecio: &d" + precio, "&fTu saldo: &d" + saldo), "confirmar");
        poner(h, 13, icono(m, a().cantidad, null, "&7Stack de " + a().cantidad, "&fPrecio: &d" + precio), null);
        poner(h, 15, icono(Material.RED_CONCRETE, 1, "&c&lCANCELAR", "&7Volver a la lista"), "cancelar");
        p.openInventory(h.inv);
    }

    // ------------------------------------------------------------ Clics

    @EventHandler
    public void alClic(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        e.setCancelled(true); // nada entra ni sale de un menú de la tienda
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= h.inv.getSize()) return;
        String accion = h.acciones[slot];
        if (accion == null) return;
        if (accion.equals("confirmar")) {
            if (h.usado) return;
            h.usado = true;
        }
        // Se actúa en el siguiente tick: abrir o cerrar menús dentro del evento de clic da problemas.
        Bukkit.getScheduler().runTask(plugin, () -> actuar(p, h, accion));
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holder) e.setCancelled(true);
    }

    private void actuar(Player p, Holder h, String accion) {
        if (!p.isOnline()) return;
        if (accion.startsWith("cat:")) {
            abrirCategoria(p, accion.substring(4));
        } else if (accion.startsWith("fam:")) {
            abrirFamilia(p, h.categoria, accion.substring(4));
        } else if (accion.startsWith("prod:")) {
            Material m = Material.matchMaterial(accion.substring(5));
            if (m != null) abrirConfirmar(p, h.categoria, h.familia, m);
        } else if (accion.equals("confirmar")) {
            if (comprar(p, h.categoria, h.familia, h.producto)) {
                abrirFamilia(p, h.categoria, h.familia); // de regreso a la lista para seguir comprando
            } else {
                p.closeInventory();
            }
        } else if (accion.equals("cancelar")) {
            abrirFamilia(p, h.categoria, h.familia);
        } else if (accion.equals("volver")) {
            switch (h.tipo) {
                case FAMILIA -> abrirCategoria(p, h.categoria);
                case CATEGORIA -> abrirPrincipal(p);
                default -> p.closeInventory();
            }
        } else if (accion.equals("cerrar")) {
            p.closeInventory();
        }
    }

    // ------------------------------------------------------------ Compra

    /** Devuelve true si se compró. Todo pasa en el hilo principal: nada cambia entre revisar y cobrar. */
    boolean comprar(Player p, String cat, String fam, Material m) {
        Ajustes a = a();
        if (!a.dentroDeZona(p.getLocation())) {
            decir(p, a.msg("solo-mercado"));
            return false;
        }
        Familia f = familia(cat, fam);
        if (f == null || m == null || !f.productos().contains(m)) {
            decir(p, a.msg("ya-no-esta"));
            return false;
        }
        double precio = f.precio();
        int cantidad = a.cantidad;
        Economia eco = plugin.eco();

        if (espacioPara(p, m) < cantidad) {
            decir(p, a.msg("sin-espacio").replace("{cantidad}", String.valueOf(cantidad)));
            return false;
        }
        if (!eco.disponible()) {
            decir(p, a.msg("error-pago"));
            plugin.getLogger().severe("Compra fallida: no hay economía de Vault.");
            return false;
        }
        if (!eco.tiene(p, precio)) {
            decir(p, a.msg("sin-dinero")
                    .replace("{precio}", Texto.dinero(precio))
                    .replace("{saldo}", Texto.dinero(eco.saldo(p))));
            return false;
        }
        if (!eco.retirar(p, precio)) {
            decir(p, a.msg("error-pago"));
            plugin.registro().anotar("ERROR_PAGO", p.getName() + " (" + p.getUniqueId() + ") | "
                    + m.name() + " x" + cantidad + " | " + Texto.dinero(precio) + " | Vault no cobró");
            return false;
        }

        Map<Integer, ItemStack> sobra = p.getInventory().addItem(new ItemStack(m, cantidad));
        if (!sobra.isEmpty()) {
            // No debería pasar (ya se revisó el espacio), pero si pasa se deshace todo.
            int noCupo = 0;
            for (ItemStack it : sobra.values()) noCupo += it.getAmount();
            int entregado = cantidad - noCupo;
            if (entregado > 0) p.getInventory().removeItem(new ItemStack(m, entregado));
            boolean devuelto = eco.depositar(p, precio);
            plugin.registro().anotar("ERROR_ENTREGA", p.getName() + " (" + p.getUniqueId() + ") | "
                    + m.name() + " x" + cantidad + " | " + Texto.dinero(precio)
                    + (devuelto ? " | dinero devuelto" : " | NO SE PUDO DEVOLVER: pagar a mano"));
            decir(p, a.msg("error-pago"));
            return false;
        }

        plugin.registro().anotar("COMPRO", p.getName() + " (" + p.getUniqueId() + ") | "
                + m.name() + " x" + cantidad + " | " + Texto.dinero(precio));
        String texto = a.msg("compro")
                .replace("{cantidad}", String.valueOf(cantidad))
                .replace("{precio}", Texto.dinero(precio))
                .replace("{saldo}", Texto.dinero(eco.saldo(p)));
        Component msg = Texto.c(a.prefijo() + texto)
                .replaceText(b -> b.matchLiteral("{producto}").replacement(Texto.material(m)));
        p.sendMessage(msg);
        decir(p, a.msg("recuerda"));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        return true;
    }

    /** Cuántos bloques de ese material caben en el inventario (sin armadura ni mano secundaria). */
    static int espacioPara(Player p, Material m) {
        int max = m.getMaxStackSize();
        ItemStack ref = new ItemStack(m);
        int libre = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            if (it == null || it.getType().isAir()) {
                libre += max;
            } else if (it.isSimilar(ref)) {
                libre += Math.max(0, max - it.getAmount());
            }
        }
        return libre;
    }

    // ------------------------------------------------------------ Ayudas

    private Familia familia(String cat, String fam) {
        Categoria c = cat == null ? null : a().categorias.get(cat);
        return (c == null || fam == null) ? null : c.familias().get(fam);
    }

    /** Menú con filas para n objetos (7 por fila, con borde) + fila de botones abajo. */
    private static Inventory crear(Holder h, int n, String titulo) {
        int filas = Math.max(1, (n + 6) / 7) + 2;
        filas = Math.min(6, filas);
        h.inv = Bukkit.createInventory(h, filas * 9, Texto.c(titulo));
        return h.inv;
    }

    /** Casilla i dentro del borde: filas 2..5, columnas 2..8. */
    private static int casilla(int i) {
        return (1 + i / 7) * 9 + 1 + (i % 7);
    }

    private void botonesAbajo(Holder h, Player p, boolean volver) {
        int base = h.inv.getSize() - 9;
        if (volver) poner(h, base, icono(Material.ARROW, 1, "&f« Volver"), "volver");
        poner(h, base + 4, icono(Material.GOLD_INGOT, 1, "&6Tu saldo",
                "&f" + Texto.dinero(plugin.eco().saldo(p)),
                "&7Más barato en los puestos de jugadores y en &d/ah"), null);
        poner(h, base + 8, icono(Material.BARRIER, 1, "&cCerrar"), "cerrar");
    }

    private static void poner(Holder h, int slot, ItemStack it, String accion) {
        h.inv.setItem(slot, it);
        h.acciones[slot] = accion;
    }

    /** nombre null = se deja el nombre normal del bloque (cada jugador lo ve en su idioma). */
    private static ItemStack icono(Material m, int cantidad, String nombre, String... lore) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(cantidad, m.getMaxStackSize())));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (nombre != null) meta.displayName(Texto.c(nombre));
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(Texto.c(s));
            meta.lore(l);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void decir(Player p, String texto) {
        p.sendMessage(Texto.c(a().prefijo() + texto));
    }
}
