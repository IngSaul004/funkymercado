package lat.funkypunky.mercado;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Utilidades de texto: colores &, dinero y nombres de materiales. */
final class Texto {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private Texto() {
    }

    /** Texto con códigos & a Component, sin la cursiva que Minecraft pone en los lores. */
    static Component c(String s) {
        return LEGACY.deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    /** 1234.5 -> "$1,234.5" (mismo formato que Essentials). */
    static String dinero(double v) {
        DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return "$" + f.format(v);
    }

    /**
     * Nombre del bloque para el chat como componente traducible: cada jugador lo ve en el idioma
     * de su juego ("Concreto rosa"), también en Bedrock.
     */
    static Component material(Material m) {
        return Component.translatable(m.translationKey());
    }
}
