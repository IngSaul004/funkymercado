package lat.funkypunky.mercado;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Lee config.yml una vez (al arrancar y con /fmercado recargar). Todo es de solo lectura. */
final class Ajustes {

    /** Máximo de familias por categoría y de productos por familia (lo que cabe en un menú de 6 filas). */
    static final int MAX_POR_MENU = 28;

    record Familia(String clave, String nombre, Material icono, double precio, List<Material> productos) {
    }

    record Categoria(String clave, String titulo, Material icono, Map<String, Familia> familias) {
    }

    final ZoneId zona;
    final boolean zonaActiva;
    final String zonaMundo;
    final int minX, minY, minZ, maxX, maxY, maxZ;
    final int cantidad;
    final boolean velocidadActiva;
    final int velocidadNivel;
    final Map<String, Categoria> categorias;
    private final ConfigurationSection mensajes;

    Ajustes(FileConfiguration c, Logger log) {
        ZoneId z;
        try {
            z = ZoneId.of(c.getString("zona-horaria", "America/Mexico_City"));
        } catch (Exception e) {
            log.warning("zona-horaria inválida; se usa America/Mexico_City.");
            z = ZoneId.of("America/Mexico_City");
        }
        zona = z;

        zonaActiva = c.getBoolean("zona.activa", true);
        zonaMundo = c.getString("zona.mundo", "world");
        List<Integer> a = c.getIntegerList("zona.min");
        List<Integer> b = c.getIntegerList("zona.max");
        if (a.size() != 3 || b.size() != 3) {
            log.warning("zona.min y zona.max deben tener 3 números [x, y, z]. La tienda queda cerrada en todos lados.");
            a = List.of(0, 0, 0);
            b = List.of(-1, -1, -1);
        }
        minX = Math.min(a.get(0), b.get(0));
        minY = Math.min(a.get(1), b.get(1));
        minZ = Math.min(a.get(2), b.get(2));
        maxX = Math.max(a.get(0), b.get(0));
        maxY = Math.max(a.get(1), b.get(1));
        maxZ = Math.max(a.get(2), b.get(2));

        cantidad = Math.max(1, Math.min(64, c.getInt("cantidad", 64)));
        velocidadActiva = c.getBoolean("velocidad.activa", true);
        velocidadNivel = Math.max(1, Math.min(5, c.getInt("velocidad.nivel", 2)));
        mensajes = c.getConfigurationSection("mensajes");

        Map<String, Categoria> cats = new LinkedHashMap<>();
        ConfigurationSection sc = c.getConfigurationSection("categorias");
        if (sc != null) {
            for (String ck : sc.getKeys(false)) {
                ConfigurationSection cs = sc.getConfigurationSection(ck);
                if (cs == null) continue;
                double precioCat = cs.getDouble("precio", -1);
                Map<String, Familia> fams = new LinkedHashMap<>();
                ConfigurationSection sf = cs.getConfigurationSection("familias");
                if (sf != null) {
                    for (String fk : sf.getKeys(false)) {
                        ConfigurationSection fs = sf.getConfigurationSection(fk);
                        if (fs == null) continue;
                        double precio = fs.getDouble("precio", precioCat);
                        if (precio <= 0) {
                            log.warning("La familia " + ck + "." + fk + " no tiene precio válido: se omite.");
                            continue;
                        }
                        List<Material> productos = new ArrayList<>();
                        for (String nombre : fs.getStringList("productos")) {
                            Material m = material(nombre);
                            if (m == null) {
                                log.warning("Producto desconocido en " + ck + "." + fk + ": " + nombre + " (se omite).");
                            } else if (productos.contains(m)) {
                                log.warning("Producto repetido en " + ck + "." + fk + ": " + nombre + " (se omite).");
                            } else if (productos.size() >= MAX_POR_MENU) {
                                log.warning("Más de " + MAX_POR_MENU + " productos en " + ck + "." + fk + ": " + nombre + " no cabe.");
                            } else {
                                productos.add(m);
                            }
                        }
                        if (productos.isEmpty()) continue;
                        if (fams.size() >= MAX_POR_MENU) {
                            log.warning("Más de " + MAX_POR_MENU + " familias en " + ck + ": " + fk + " no cabe.");
                            continue;
                        }
                        Material icono = material(fs.getString("icono", ""));
                        String clave = fk.toLowerCase(Locale.ROOT);
                        fams.put(clave, new Familia(clave, fs.getString("nombre", "&d" + fk),
                                icono != null ? icono : productos.get(0), precio, List.copyOf(productos)));
                    }
                }
                if (fams.isEmpty()) {
                    log.warning("La categoría " + ck + " no tiene productos válidos: se omite.");
                    continue;
                }
                Material icono = material(cs.getString("icono", ""));
                String clave = ck.toLowerCase(Locale.ROOT);
                cats.put(clave, new Categoria(clave, cs.getString("titulo", "&5&l" + ck),
                        icono != null ? icono : fams.values().iterator().next().icono(),
                        Collections.unmodifiableMap(fams)));
            }
        }
        categorias = Collections.unmodifiableMap(cats);
    }

    /** Solo materiales que existen como objeto (no aire, no bloques técnicos). */
    private static Material material(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        Material m = Material.matchMaterial(nombre.trim());
        return (m != null && m.isItem() && !m.isAir()) ? m : null;
    }

    boolean dentroDeZona(Location l) {
        if (!zonaActiva) return true;
        if (l.getWorld() == null || !l.getWorld().getName().equals(zonaMundo)) return false;
        int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    int totalProductos() {
        int n = 0;
        for (Categoria c : categorias.values()) {
            for (Familia f : c.familias().values()) n += f.productos().size();
        }
        return n;
    }

    String msg(String clave) {
        String s = mensajes == null ? null : mensajes.getString(clave);
        return s == null ? clave : s;
    }

    String prefijo() {
        return msg("prefijo");
    }
}
